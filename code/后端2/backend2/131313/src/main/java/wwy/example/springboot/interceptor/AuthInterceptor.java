/*
package wwy.example.springboot.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import wwy.example.springboot.exception.ForbiddenException;
import wwy.example.springboot.exception.UnauthorizedException;

// 示例拦截器
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token == null) {
            // 抛出异常，由全局异常处理器处理
            throw new UnauthorizedException("请先登录");
        }
        // 验证 token...
        boolean valid = validateToken(token);
        if (!valid) {
            throw new ForbiddenException("无权限访问");
        }
        return true;
    }

    // 示例验证方法（需自行实现）
    private boolean validateToken(String token) {
        // 这里写 token 验证逻辑，返回 true 表示有效
        return true; // 临时返回 true，实际应替换为真实校验
    }
}
 */