package org.example.web.service.assessment;

/** 能力补充测评的业务异常（带 HTTP 语义的错误码，供控制器映射）。 */
public class AssessmentException extends RuntimeException {
    private final int status;
    private final String code;

    public AssessmentException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }
}
