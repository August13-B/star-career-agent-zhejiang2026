package org.example.web.dto.requset;


import lombok.Data;

/**
 * 发送给AI服务器的请求数据
 */
@Data
public class AIRequest {
    /**
     * 用户输入的文本
     */
    private String prompt;

    /**
     * 用户ID（可选）
     */
    private String userId;

    /**
     * 会话ID（可选）
     */
    private String sessionId;

    /**
     * 其他自定义参数
     */
    private Object parameters;
}
