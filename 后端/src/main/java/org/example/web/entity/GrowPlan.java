package org.example.web.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class GrowPlan {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 来自匹配结果
     */
    private Long matchId;

    /**
     * 关联职业报告ID
     */
    private Long reportId;

    /**
     * 目标岗位
     */
    private String targetJob;

    /**
     * 计划名称
     */
    private String planName;

    /**
     * 成长建议
     */
    private String planContent;

    /**
     * 计划类型：1-短期（3个月），2-中期（6个月），3-长期（1年）
     */
    private Integer planType;

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
     * 0未开始 1进行中 2已完成 3已暂停
     */
    private Integer totalStatus;

    /**
     * 总进度(%)
     */
    private BigDecimal progress;

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
