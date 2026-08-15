package org.example.web.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class JobPromotionGraph {
    /**
     * 非自增主键（雪花算法生成）
     */
    private Long id;

    /**
     * 主岗位id（关联岗位要求画像表）
     */
    private Long mainJobId;

    /**
     * 晋升岗位1-id（关联岗位要求画像表）
     */
    private Long promotionJob1Id;

    /**
     * 晋升岗位1-描述
     */
    private String promotionJob1Desc;

    /**
     * 晋升岗位1-技能差异
     */
    private String promotionJob1SkillDiff;

    /**
     * 晋升岗位1-经验要求
     */
    private String promotionJob1Experience;

    /**
     * 晋升岗位1-学习周期（月）
     */
    private Integer promotionJob1LearningCycle;

    /**
     * 晋升岗位2-id（关联岗位要求画像表）
     */
    private Long promotionJob2Id;

    /**
     * 晋升岗位2-描述
     */
    private String promotionJob2Desc;

    /**
     * 晋升岗位2-技能差异
     */
    private String promotionJob2SkillDiff;

    /**
     * 晋升岗位2-经验要求
     */
    private String promotionJob2Experience;

    /**
     * 晋升岗位2-学习周期（月）
     */
    private Integer promotionJob2LearningCycle;

    /**
     * 晋升岗位3-id（关联岗位要求画像表）
     */
    private Long promotionJob3Id;

    /**
     * 晋升岗位3-描述
     */
    private String promotionJob3Desc;

    /**
     * 晋升岗位3-技能差异
     */
    private String promotionJob3SkillDiff;

    /**
     * 晋升岗位3-经验要求
     */
    private String promotionJob3Experience;

    /**
     * 晋升岗位3-学习周期（月）
     */
    private Integer promotionJob3LearningCycle;

    /**
     * 晋升岗位4-id（关联岗位要求画像表）
     */
    private Long promotionJob4Id;

    /**
     * 晋升岗位4-描述
     */
    private String promotionJob4Desc;

    /**
     * 晋升岗位4-技能差异
     */
    private String promotionJob4SkillDiff;

    /**
     * 晋升岗位4-经验要求
     */
    private String promotionJob4Experience;

    /**
     * 晋升岗位4-学习周期（月）
     */
    private Integer promotionJob4LearningCycle;

    /**
     * 晋升岗位5-id（关联岗位要求画像表）
     */
    private Long promotionJob5Id;

    /**
     * 晋升岗位5-描述
     */
    private String promotionJob5Desc;

    /**
     * 晋升岗位5-技能差异
     */
    private String promotionJob5SkillDiff;

    /**
     * 晋升岗位5-经验要求
     */
    private String promotionJob5Experience;

    /**
     * 晋升岗位5-学习周期（月）
     */
    private Integer promotionJob5LearningCycle;

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
     * 删除标识（0-未删除，1-已删除）
     */
    private Integer isDeleted;
}
