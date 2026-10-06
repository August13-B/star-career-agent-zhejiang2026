package org.example.web.service.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.web.tool.RSA_256;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 训练目标岗位目录。
 *
 * <p>产品口径（已与用户确认）：
 * <ul>
 *   <li>候选岗位来源：**学生职业意向 → 全库搜索**；意向为空时回退到最近一次职业报告的目标岗位</li>
 *   <li>两者都没有 → 不兜底推荐、不允许手填，提示用户先去完善意向/生成报告</li>
 *   <li>选定岗位后**冻结快照**进会话，供出题占位符（job/skills 花括号变量）与评分对照使用</li>
 * </ul>
 *
 * <p>实现要点：
 * <ol>
 *   <li>画像字段（career_intentions / job_intention_detail）在库里是 **RSA（兼容 AES）密文**，
 *       必须先解密，否则会拿密文去 LIKE 搜索（曾导致"暂无可选岗位"）</li>
 *   <li>候选池**优先取岗位画像表** {@code job_requirement_profile}（181 条人工整理的「级别+岗位」，
 *       如「中级Java开发工程师」）；为空时再退到真实岗位 {@code job_info}（岗位名较杂：Java/C/C++…）</li>
 *   <li>搜索**逐级放宽**：整词 → 5/4/3 字滑动子串（"全栈开发工程师" → "开发工程师" → "工程师"），
 *       按命中长度由长到短排序，避免一上来就被「工程师」这类宽词淹没</li>
 *   <li>候选键带池前缀（{@code profile:123} / {@code job:456}），避免两个池的 id 混淆</li>
 * </ol>
 */
@Component
public class TrainingJobCatalog {
    private static final int MAX_KEYWORDS = 6;
    private static final int DEFAULT_LIMIT = 20;

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    /** 可选：单元测试的最小上下文里可能没有 RSA Bean（此时按原值处理） */
    private final ObjectProvider<RSA_256> rsa256;

    public TrainingJobCatalog(JdbcTemplate jdbc, ObjectMapper json, ObjectProvider<RSA_256> rsa256) {
        this.jdbc = jdbc;
        this.json = json;
        this.rsa256 = rsa256;
    }

    /** 意向文本 → 搜索关键词（按逗号/顿号/分号/斜杠/空白切分；去噪、去重、限长限数）。 */
    public static List<String> keywords(String intent) {
        List<String> result = new ArrayList<>();
        if (intent == null || intent.isBlank()) {
            return result;
        }
        for (String raw : intent.split("[,，、;；/|\\s]+")) {
            String word = raw.strip()
                    .replaceAll("^(意向|目标|想从事|希望从事|求职|期望)[:：]?", "")
                    .replaceAll("^[^\\p{IsHan}A-Za-z0-9]+|[^\\p{IsHan}A-Za-z0-9]+$", "")
                    .strip();
            if (word.length() < 2 || word.length() > 20 || result.contains(word)) {
                continue;
            }
            result.add(word);
            if (result.size() >= MAX_KEYWORDS) {
                break;
            }
        }
        return result;
    }

    /**
     * 逐级放宽搜索词：整词 → 5/4/3 字滑动子串。
     *
     * <p>仅对「含汉字且长度 ≥5」的词生效（"Java"、"数据分析" 不会产生碎片）。
     */
    static List<String> expand(String word) {
        List<String> probes = new ArrayList<>();
        if (word == null || word.isBlank()) {
            return probes;
        }
        String value = word.strip();
        probes.add(value);
        boolean han = value.codePoints().anyMatch(code -> Character.UnicodeScript.of(code) == Character.UnicodeScript.HAN);
        if (!han || value.length() < 5) {
            return probes;
        }
        for (int size = Math.min(5, value.length() - 1); size >= 3; size--) {
            for (int start = 0; start + size <= value.length(); start++) {
                String part = value.substring(start, start + size);
                if (!probes.contains(part)) {
                    probes.add(part);
                }
            }
        }
        return probes;
    }

    /** 岗位关键词来源：意向（解密后）优先，其次最近报告的 targetJob。 */
    public Map<String, String> source(Long userId) {
        List<Map<String, Object>> profiles = jdbc.queryForList(
                "SELECT career_intentions, job_intention_detail FROM student_profile WHERE user_id=? AND is_deleted=0 ORDER BY id LIMIT 1",
                userId);
        if (!profiles.isEmpty()) {
            String intent = dec(nz(profiles.get(0).get("career_intentions")));
            String detail = dec(nz(profiles.get(0).get("job_intention_detail")));
            String text = !intent.isBlank() ? intent : detail;
            if (!text.isBlank()) {
                return Map.of("type", "intent", "keyword", text);
            }
        }
        String target = reportTargetJob(userId);
        return target.isBlank() ? null : Map.of("type", "report", "keyword", target);
    }

    /** 训练工作台用：候选岗位 + 来源 + 「是否需要先去完善」。 */
    public Map<String, Object> candidates(Long userId) {
        return candidates(userId, DEFAULT_LIMIT);
    }

    public Map<String, Object> candidates(Long userId, int limit) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, String> source = source(userId);
        if (source == null) {
            result.put("needProfile", true);
            result.put("message", "还没有可用的目标岗位：请先到「个人中心」完善职业意向，或先生成一份职业报告");
            result.put("items", List.of());
            return result;
        }
        List<String> words = keywords(source.get("keyword"));
        List<Map<String, Object>> items = words.isEmpty() ? List.of() : search(words, limit);
        result.put("needProfile", items.isEmpty());
        result.put("source", source.get("type"));
        result.put("keyword", source.get("keyword"));
        result.put("message", items.isEmpty()
                ? "按你的职业意向（" + source.get("keyword") + "）没有搜到匹配岗位：请把「职业意向」写得更通用（例如「前端开发」），或先生成一份职业报告"
                : null);
        result.put("items", items);
        return result;
    }

    /** 搜索：岗位画像池优先，逐级放宽；仍为空时退到真实岗位池。 */
    public List<Map<String, Object>> search(List<String> words, int limit) {
        Map<String, Map<String, Object>> found = new LinkedHashMap<>();
        List<String> probes = new ArrayList<>();
        for (String word : words) {
            probes.addAll(expand(word));
        }
        for (String probe : probes) {
            collectProfiles(found, probe, limit);
            if (found.size() >= limit) {
                return new ArrayList<>(found.values());
            }
        }
        if (found.isEmpty()) {
            for (String probe : probes) {
                collectJobs(found, probe, limit);
                if (found.size() >= limit) {
                    break;
                }
            }
        }
        return new ArrayList<>(found.values());
    }

    private void collectProfiles(Map<String, Map<String, Object>> found, String probe, int limit) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, position_name, category, industry, level FROM job_requirement_profile"
                        + " WHERE is_deleted=0 AND position_name LIKE ?"
                        + " ORDER BY (position_name = ?) DESC, CHAR_LENGTH(position_name), id LIMIT ?",
                "%" + probe + "%", probe, Math.max(1, Math.min(limit, 50)));
        for (Map<String, Object> row : rows) {
            long id = ((Number) row.get("id")).longValue();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("jobId", "profile:" + id);
            item.put("pool", "profile");
            item.put("positionName", nz(row.get("position_name")));
            item.put("category", nz(row.get("category")));
            item.put("industry", nz(row.get("industry")));
            item.put("level", nz(row.get("level")));
            item.put("companyName", "");
            item.put("address", "");
            item.put("salaryRange", "");
            item.put("matchedBy", probe);
            found.putIfAbsent("profile:" + id, item);
            if (found.size() >= limit) {
                return;
            }
        }
    }

    private void collectJobs(Map<String, Map<String, Object>> found, String probe, int limit) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, job_name, company_name, industry, address, salary_range FROM job_info"
                        + " WHERE is_deleted=0 AND job_name LIKE ?"
                        + " ORDER BY (job_name = ?) DESC, CHAR_LENGTH(job_name), id LIMIT ?",
                "%" + probe + "%", probe, Math.max(1, Math.min(limit, 50)));
        for (Map<String, Object> row : rows) {
            long id = ((Number) row.get("id")).longValue();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("jobId", "job:" + id);
            item.put("pool", "job");
            item.put("positionName", nz(row.get("job_name")));
            item.put("category", "");
            item.put("industry", nz(row.get("industry")));
            item.put("level", "");
            item.put("companyName", nz(row.get("company_name")));
            item.put("address", nz(row.get("address")));
            item.put("salaryRange", nz(row.get("salary_range")));
            item.put("matchedBy", probe);
            found.putIfAbsent("job:" + id, item);
            if (found.size() >= limit) {
                return;
            }
        }
    }

    /** 冻结岗位快照：{@code profile:123} → 岗位画像表；{@code job:456} → 真实岗位表。 */
    public JsonNode snapshot(String jobKey) {
        if (jobKey == null || jobKey.isBlank()) {
            return null;
        }
        int split = jobKey.indexOf(':');
        String pool = split > 0 ? jobKey.substring(0, split) : "job";
        long id;
        try {
            id = Long.parseLong(split > 0 ? jobKey.substring(split + 1) : jobKey);
        } catch (NumberFormatException e) {
            return null;
        }
        return "profile".equals(pool) ? profileSnapshot(id) : jobSnapshot(id);
    }

    // ====================== 岗位画像池 ======================

    private JsonNode profileSnapshot(long profileId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM job_requirement_profile WHERE id=? AND is_deleted=0", profileId);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> profile = rows.get(0);
        ObjectNode node = json.createObjectNode();
        node.put("jobId", String.valueOf(profileId));
        node.put("pool", "profile");
        node.put("profileId", String.valueOf(profileId));
        node.put("positionName", nz(profile.get("position_name")));
        node.put("category", nz(profile.get("category")));
        node.put("industry", nz(profile.get("industry")));
        node.put("level", nz(profile.get("level")));
        node.put("description", nz(profile.get("description")));
        requirements(node, profileId);
        enrichFromJobs(node);
        return node;
    }

    /** 10 维岗位要求（学历/实习/技能/证书/软实力）—— 要求表为空时自然跳过。 */
    private void requirements(ObjectNode node, long profileId) {
        List<Map<String, Object>> hard = jdbc.queryForList(
                "SELECT education_requirement, internship_requirement FROM job_hard_requirement WHERE is_deleted=0 AND job_id=?", profileId);
        if (!hard.isEmpty()) {
            node.put("education", nz(hard.get(0).get("education_requirement")));
            node.put("internship", nz(hard.get(0).get("internship_requirement")));
        }
        List<Map<String, Object>> skill = jdbc.queryForList(
                "SELECT professional_skill, certificate_requirement FROM job_skill_requirement WHERE is_deleted=0 AND job_id=?", profileId);
        if (!skill.isEmpty()) {
            node.put("skills", nz(skill.get(0).get("professional_skill")));
            node.put("certificate", nz(skill.get(0).get("certificate_requirement")));
        }
        List<Map<String, Object>> soft = jdbc.queryForList(
                "SELECT innovation_ability, learning_ability, pressure_resistance, communication_ability, problem_solving, teamwork_ability"
                        + " FROM job_soft_requirement WHERE is_deleted=0 AND job_id=?", profileId);
        if (!soft.isEmpty()) {
            Map<String, Object> row = soft.get(0);
            ObjectNode softNode = node.putObject("softRequirements");
            softNode.put("innovation", nz(row.get("innovation_ability")));
            softNode.put("learning", nz(row.get("learning_ability")));
            softNode.put("pressure", nz(row.get("pressure_resistance")));
            softNode.put("communication", nz(row.get("communication_ability")));
            softNode.put("problem_solving", nz(row.get("problem_solving")));
            softNode.put("teamwork", nz(row.get("teamwork_ability")));
        }
    }

    /** 画像没有企业/薪资，从真实岗位池里找同类岗位补齐（仅作展示参考）。 */
    private void enrichFromJobs(ObjectNode node) {
        String positionName = node.path("positionName").asText();
        if (positionName.isBlank()) {
            return;
        }
        for (String probe : expand(positionName)) {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT company_name, address, salary_range, job_detail FROM job_info"
                            + " WHERE is_deleted=0 AND job_name LIKE ? ORDER BY CHAR_LENGTH(job_name), id LIMIT 1",
                    "%" + probe + "%");
            if (rows.isEmpty()) {
                continue;
            }
            Map<String, Object> job = rows.get(0);
            node.put("companyName", nz(job.get("company_name")));
            node.put("address", nz(job.get("address")));
            node.put("salaryRange", nz(job.get("salary_range")));
            node.put("referenceJobName", probe);
            String detail = nz(job.get("job_detail")).replaceAll("\\s+", " ").strip();
            if (!detail.isBlank()) {
                node.put("jobDetail", detail.length() > 400 ? detail.substring(0, 400) + "…" : detail);
            }
            return;
        }
    }

    // ====================== 真实岗位池 ======================

    private JsonNode jobSnapshot(long jobId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM job_info WHERE id=? AND is_deleted=0", jobId);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> job = rows.get(0);
        ObjectNode node = json.createObjectNode();
        node.put("jobId", String.valueOf(jobId));
        node.put("pool", "job");
        node.put("positionName", nz(job.get("job_name")));
        node.put("companyName", nz(job.get("company_name")));
        node.put("industry", nz(job.get("industry")));
        node.put("address", nz(job.get("address")));
        node.put("salaryRange", nz(job.get("salary_range")));
        String detail = nz(job.get("job_detail")).replaceAll("\\s+", " ").strip();
        node.put("description", detail.length() > 600 ? detail.substring(0, 600) + "…" : detail);
        String positionName = node.path("positionName").asText();
        List<Map<String, Object>> profiles = jdbc.queryForList(
                "SELECT id, category, level, description FROM job_requirement_profile"
                        + " WHERE is_deleted=0 AND position_name LIKE ? ORDER BY (position_name = ?) DESC, id LIMIT 1",
                "%" + positionName + "%", positionName);
        if (!profiles.isEmpty()) {
            Map<String, Object> profile = profiles.get(0);
            long profileId = ((Number) profile.get("id")).longValue();
            node.put("profileId", String.valueOf(profileId));
            node.put("category", nz(profile.get("category")));
            node.put("level", nz(profile.get("level")));
            node.put("profileDescription", nz(profile.get("description")));
            requirements(node, profileId);
        }
        return node;
    }

    // ====================== 工具 ======================

    /** 报告目标岗位：优先 content.targetJob；老报告没有该字段时，从 goals 的 title/skills 里取线索。 */
    private String reportTargetJob(Long userId) {
        List<String> rows = jdbc.queryForList(
                "SELECT report_content FROM career_report WHERE user_id=? AND is_deleted=0 ORDER BY create_time DESC LIMIT 5",
                String.class, userId);
        for (String content : rows) {
            try {
                JsonNode root = json.readTree(content);
                String target = root.path("targetJob").asText("");
                if (target.isBlank()) {
                    target = root.path("goals").path(0).path("targetJob").asText("");
                }
                if (!target.isBlank()) {
                    return target;
                }
            } catch (Exception ignore) {
                // 历史报告可能不是合法 JSON：跳过
            }
        }
        return "";
    }

    /** 画像敏感字段解密（RSA 优先，兼容早期 AES 写入）；失败或无解密能力时回退原值。 */
    private String dec(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        RSA_256 tool = rsa256.getIfAvailable();
        if (tool == null) {
            return value.strip();
        }
        String text = value.trim();
        try {
            String plain = tool.rsaDecrypt(text);
            if (plain != null && !plain.isBlank()) {
                return plain.strip();
            }
        } catch (Exception ignore) {
            // 非 RSA 密文，继续尝试 AES
        }
        try {
            String plain = tool.decryptFromDB(text);
            if (plain != null && !plain.isBlank()) {
                return plain.strip();
            }
        } catch (Exception ignore) {
            // 非密文，保持原值
        }
        return value.strip();
    }

    private static String nz(Object value) {
        return value == null ? "" : String.valueOf(value).strip();
    }
}
