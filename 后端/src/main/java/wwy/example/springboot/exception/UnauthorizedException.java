package wwy.example.springboot.exception;

// 401 - 未登录/登录失效
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}

