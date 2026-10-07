package org.example.web.service.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 简历解析结果的落库：
 * <ol>
 *   <li><b>并入个人资料</b>：不覆盖用户已填写的非空值；技能/证书/经历型字段去重追加；</li>
 *   <li><b>硬实力四项加分</b>：按 {@link ProfileScorePolicy#resumeBonus}（只加不减、单项 ≤+15，
 *       且仅首次评估或该维度此前为空时生效）；</li>
 *   <li>有分数变化时写出新的画像行（与其它写入方一致：软删旧行 + 版本号 + 两张历史表 + 变更留痕）。</li>
 * </ol>
 *
 * <p>本方法只做本地短事务；**AI 调用在调用方完成，绝不在事务内**（沿用测评/训练模块口径）。
 */
@Service
public class ResumeOutcomeService {

    private static final List<String> HARD = List.of("education", "internship", "professional", "certificate");
    private static final List<String> SOFT = List.of("innovation", "learning", "pressure", "communication", "problem_solving", "teamwork");
    private static final List<String> ALL = List.of(
            "education", "internship", "professional", "certificate",
            "innovation", "learning", "pressure", "communication", "problem_solving", "teamwork");
    /** 硬实力四项分值所对应的资料字段（用于判断"该维度此前是否为空"）。 */
    private static final List<String> HARD_SOURCE_FIELDS = List.of("education", "work_experience", "skill", "certificate");

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public ResumeOutcomeService(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Object> apply(Long userId, ResumeFields fields) {
        jdbc.queryForObject("SELECT id FROM user WHERE id=? FOR UPDATE", Long.class, userId);
        List<Map<String, Object>> profiles = jdbc.queryForList(
                "SELECT * FROM student_profile WHERE user_id=? AND is_deleted=0 FOR UPDATE", userId);
        List<Map<String, Object>> abilities = jdbc.queryForList(
                "SELECT * FROM student_ability WHERE user_id=? AND is_deleted=0 FOR UPDATE", userId);
        List<Map<String, Object>> scores = jdbc.queryForList(
                "SELECT * FROM student_ability_score WHERE user_id=? ORDER BY update_time DESC,id DESC FOR UPDATE", userId);
        List<Map<String, Object>> active = scores.stream()
                .filter(row -> number(row.get("is_deleted")) == 0 && number(row.get("score_type")) == 1).toList();
        if (profiles.size() != 1 || abilities.size() != 1 || active.size() != 1) {
            throw new ResumeParseException("当前能力画像不唯一或不存在，请先在个人中心补全基本情况后再上传简历");
        }
        Map<String, Object> profile = profiles.get(0);
        Map<String, Object> ability = abilities.get(0);
        Map<String, Object> old = active.get(0);

        // ① 并入个人资料（不覆盖非空；列表/经历型去重追加）
        Map<String, Object> updates = new LinkedHashMap<>();
        fillIfBlank(updates, profile, "education", fields.education());
        fillIfBlank(updates, profile, "major", fields.major());
        appendList(updates, profile, "skill", fields.skill());
        appendList(updates, profile, "certificate", fields.certificate());
        appendParagraph(updates, profile, "work_experience", fields.workExperience());
        appendParagraph(updates, profile, "project_experience", fields.projectExperience());
        List<String> filled = new ArrayList<>(updates.keySet());
        if (!updates.isEmpty()) {
            String sets = String.join(",", updates.keySet().stream().map(column -> column + "=?").toList());
            List<Object> values = new ArrayList<>(updates.values());
            values.add(profile.get("id"));
            jdbc.update("UPDATE student_profile SET " + sets + ", update_time=CURRENT_TIMESTAMP WHERE id=?", values.toArray());
        }

        // ② 硬实力四项加分（只加不减）
        boolean firstTime = ProfileScorePolicy.isFirstTime((String) old.get("change_source"));
        Map<String, Object> before = new LinkedHashMap<>();
        Map<String, Object> after = new LinkedHashMap<>();
        for (String dimension : ALL) {
            int value = number(old.get(dimension + "_score"));
            before.put(dimension, value);
            after.put(dimension, value);
        }
        List<String> scoreNotes = new ArrayList<>();
        for (int i = 0; i < HARD.size(); i++) {
            String dimension = HARD.get(i);
            Integer suggested = fields.scores().get(dimension);
            if (suggested == null) {
                continue;
            }
            boolean dimensionEmpty = isBlank(profile.get(HARD_SOURCE_FIELDS.get(i)));
            int now = number(before.get(dimension));
            int result = ProfileScorePolicy.resumeBonus(firstTime, dimensionEmpty, now, suggested);
            if (result != now) {
                after.put(dimension, result);
                scoreNotes.add(dimension + " " + now + "→" + result);
            }
        }

        // ③ 有变化才写新画像行（无变化只更新资料，避免无谓地让别的会话基线失效）
        boolean changed = !scoreNotes.isEmpty();
        double hard = HARD.stream().mapToInt(d -> number(after.get(d))).average().orElse(60);
        double soft = SOFT.stream().mapToInt(d -> number(after.get(d))).average().orElse(60);
        double total = ProfileScorePolicy.total((int) Math.round(hard), (int) Math.round(soft));
        after.put("total", total);
        int version = number(profile.get("version"));
        if (changed) {
            version = bumpVersion(userId, profile, version);
            writeScore(userId, ability, old, after, total, version, firstTime,
                    "简历解析补充：" + String.join("、", scoreNotes));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", changed ? "applied" : "profile_only");
        result.put("filledFields", filled);
        result.put("parsed", Map.of(
                "education", fields.education(),
                "major", fields.major(),
                "skill", fields.skill(),
                "certificate", fields.certificate(),
                "workExperience", fields.workExperience(),
                "projectExperience", fields.projectExperience()));
        result.put("firstTime", firstTime);
        result.put("source", ProfileScorePolicy.SOURCE_RESUME);
        result.put("before", before);
        result.put("after", after);
        result.put("deltas", ProfileChangeDetail.deltas(before, after));
        result.put("reasons", fields.reasons());
        result.put("version", version);
        result.put("message", changed
                ? "已补充个人资料并同步更新能力画像，总分 " + total
                : "已补充个人资料");
        return result;
    }

    /** 版本号推进：与画像历史表取最大，保持既有不变式。 */
    private int bumpVersion(Long userId, Map<String, Object> profile, int current) {
        Integer historyMax = jdbc.queryForObject(
                "SELECT COALESCE(MAX(version),0) FROM student_profile_history WHERE user_id=?", Integer.class, userId);
        int version = Math.max(current, historyMax == null ? 0 : historyMax) + 1;
        jdbc.update("UPDATE student_profile SET version=?, update_time=CURRENT_TIMESTAMP WHERE id=?", version, profile.get("id"));
        Map<String, Object> snapshot = new LinkedHashMap<>(profile);
        snapshot.put("version", version);
        jdbc.update("INSERT INTO student_profile_history(id,profile_id,user_id,version,profile_data,change_reason) VALUES(?,?,?,?,?,?)",
                org.example.web.tool.SnowIdCreater.generateId(24), profile.get("id"), userId, version,
                write(snapshot), "简历解析补充资料");
        return version;
    }

    /** 写出新的当前画像行（软删旧行）+ 分数历史，并记录变更来源与明细。 */
    private void writeScore(Long userId, Map<String, Object> ability, Map<String, Object> old,
                            Map<String, Object> after, double total, int version, boolean firstTime, String reason) {
        String columns = String.join(",", ALL.stream().map(d -> d + "_score").toList());
        String placeholders = String.join(",", Collections.nCopies(ALL.size(), "?"));
        long scoreId = org.example.web.tool.SnowIdCreater.generateId(24);
        long scoreHistoryId = org.example.web.tool.SnowIdCreater.generateId(24);
        Map<String, Object> before = new LinkedHashMap<>();
        for (String dimension : ALL) {
            before.put(dimension, number(old.get(dimension + "_score")));
        }
        jdbc.update("UPDATE student_ability_score SET is_deleted=1 WHERE id=?", old.get("id"));
        List<Object> values = new ArrayList<>();
        values.add(scoreId);
        values.add(userId);
        values.add(ability.get("id"));
        ALL.forEach(d -> values.add(after.get(d)));
        values.add(total);
        values.add("简历解析更新");
        values.add(ProfileScorePolicy.SOURCE_RESUME);
        values.add(write(ProfileChangeDetail.of(firstTime, ProfileScorePolicy.SOURCE_RESUME, reason, before, after)));
        jdbc.update("INSERT INTO student_ability_score(id,user_id,ability_id," + columns
                + ",total_score,score_comment,score_type,change_source,change_detail) VALUES("
                + placeholders + ",?,?,1,?,?)", values.toArray());
        jdbc.update("INSERT INTO student_ability_score_history(id,user_id,ability_id,score_id,version,"
                        + columns + ",total_score,score_type,score_comment,change_reason)"
                        + " SELECT ?,user_id,ability_id,id,?," + columns
                        + ",total_score,score_type,score_comment,? FROM student_ability_score WHERE id=?",
                scoreHistoryId, version, "简历解析", scoreId);
    }

    // ── 合并规则（静态、可单测）────────────────────────────────────────

    /** 仅在原字段为空时写入（不覆盖用户已填内容）。 */
    static void fillIfBlank(Map<String, Object> updates, Map<String, Object> profile, String column, String value) {
        String incoming = value == null ? "" : value.strip();
        if (incoming.isEmpty() || !isBlank(profile.get(column))) {
            return;
        }
        updates.put(column, incoming);
    }

    /** 列表型字段（技能/证书）：拆分已有条目去重后追加。 */
    static void appendList(Map<String, Object> updates, Map<String, Object> profile, String column, List<String> incoming) {
        String current = text(profile.get(column));
        Set<String> known = new LinkedHashSet<>(split(current));
        List<String> added = new ArrayList<>();
        for (String item : incoming) {
            String value = clean(item);
            if (value.isEmpty() || known.contains(value)) {
                continue;
            }
            known.add(value);
            added.add(value);
        }
        if (!added.isEmpty()) {
            updates.put(column, current.isEmpty() ? String.join("、", added) : current + "、" + String.join("、", added));
        }
    }

    /** 经历型长文本：整体不重复则换行追加。 */
    static void appendParagraph(Map<String, Object> updates, Map<String, Object> profile, String column, List<String> incoming) {
        String current = text(profile.get(column));
        List<String> added = new ArrayList<>();
        for (String item : incoming) {
            String value = clean(item);
            if (value.isEmpty() || current.contains(value)) {
                continue;
            }
            added.add(value);
        }
        if (!added.isEmpty()) {
            updates.put(column, current.isEmpty() ? String.join("\n", added) : current + "\n" + String.join("\n", added));
        }
    }

    /** 拆分已有条目：支持 、，,；; 与换行。 */
    static List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> items = new ArrayList<>();
        for (String part : value.split("[、,，;；\\n]")) {
            String item = clean(part);
            if (!item.isEmpty()) {
                items.add(item);
            }
        }
        return items;
    }

    private static String clean(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").strip();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).strip();
    }

    private static boolean isBlank(Object value) {
        return text(value).isEmpty();
    }

    private static int number(Object value) {
        return value instanceof Number number ? number.intValue() : -1;
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new ResumeParseException("解析结果序列化失败，请重试");
        }
    }
}
