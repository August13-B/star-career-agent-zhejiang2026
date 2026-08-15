package wwy.example.springboot.exception;

// 404 - 资源不存在
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
