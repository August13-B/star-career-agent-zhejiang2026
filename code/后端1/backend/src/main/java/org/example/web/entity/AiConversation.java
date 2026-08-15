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
}
