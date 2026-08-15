package org.example.web.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class JobRequirementProfile {
    /**
     * 岗位需求主键ID
     */
    private Long id;

    /**
     * 岗位名称
     */
    private String positionName;

    /**
     * 岗位大类(技术/产品/运营)
     */
    private String category;

    /**
     * 所属行业
     */
    private String industry;

    /**
     * 岗位基础描述
     */
    private String description;

    /**
     * 职级：1-入门 2-中级 3-高级
     */
    private Integer level;

    /**
     * 硬门槛权重(%)
     */
    private BigDecimal hardWeight;

    /**
     * 专业技能权重(%)
     */
    private BigDecimal skillWeight;

    /**
     * 软实力权重(%)
     */
    private BigDecimal softWeight;

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
