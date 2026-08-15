package wwy.example.springboot.common;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor  // 添加无参构造器，便于 AIServiceImpl 中使用
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 成功 200
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    // 登录失效 401
    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message, null);
    }

    // 无权限 403
    public static <T> Result<T> forbidden(String message) {
        return new Result<>(403, message, null);
    }

    // 资源不存在 404
    public static <T> Result<T> notFound(String message) {
        return new Result<>(404, message, null);
    }

    // 网络错误 500
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
}