package org.example.web.service.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.web.service.training.TrainingContentCipher;
import org.example.web.tool.SnowIdCreater;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 能力补充测评的存储层（JdbcTemplate + Map，沿用训练模块风格）。
 *
 * <p>三张表（迁移 014）：{@code assessment_session} / {@code assessment_turn} / {@code assessment_evaluation}。
 * 题干、选项、作答、评分结果均为 AES-GCM 密文（{@link TrainingContentCipher}，与训练同一套存储密钥）。
 */
@Component
public class AssessmentStore {
    public static final List<String> SOFT_DIMENSIONS = List.of(
            "communication", "teamwork", "problem_solving", "innovation", "learning", "pressure");
    public static final List<String> HARD_DIMENSIONS = List.of(
            "education", "internship", "professional", "certificate");

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final TrainingContentCipher cipher;

    public AssessmentStore(JdbcTemplate jdbc, ObjectMapper json, TrainingContentCipher cipher) {
        this.jdbc = jdbc;
        this.json = json;
        this.cipher = cipher;
    }

    /** 雪花 ID 类别必须落在 0–31（20 已被训练模块占用，这里用 21）。 */
    public static long id() {
        return SnowIdCreater.generateId(21);
    }

    // ====================== 会话 ======================

    public Map<String, Object> session(Long sessionId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM assessment_session WHERE id=?", sessionId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> ownedSession(Long sessionId, Long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM assessment_session WHERE id=? AND user_id=?", sessionId, userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> lockOwnedSession(Long sessionId, Long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM assessment_session WHERE id=? AND user_id=? FOR UPDATE", sessionId, userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> requestedSession(Long userId, String requestId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM assessment_session WHERE user_id=? AND client_request_id=?", userId, requestId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insertSession(Long id, Long userId, String requestId, List<Map<String, Object>> plan,
                              int objectiveTarget, int subjectiveTarget, JsonNode hard, JsonNode context,
                              Long baselineScoreId, Integer baselineProfileVersion) {
        jdbc.update("INSERT INTO assessment_session(id,user_id,status,version,objective_target,subjective_target,"
                        + "answered_count,plan,hard_snapshot,context_snapshot,baseline_score_id,baseline_profile_version,"
                        + "draft,client_request_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id, userId, "active", 0, objectiveTarget, subjectiveTarget, 0, write(plan), hard.toString(),
                context.toString(), baselineScoreId, baselineProfileVersion, cipher.encrypt(""), requestId);
    }

    public void touchSession(Long sessionId, String status, int incrementAnswered, Long currentTurnId) {
        jdbc.update("UPDATE assessment_session SET status=?,answered_count=answered_count+?,current_turn_id=?,"
                        + "version=version+1,update_time=CURRENT_TIMESTAMP(3) WHERE id=?",
                status, incrementAnswered, currentTurnId, sessionId);
    }

    public com.fasterxml.jackson.databind.ObjectMapper json() {
        return json;
    }

    /** 清空草稿（提交作答后；不做版本校验，避免与前端自动保存竞争） */
    public void clearDraft(Long sessionId) {
        jdbc.update("UPDATE assessment_session SET draft=?,draft_version=draft_version+1,update_time=CURRENT_TIMESTAMP(3) WHERE id=?",
                cipher.encrypt(""), sessionId);
    }

    public void saveDraft(Long sessionId, String content, int expectedVersion) {
        if (jdbc.update("UPDATE assessment_session SET draft=?,draft_version=draft_version+1,update_time=CURRENT_TIMESTAMP(3)"
                + " WHERE id=? AND draft_version=?", cipher.encrypt(content), sessionId, expectedVersion) != 1) {
            throw new AssessmentException(409, "DRAFT_CONFLICT", "草稿已在其他页面修改，请先读取最新草稿");
        }
    }

    public void finishSession(Long sessionId, String status) {
        jdbc.update("UPDATE assessment_session SET status=?,current_turn_id=NULL,update_time=CURRENT_TIMESTAMP(3) WHERE id=?",
                status, sessionId);
    }

    public List<Map<String, Object>> sessions(Long userId, int offset, int limit) {
        return jdbc.queryForList("SELECT id,status,objective_target,subjective_target,answered_count,create_time,update_time"
                + " FROM assessment_session WHERE user_id=? ORDER BY create_time DESC LIMIT ? OFFSET ?", userId, limit, offset);
    }

    public int sessionCount(Long userId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM assessment_session WHERE user_id=?", Integer.class, userId);
        return count == null ? 0 : count;
    }

    // ====================== 轮次 ======================

    public List<Map<String, Object>> turns(Long sessionId) {
        return jdbc.queryForList("SELECT * FROM assessment_turn WHERE session_id=? ORDER BY ordinal", sessionId);
    }

    public Map<String, Object> turn(Long turnId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM assessment_turn WHERE id=?", turnId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> lockCurrentTurn(Long sessionId, Long turnId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM assessment_turn WHERE id=? AND session_id=? FOR UPDATE", turnId, sessionId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public long insertTurn(Long sessionId, int ordinal, int questionNo, String kind, String dimension,
                           boolean followUp, String question, JsonNode options, int limitSeconds) {
        long id = id();
        jdbc.update("INSERT INTO assessment_turn(id,session_id,ordinal,question_no,kind,dimension,follow_up,question,options,status,limit_seconds,started_at)"
                        + " VALUES(?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP(3))",
                id, sessionId, ordinal, questionNo, kind, dimension, followUp ? 1 : 0,
                cipher.encrypt(question), options == null ? null : cipher.encrypt(options.toString()), "asking", limitSeconds);
        return id;
    }

    public void answerTurn(Long turnId, String answer, Integer chosen, String status) {
        jdbc.update("UPDATE assessment_turn SET answer=?,chosen=?,status=?,answered_at=CURRENT_TIMESTAMP(3) WHERE id=?",
                answer == null ? null : cipher.encrypt(answer), chosen, status, turnId);
    }

    /**
     * 客观题维度分复算：{@code 实选分值之和 ÷ 满分之和}（仅统计**已作答**的题；
     * 未作答/超时的题不计入分子分母，避免与评语里的「超时」提醒双重惩罚）。
     */
    public int[] objectiveTally(Long sessionId, String dimension) {
        int got = 0, max = 0;
        for (Map<String, Object> row : jdbc.queryForList(
                "SELECT options,chosen FROM assessment_turn WHERE session_id=? AND kind='objective' AND dimension=?",
                sessionId, dimension)) {
            JsonNode options = options(row);
            Object chosen = row.get("chosen");
            if (options == null || !options.isArray() || options.isEmpty() || !(chosen instanceof Number number)) {
                continue;
            }
            int index = number.intValue();
            if (index < 0 || index >= options.size()) {
                continue;
            }
            int full = 0;
            for (JsonNode option : options) {
                full += option.path("score").asInt(0);
            }
            got += options.get(index).path("score").asInt(0);
            max += full;
        }
        return new int[]{got, max};
    }

    // ====================== 评分 ======================

    public Map<String, Object> evaluation(Long sessionId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM assessment_evaluation WHERE session_id=?", sessionId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void insertEvaluation(Long sessionId, String status, JsonNode result, JsonNode objective, String message) {
        jdbc.update("INSERT INTO assessment_evaluation(id,session_id,status,result_json,objective_json,message) VALUES(?,?,?,?,?,?)",
                id(), sessionId, status, cipher.encrypt(result.toString()),
                objective == null ? null : cipher.encrypt(objective.toString()), message);
    }

    // ====================== 工具 ======================

    public String decrypt(String value) {
        return cipher.decrypt(value);
    }

    public JsonNode read(String value) {
        try {
            return value == null ? null : json.readTree(value);
        } catch (Exception e) {
            return null;
        }
    }

    public String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new AssessmentException(500, "SERIALIZE_FAILED", "数据序列化失败");
        }
    }

    /** 面向接口/前端的轮次视图（数值型 id 一律转字符串，避免前端精度丢失；客观题**不下发选项分值**）。 */
    /**
     * 读取轮次选项：库里是 **AES-GCM 密文**，必须先解密再解析。
     *
     * <p>曾因这里漏解密（直接当 JSON 解析）导致：前端**客观题没有选项**、提交时校验失败、
     * 客观题维度分复算与证据目录全部拿不到选项。
     */
    public JsonNode options(Map<String, Object> row) {
        Object raw = row == null ? null : row.get("options");
        if (raw == null) {
            return null;
        }
        String text = String.valueOf(raw);
        return read(text.startsWith("g1:") ? decrypt(text) : text);
    }

    public Map<String, Object> turnView(Map<String, Object> row, boolean withAnswer) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("turnId", String.valueOf(row.get("id")));
        view.put("ordinal", ((Number) row.get("ordinal")).intValue());
        view.put("questionNo", ((Number) row.get("question_no")).intValue());
        view.put("kind", row.get("kind"));
        view.put("dimension", row.get("dimension"));
        view.put("followUp", ((Number) row.get("follow_up")).intValue() == 1);
        view.put("question", decrypt((String) row.get("question")));
        view.put("status", row.get("status"));
        view.put("limitSeconds", ((Number) row.get("limit_seconds")).intValue());
        view.put("startedAt", String.valueOf(row.get("started_at")));
        JsonNode options = options(row);
        if (options != null && options.isArray()) {
            List<Map<String, Object>> safe = new ArrayList<>();
            for (int i = 0; i < options.size(); i++) {
                Map<String, Object> option = new LinkedHashMap<>();
                option.put("index", i);
                option.put("text", options.get(i).path("text").asText(""));
                safe.add(option);
            }
            view.put("options", safe);
        }
        if (withAnswer && row.get("answer") != null) {
            view.put("answer", decrypt((String) row.get("answer")));
        }
        if (row.get("chosen") != null) {
            view.put("chosen", ((Number) row.get("chosen")).intValue());
        }
        return view;
    }

    public Map<String, Object> evaluationView(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("status", row.get("status"));
        view.put("message", row.get("message"));
        view.put("createdAt", String.valueOf(row.get("create_time")));
        view.put("result", read(decrypt((String) row.get("result_json"))));
        view.put("objective", row.get("objective_json") == null ? null : read(decrypt((String) row.get("objective_json"))));
        return view;
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
