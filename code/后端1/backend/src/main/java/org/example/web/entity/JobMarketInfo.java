package org.example.web.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class JobMarketInfo {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 关联岗位主表ID
     */
    private Long jobId;

    /**
     * 所属行业
     */
    private String industry;

    /**
     * 应届生入门门槛
     */
    private String entryThreshold;

    /**
     * 市场供需情况：1-供大于求，2-供需平衡，3-供不应求
     */
    private Integer marketSupplyDemand;

    /**
     * 供需比
     */
    private BigDecimal supplyDemandRatio;

    /**
     * 应届生起薪范围
     */
    private String salaryEntry;

    /**
     * 1年经验薪资范围
     */
    private String salary1year;

    /**
     * 3年经验薪资范围
     */
    private String salary3year;

    /**
     * 5年经验薪资范围
     */
    private String salary5year;

    /**
     * 薪资发展趋势描述
     */
    private String salaryTrend;

    /**
     * 专业适配度（JSON格式存储各专业适配度）
     */
    private String majorAdaptation;

    /**
     * 城市分布（JSON格式存储各城市占比）
     */
    private String cityDistribution;

    /**
     * 竞争激烈程度：1-低，2-中，3-高，4-极高
     */
    private Integer competitionLevel;

    /**
     * 岗位增长率(%)
     */
    private BigDecimal growthRate;

    /**
     * 未来前景描述
     */
    private String futureOutlook;

    /**
     * 数据来源
     */
    private String dataSource;

    /**
     * 数据更新日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime dataUpdateDate;

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
