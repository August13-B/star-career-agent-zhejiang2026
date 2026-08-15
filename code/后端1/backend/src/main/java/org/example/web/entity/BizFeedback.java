package org.example.web.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class BizFeedback {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 导师ID
     */
    private Long fromUserId;

    /**
     * 学生ID
     */
    private Long toUserId;

    /**
     * 关联匹配结果
     */
    private Long matchId;

    /**
     * 关联职业报告
     */
    private Long reportId;

    /**
     * 反馈类型：1简历点评 2规划建议 3面试评价
     */
    private Integer feedbackType;

    /**
     * 反馈内容
     */
    private String content;

    /**
     * 评分（1-5分）
     */
    private Integer score;

    /**
     * 是否已读：0-未读，1-已读
     */
    private Integer isRead;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    private Integer isDeleted;
}
