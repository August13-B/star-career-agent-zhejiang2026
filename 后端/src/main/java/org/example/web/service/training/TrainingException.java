package org.example.web.service.training;

public class TrainingException extends RuntimeException {
    private final int status;
    private final String code;

    public TrainingException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }
    public String code() { return code; }
}
