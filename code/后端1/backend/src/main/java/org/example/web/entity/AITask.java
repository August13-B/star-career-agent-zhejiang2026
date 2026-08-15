package org.example.web.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AITask {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 关联成长计划ID
     */
    private Long planId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务类型：1技能 2证书 3项目 4实习
     */
    private Integer taskType;

    /**
     * 任务说明
     */
    private String taskDesc;

    /**
     * 学习资源链接（JSON格式存储）
     */
    private String resource;

    /**
     * 目标能力维度
     */
    private String targetAbility;

    /**
     * 预期成果
     */
    private String expectedOutcome;

    /**
     * 开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime startDate;

    /**
     * 结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime endDate;

    /**
     * 执行进度(%)
     */
    private BigDecimal progress;

    /**
     * 0未开始 1进行中 2已完成 3已暂停 4已延期
     */
    private Integer status;

    /**
     * 完成情况详情
     */
    private String completionDetail;

    /**
     * 实际完成日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completionDate;

    /**
     * 效果评估
     */
    private String effectEvaluation;

    /**
     * 效果评分（1-5分）
     */
    private Integer effectScore;

    /**
     * 动态调整原因
     */
    private String adjustmentReason;

    /**
     * 调整历史记录（JSON格式）
     */
    private String adjustmentHistory;

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