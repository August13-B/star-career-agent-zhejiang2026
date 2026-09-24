package org.example.web.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** HTTP boundaries check the authenticated subject before invoking legacy services. */
@Component
public class AccessGuard {
    private final JdbcTemplate db;
    private final HttpServletRequest request;
    private static final Set<String> TABLES = Set.of("student_profile", "student_ability",
        "student_ability_score", "student_image", "ai_conversation", "career_report", "match_record", "grow_plan");

    public AccessGuard(JdbcTemplate db, HttpServletRequest request) { this.db = db; this.request = request; }

    public long current() {
        Object id = request.getAttribute("authenticatedUserId");
        if (!(id instanceof Long value)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请重新登录");
        return value;
    }

    public long self(Object supplied) {
        long user = current();
        if (supplied != null && number(supplied) != user)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问其他账号的数据");
        return user;
    }

    public void admin() {
        current();
        if (!Integer.valueOf(2).equals(request.getAttribute("authenticatedUserRole")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要管理员权限");
    }

    public long number(Object value) {
        try {
            long id = Long.parseLong(String.valueOf(value));
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID 格式错误"); }
    }

    public void owned(String table, Object id) {
        if (!TABLES.contains(table)) throw new IllegalArgumentException("Unsupported ownership table");
        Integer count = db.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id=? AND user_id=? AND is_deleted=0",
            Integer.class, number(id), current());
        if (count == null || count != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在或无权访问");
    }

    public void optionalOwned(String table, Object id) { if (id != null) owned(table, id); }
    public void batch(String table, List<?> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "每批需包含 1–100 条记录");
        for (Object id : ids) owned(table, id);
    }
    public void task(Object id) {
        Integer count = db.queryForObject("SELECT COUNT(*) FROM grow_task t JOIN grow_plan p ON p.id=t.plan_id WHERE t.id=? AND p.user_id=? AND t.is_deleted=0 AND p.is_deleted=0",
            Integer.class, number(id), current());
        if (count == null || count != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在或无权访问");
    }
    public void conversation(Map<String,Object> body) {
        Object supplied = body.containsKey("user_id") ? body.get("user_id") : body.get("userId");
        long user = self(supplied);
        body.put("user_id", user); body.put("userId", user);
        optionalOwned("ai_conversation", body.containsKey("conversation_id") ? body.get("conversation_id") : body.get("conversationId"));
    }
}
