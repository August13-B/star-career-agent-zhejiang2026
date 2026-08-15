package wwy.example.springboot.exception;

// 500 - 业务异常（可扩展）
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
