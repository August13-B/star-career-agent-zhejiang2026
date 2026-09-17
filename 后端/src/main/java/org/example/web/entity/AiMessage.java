package org.example.web.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AiMessage {
    private Long id;
    private Long conversationId;
    private Integer messageType;
    private Integer contentType;
    private String content;
    private String audioUrl;
    private String imageUrl;
    private String contextInfo;
    private String modelName;
    private Integer responseTime;
    private Integer sequence;
    private LocalDateTime createTime;

    /** 百宝箱平台消息ID（CUSTOM 卡片事件携带） */
    private String tboxMessageId;
    /** 百宝箱请求ID（RUN_FINISHED.rawEvent.requestId，用于三方对账） */
    private String tboxRequestId;
}
