package org.example.web.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AiConversation {
    private Long id;
    private Long userId;
    private Integer conversationType;
    private String title;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;

    /** 百宝箱平台会话ID（/api/tbox/session 返回） */
    private String tboxSessionId;
    /** 百宝箱应用会话ID（/api/conversation/create 返回） */
    private String tboxConversationId;
}
