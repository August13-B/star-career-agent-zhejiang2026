package wwy.example.springboot.exception;

// 403 - 无权限
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
