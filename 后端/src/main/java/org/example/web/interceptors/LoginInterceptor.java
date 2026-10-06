package org.example.web.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.example.web.tool.JwtUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    private final JdbcTemplate db;
    public LoginInterceptor(JdbcTemplate db) { this.db = db; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean knowledge = path.startsWith("/job-") || path.startsWith("/analysis/");
        if ("OPTIONS".equals(request.getMethod())) return true;
        if (knowledge && Set.of("GET", "HEAD").contains(request.getMethod())) return true;
        long userId;
        int role;
        try {
            var claims = JwtUtil.parseToken(request.getHeader("Authorization"));
            userId = Long.parseLong(String.valueOf(claims.get("id")));
            var rows = db.queryForList("SELECT user_role FROM user WHERE id=? AND user_status=1 AND is_deleted=0", userId);
            if (rows.size() != 1) return reject(response, 401, "登录已失效，请重新登录");
            role = ((Number) rows.get(0).get("user_role")).intValue();
        } catch (Exception e) { return reject(response, 401, "登录已失效，请重新登录"); }
        request.setAttribute("authenticatedUserId", userId);
        request.setAttribute("authenticatedUserRole", role);
        // Comparison only reads job data; all other knowledge writes change shared records.
        if ((knowledge && !"/job-compare/analyze-new-job".equals(path) || path.startsWith("/new/") || "/user/deleteById".equals(path)) && role != 2)
            return reject(response, 403, "需要管理员权限");
        return true;
    }

    private boolean reject(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}");
        return false;
    }
}
