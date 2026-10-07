package org.example.web.service.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 多智能体联合测评报告 → 能力画像的落库。
 *
 * <p>与其它写入方共用同一策略 {@link ProfileScorePolicy}（首次大幅、再次 ≤10、降低需理由），
 * 并记录 {@code change_source=report} 与变更明细（供「画像已更新」提示与个人中心展示）。
 * AI 调用在调用方完成，本类只做本地短事务。
 */
@Service
public class ReportProfileOutcomeService {

    private static final List<String> HARD = List.of("education", "internship", "professional", "certificate");
    private static final List<String> SOFT = List.of("innovation", "learning", "pressure", "communication", "problem_solving", "teamwork");
    private static final List<String> ALL = List.of(
            "education", "internship", "professional", "certificate",
            "innovation", "learning", "pressure", "communication", "problem_solving", "teamwork");

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public ReportProfileOutcomeService(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /**
     * @param suggestions 各维度建议分（仅包含"报告里有依据"的维度）
     * @param reasons     各维度依据（引用报告原文；降低时作为必填理由）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Object> apply(Long userId, Long reportId, Map<String, Integer> suggestions, Map<String, String> reasons) {
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
            throw new ResumeParseException("请先在个人中心补全基本情况");
        }
        Map<String, Object> profile = profiles.get(0);
        Map<String, Object> ability = abilities.get(0);
        Map<String, Object> old = active.get(0);

        boolean firstTime = ProfileScorePolicy.isFirstTime((String) old.get("change_source"));
        Map<String, Object> before = new LinkedHashMap<>();
        Map<String, Object> after = new LinkedHashMap<>();
        for (String dimension : ALL) {
            int value = number(old.get(dimension + "_score"));
            before.put(dimension, value);
            after.put(dimension, value);
        }
        String reason = "多智能体联合测评报告";
        for (String dimension : ALL) {
            Integer suggested = suggestions.get(dimension);
            if (suggested == null) {
                continue;
            }
            String evidence = reasons.getOrDefault(dimension, reason);
            int now = number(before.get(dimension));
            after.put(dimension, ProfileScorePolicy.clamp(firstTime, now, suggested, evidence));
        }

        double hard = HARD.stream().mapToInt(d -> number(after.get(d))).average().orElse(60);
        double soft = SOFT.stream().mapToInt(d -> number(after.get(d))).average().orElse(60);
        double total = ProfileScorePolicy.total((int) Math.round(hard), (int) Math.round(soft));
        after.put("total", total);
        Map<String, Object> deltas = ProfileChangeDetail.deltas(before, after);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", deltas.isEmpty() ? "unchanged" : "applied");
        result.put("reportId", String.valueOf(reportId));
        result.put("firstTime", firstTime);
        result.put("source", ProfileScorePolicy.SOURCE_REPORT);
        result.put("sourceLabel", ProfileChangeService.label(ProfileScorePolicy.SOURCE_REPORT));
        result.put("before", before);
        result.put("after", after);
        result.put("deltas", deltas);
        result.put("reasons", reasons);
        result.put("total", total);
        if (deltas.isEmpty()) {
            result.put("version", number(profile.get("version")));
            return result;
        }

        int version = bumpVersion(userId, profile);
        writeScore(userId, ability, old, after, total, version, firstTime, reason, before);
        result.put("version", version);
        return result;
    }

    private int bumpVersion(Long userId, Map<String, Object> profile) {
        Integer historyMax = jdbc.queryForObject(
                "SELECT COALESCE(MAX(version),0) FROM student_profile_history WHERE user_id=?", Integer.class, userId);
        int version = Math.max(number(profile.get("version")), historyMax == null ? 0 : historyMax) + 1;
        jdbc.update("UPDATE student_profile SET version=?, update_time=CURRENT_TIMESTAMP WHERE id=?", version, profile.get("id"));
        Map<String, Object> snapshot = new LinkedHashMap<>(profile);
        snapshot.put("version", version);
        jdbc.update("INSERT INTO student_profile_history(id,profile_id,user_id,version,profile_data,change_reason) VALUES(?,?,?,?,?,?)",
                org.example.web.tool.SnowIdCreater.generateId(25), profile.get("id"), userId, version,
                write(snapshot), "多智能体联合测评");
        return version;
    }

    private void writeScore(Long userId, Map<String, Object> ability, Map<String, Object> old,
                            Map<String, Object> after, double total, int version, boolean firstTime,
                            String reason, Map<String, Object> before) {
        String columns = String.join(",", ALL.stream().map(d -> d + "_score").toList());
        String placeholders = String.join(",", Collections.nCopies(ALL.size(), "?"));
        long scoreId = org.example.web.tool.SnowIdCreater.generateId(25);
        long scoreHistoryId = org.example.web.tool.SnowIdCreater.generateId(25);
        jdbc.update("UPDATE student_ability_score SET is_deleted=1 WHERE id=?", old.get("id"));
        List<Object> values = new ArrayList<>();
        values.add(scoreId);
        values.add(userId);
        values.add(ability.get("id"));
        ALL.forEach(d -> values.add(after.get(d)));
        values.add(total);
        values.add("多智能体联合测评更新");
        values.add(ProfileScorePolicy.SOURCE_REPORT);
        values.add(write(ProfileChangeDetail.of(firstTime, ProfileScorePolicy.SOURCE_REPORT, reason, before, after)));
        jdbc.update("INSERT INTO student_ability_score(id,user_id,ability_id," + columns
                + ",total_score,score_comment,score_type,change_source,change_detail) VALUES("
                + placeholders + ",?,?,1,?,?)", values.toArray());
        jdbc.update("INSERT INTO student_ability_score_history(id,user_id,ability_id,score_id,version,"
                        + columns + ",total_score,score_type,score_comment,change_reason)"
                        + " SELECT ?,user_id,ability_id,id,?," + columns
                        + ",total_score,score_type,score_comment,? FROM student_ability_score WHERE id=?",
                scoreHistoryId, version, "多智能体联合测评", scoreId);
    }

    private static int number(Object value) {
        return value instanceof Number number ? number.intValue() : -1;
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new ResumeParseException("画像变更明细序列化失败，请重试");
        }
    }
}
