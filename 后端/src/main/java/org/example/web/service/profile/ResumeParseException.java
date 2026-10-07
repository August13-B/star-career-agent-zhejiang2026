package org.example.web.service.profile;

/** 简历解析失败（用户可读原因，直接回显到前端）。 */
public class ResumeParseException extends RuntimeException {
    public ResumeParseException(String message) {
        super(message);
    }
}
