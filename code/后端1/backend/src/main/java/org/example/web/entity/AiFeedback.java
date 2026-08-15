package org.example.web.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class AiFeedback {
    /**
     * 反馈ID
     */
    private Long id;

    /**
     * 关联消息ID
     */
    private Long messageId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 反馈类型：1-点赞，2-点踩，3-详细反馈
     */
    private Integer feedbackType;

    /**
     * 评分（1-5分）
     */
    private Integer score;

    /**
     * 反馈内容
     */
    private String content;

    /**
     * 改进建议
     */
    private String improvementSuggestion;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
