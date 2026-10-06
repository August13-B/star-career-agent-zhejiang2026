package org.example.web.service.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 训练目标岗位目录。
 *
 * <p>产品口径（已与用户确认）：
 * <ul>
 *   <li>候选岗位来源：**学生职业意向 → 全库岗位搜索**；意向为空时回退到最近一次职业报告的目标岗位</li>
 *   <li>两者都没有 → 不兜底推荐、不允许手填，提示用户先去完善意向/生成报告</li>
 *   <li>选定岗位后**冻结快照**进会话，供出题占位符（形如 job/skills 的花括号变量）与评分对照使用</li>
 * </ul>
 */
@Component
public class TrainingJobCatalog {
    private static final int MAX_KEYWORDS = 6;
    private static final int DEFAULT_LIMIT = 20;

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public TrainingJobCatalog(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
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

    /** 岗位关键词来源：意向优先，其次最近报告的 targetJob。 */
    public Map<String, String> source(Long userId) {
        Map<String, String> profile = jdbc.query(
                "SELECT career_intentions, job_intention_detail FROM student_profile WHERE user_id=? AND is_deleted=0 ORDER BY id LIMIT 1",
                rs -> rs.next() ? Map.of("intent", nz(rs.getString(1)), "detail", nz(rs.getString(2))) : null,
                userId);
        if (profile != null) {
            String text = !profile.get("intent").isBlank() ? profile.get("intent") : profile.get("detail");
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
                ? "按你的职业意向（" + source.get("keyword") + "）没有搜到匹配岗位：请调整「职业意向」的用词，或先生成一份职业报告"
                : null);
        result.put("items", items);
        return result;
    }

    /** 按关键词全库搜索岗位（精确匹配优先，其次短名优先）。 */
    public List<Map<String, Object>> search(List<String> words, int limit) {
        Map<Long, Map<String, Object>> found = new LinkedHashMap<>();
        for (String word : words) {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT id, job_name, company_name, industry, address, salary_range FROM job_info"
                            + " WHERE is_deleted=0 AND job_name LIKE ?"
                            + " ORDER BY (job_name = ?) DESC, CHAR_LENGTH(job_name), id LIMIT ?",
                    "%" + word + "%", word, Math.max(1, Math.min(limit, 50)));
            for (Map<String, Object> row : rows) {
                Long id = ((Number) row.get("id")).longValue();
                found.putIfAbsent(id, Map.of(
                        "jobId", String.valueOf(id),
                        "positionName", nz((String) row.get("job_name")),
                        "companyName", nz((String) row.get("company_name")),
                        "industry", nz((String) row.get("industry")),
                        "address", nz((String) row.get("address")),
                        "salaryRange", nz((String) row.get("salary_range")),
                        "matchedBy", word));
                if (found.size() >= limit) {
                    return new ArrayList<>(found.values());
                }
            }
        }
        return new ArrayList<>(found.values());
    }

    /** 冻结岗位快照：岗位基本信息 +（若已入库）岗位画像与技能/证书要求。 */
    public JsonNode snapshot(Long jobId) {
        if (jobId == null) {
            return null;
        }
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM job_info WHERE id=? AND is_deleted=0", jobId);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> job = rows.get(0);
        ObjectNode node = json.createObjectNode();
        node.put("jobId", String.valueOf(jobId));
        node.put("positionName", nz((String) job.get("job_name")));
        node.put("companyName", nz((String) job.get("company_name")));
        node.put("industry", nz((String) job.get("industry")));
        node.put("address", nz((String) job.get("address")));
        node.put("salaryRange", nz((String) job.get("salary_range")));
        String detail = nz((String) job.get("job_detail")).replaceAll("\\s+", " ").strip();
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
            node.put("category", nz((String) profile.get("category")));
            node.put("level", nz(String.valueOf(profile.get("level"))));
            node.put("profileDescription", nz((String) profile.get("description")));
            List<Map<String, Object>> skills = jdbc.queryForList(
                    "SELECT professional_skill, certificate_requirement FROM job_skill_requirement WHERE is_deleted=0 AND job_id=?",
                    profileId);
            if (!skills.isEmpty()) {
                node.put("skills", nz((String) skills.get(0).get("professional_skill")));
                node.put("certificate", nz((String) skills.get(0).get("certificate_requirement")));
            }
        }
        return node;
    }

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

    private static String nz(String value) {
        return value == null ? "" : value.strip();
    }
}
