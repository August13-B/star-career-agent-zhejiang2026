package wwy.example.springboot.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import wwy.example.springboot.common.Result;

@Slf4j
@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler(UnauthorizedException.class)
    public Result<Void> handleUnauthorized(UnauthorizedException e) {
        log.warn("未授权访问：{}", e.getMessage());
        return Result.unauthorized(e.getMessage());   // 401
    }

    @ExceptionHandler(ForbiddenException.class)
    public Result<Void> handleForbidden(ForbiddenException e) {
        log.warn("无权限访问：{}", e.getMessage());
        return Result.forbidden(e.getMessage());      // 403
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Result<Void> handleResourceNotFound(ResourceNotFoundException e) {
        log.warn("资源不存在：{}", e.getMessage());
        return Result.notFound(e.getMessage());       // 404
    }

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(e.getMessage());          // 500
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("网络错误");               // 500
    }
}