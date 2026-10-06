package org.example.web.service.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.web.service.training.TrainingContentCipher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测评结果落库：**只覆盖软实力六维**（硬实力四项从当前基线行原样复制保留），
 * 总分由后端重算 {@code 硬均值×30% + 软均值×70%}，并写入两张历史表（version+1）。
 *
 * <p>乐观并发保护（沿用训练模块口径）：会话开始时记录了基线行 id 与画像版本，
 * 若测评期间已被别处更新（例如又跑了一次训练）→ **跳过不覆盖**，只保留本次反馈。
 */
@Service
public class AssessmentOutcomeService {
    private static final List<String> HARD = AssessmentStore.HARD_DIMENSIONS;
    private static final List<String> SOFT = AssessmentStore.SOFT_DIMENSIONS;
    private static final List<String> ALL = List.of(
            "education", "internship", "professional", "certificate",
            "innovation", "learning", "pressure", "communication", "problem_solving", "teamwork");

    private final JdbcTemplate jdbc;
    private final TrainingContentCipher cipher;
    private final com.fasterxml.jackson.databind.ObjectMapper json;

    public AssessmentOutcomeService(JdbcTemplate jdbc, TrainingContentCipher cipher,
                                    com.fasterxml.jackson.databind.ObjectMapper json) {
        this.jdbc = jdbc;
        this.cipher = cipher;
        this.json = json;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Object> apply(Long userId, Long sessionId) {
        jdbc.queryForObject("SELECT id FROM user WHERE id=? FOR UPDATE", Long.class, userId);
        List<Map<String, Object>> sessions = jdbc.queryForList("SELECT * FROM assessment_session WHERE id=?", sessionId);
        List<Map<String, Object>> evaluations = jdbc.queryForList("SELECT * FROM assessment_evaluation WHERE session_id=?", sessionId);
        if (sessions.isEmpty() || evaluations.isEmpty() || !"valid".equals(evaluations.get(0).get("status"))) {
            return Map.of("status", "skipped", "message", "本次没有可用的有效评分，仅保留测评反馈");
        }
        Map<String, Object> session = sessions.get(0);
        JsonNode scored = read(cipher.decrypt((String) evaluations.get(0).get("result_json")));
        if (scored == null || !scored.path("dimensions").isObject()) {
            return Map.of("status", "skipped", "message", "评分结果不可用，仅保留测评反馈");
        }

        List<Map<String, Object>> profiles = jdbc.queryForList(
                "SELECT * FROM student_profile WHERE user_id=? AND is_deleted=0 FOR UPDATE", userId);
        List<Map<String, Object>> abilities = jdbc.queryForList(
                "SELECT * FROM student_ability WHERE user_id=? AND is_deleted=0 FOR UPDATE", userId);
        List<Map<String, Object>> scores = jdbc.queryForList(
                "SELECT * FROM student_ability_score WHERE user_id=? ORDER BY update_time DESC,id DESC FOR UPDATE", userId);
        List<Map<String, Object>> active = scores.stream()
                .filter(row -> number(row.get("is_deleted")) == 0 && number(row.get("score_type")) == 1).toList();
        if (profiles.size() != 1 || abilities.size() != 1 || active.size() != 1) {
            return Map.of("status", "skipped", "message", "当前能力基线不唯一或不存在，请先在个人中心补全基本情况");
        }
        Map<String, Object> profile = profiles.get(0), ability = abilities.get(0), old = active.get(0);
        Long baselineId = session.get("baseline_score_id") == null ? null : ((Number) session.get("baseline_score_id")).longValue();
        Integer baselineVersion = session.get("baseline_profile_version") == null
                ? null : ((Number) session.get("baseline_profile_version")).intValue();
        if (baselineId != null && (!baselineId.equals(numberLong(old.get("id")))
                || (baselineVersion != null && baselineVersion != number(profile.get("version"))))) {
            return Map.of("status", "skipped", "message", "测评期间能力基线或画像已更新，本次保留反馈、避免覆盖较新的数据");
        }

        Map<String, Object> before = new LinkedHashMap<>();
        Map<String, Object> after = new LinkedHashMap<>();
        for (String dimension : ALL) {
            int value = number(old.get(dimension + "_score"));
            before.put(dimension, value);
            after.put(dimension, SOFT.contains(dimension) ? scored.path("dimensions").path(dimension).asInt(value) : value);
        }
        double hardAverage = HARD.stream().mapToInt(d -> number(after.get(d))).average().orElse(60);
        double softAverage = SOFT.stream().mapToInt(d -> number(after.get(d))).average().orElse(60);
        double total = Math.round((hardAverage * 0.3 + softAverage * 0.7) * 10) / 10.0;
        after.put("total", total);

        int version = number(profile.get("version")) + 1;
        Integer historyMax = jdbc.queryForObject(
                "SELECT COALESCE(MAX(version),0) FROM student_profile_history WHERE user_id=?", Integer.class, userId);
        if (historyMax != null && historyMax >= version) {
            version = historyMax + 1;
        }
        String columns = String.join(",", ALL.stream().map(d -> d + "_score").toList());
        String placeholders = String.join(",", java.util.Collections.nCopies(ALL.size(), "?"));
        long scoreId = AssessmentStore.id(), profileHistoryId = AssessmentStore.id(), scoreHistoryId = AssessmentStore.id();
        jdbc.update("UPDATE student_ability_score SET is_deleted=1 WHERE id=?", old.get("id"));
        List<Object> values = new java.util.ArrayList<>();
        values.add(scoreId);
        values.add(userId);
        values.add(ability.get("id"));
        ALL.forEach(d -> values.add(after.get(d)));
        values.add(total);
        values.add("能力补充测评更新：本次只覆盖软实力六维，硬实力四项保持原有值");
        jdbc.update("INSERT INTO student_ability_score(id,user_id,ability_id," + columns + ",total_score,score_comment,score_type)"
                + " VALUES(" + placeholders + ",?,?,1)", values.toArray());
        jdbc.update("UPDATE student_profile SET version=?,update_time=CURRENT_TIMESTAMP WHERE id=?", version, profile.get("id"));
        Map<String, Object> snapshot = new LinkedHashMap<>(profile);
        snapshot.put("version", version);
        jdbc.update("INSERT INTO student_profile_history(id,profile_id,user_id,version,profile_data,change_reason) VALUES(?,?,?,?,?,?)",
                profileHistoryId, profile.get("id"), userId, version, write(snapshot), "能力补充测评");
        jdbc.update("INSERT INTO student_ability_score_history(id,user_id,ability_id,score_id,profile_history_id,version,"
                        + columns + ",total_score,industry_rank,peer_rank,score_type,score_comment,change_reason)"
                        + " SELECT ?,user_id,ability_id,id,?,?," + columns
                        + ",total_score,industry_rank,peer_rank,score_type,score_comment,? FROM student_ability_score WHERE id=?",
                scoreHistoryId, profileHistoryId, version, "能力补充测评", scoreId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "applied");
        result.put("version", version);
        result.put("before", before);
        result.put("after", after);
        result.put("message", "已更新本次覆盖的软实力六维（硬实力四项保持原值），总分 " + total);
        return result;
    }

    private JsonNode read(String value) {
        try {
            return value == null ? null : json.readTree(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new AssessmentException(500, "SERIALIZE_FAILED", "数据序列化失败");
        }
    }

    private static int number(Object value) {
        return value instanceof Number number ? number.intValue() : -1;
    }

    private static Long numberLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}
