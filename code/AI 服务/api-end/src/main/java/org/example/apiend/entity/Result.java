package org.example.apiend.entity;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <E> Result<E> success(E data) {
        return new Result<E>(10001, "操作成功", data);
    }

    public static <E> Result<E> success() {
        return new Result<E>(10001, "操作成功", null);
    }

    public static <E> Result<E> success(String message, E data) {
        return new Result<E>(10001, message, data);
    }

    public static <E> Result<E> error(String message) {
        return new Result<E>(10002, message, null);
    }

    public static <E> Result<E> error(String message, E data) {
        return new Result<E>(10002, message, data);
    }
}
