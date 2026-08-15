package org.example.web.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class JobTransferGraph {
    /**
     * 非自增主键（雪花算法生成）
     */
    private Long id;

    /**
     * 主岗位id（关联岗位要求画像表）
     */
    private Long mainJobId;

    /**
     * 换岗岗位1-id（必填，关联岗位要求画像表）
     */
    private Long transferJob1Id;

    /**
     * 换岗岗位1-描述（必填）
     */
    private String transferJob1Desc;

    /**
     * 换岗岗位1-技能差异
     */
    private String transferJob1SkillDiff;

    /**
     * 换岗岗位1-学历要求
     */
    private String transferJob1Education;

    /**
     * 换岗岗位1-经验要求
     */
    private String transferJob1Experience;

    /**
     * 换岗岗位1-学习周期（月）
     */
    private Integer transferJob1LearningCycle;

    /**
     * 换岗岗位1-适配难度：1-低，2-中，3-高
     */
    private Integer transferJob1Difficulty;

    /**
     * 换岗岗位2-id（必填，关联岗位要求画像表）
     */
    private Long transferJob2Id;

    /**
     * 换岗岗位2-描述（必填）
     */
    private String transferJob2Desc;

    /**
     * 换岗岗位2-技能差异
     */
    private String transferJob2SkillDiff;

    /**
     * 换岗岗位2-学历要求
     */
    private String transferJob2Education;

    /**
     * 换岗岗位2-经验要求
     */
    private String transferJob2Experience;

    /**
     * 换岗岗位2-学习周期（月）
     */
    private Integer transferJob2LearningCycle;

    /**
     * 换岗岗位2-适配难度：1-低，2-中，3-高
     */
    private Integer transferJob2Difficulty;

    /**
     * 换岗岗位3-id（关联岗位要求画像表）
     */
    private Long transferJob3Id;

    /**
     * 换岗岗位3-描述
     */
    private String transferJob3Desc;

    /**
     * 换岗岗位3-技能差异
     */
    private String transferJob3SkillDiff;

    /**
     * 换岗岗位3-学历要求
     */
    private String transferJob3Education;

    /**
     * 换岗岗位3-经验要求
     */
    private String transferJob3Experience;

    /**
     * 换岗岗位3-学习周期（月）
     */
    private Integer transferJob3LearningCycle;

    /**
     * 换岗岗位3-适配难度：1-低，2-中，3-高
     */
    private Integer transferJob3Difficulty;

    /**
     * 换岗岗位4-id（关联岗位要求画像表）
     */
    private Long transferJob4Id;

    /**
     * 换岗岗位4-描述
     */
    private String transferJob4Desc;

    /**
     * 换岗岗位4-技能差异
     */
    private String transferJob4SkillDiff;

    /**
     * 换岗岗位4-学历要求
     */
    private String transferJob4Education;

    /**
     * 换岗岗位4-经验要求
     */
    private String transferJob4Experience;

    /**
     * 换岗岗位4-学习周期（月）
     */
    private Integer transferJob4LearningCycle;

    /**
     * 换岗岗位4-适配难度：1-低，2-中，3-高
     */
    private Integer transferJob4Difficulty;

    /**
     * 换岗岗位5-id（关联岗位要求画像表）
     */
    private Long transferJob5Id;

    /**
     * 换岗岗位5-描述
     */
    private String transferJob5Desc;

    /**
     * 换岗岗位5-技能差异
     */
    private String transferJob5SkillDiff;

    /**
     * 换岗岗位5-学历要求
     */
    private String transferJob5Education;

    /**
     * 换岗岗位5-经验要求
     */
    private String transferJob5Experience;

    /**
     * 换岗岗位5-学习周期（月）
     */
    private Integer transferJob5LearningCycle;

    /**
     * 换岗岗位5-适配难度：1-低，2-中，3-高
     */
    private Integer transferJob5Difficulty;

    /**
     * 换岗岗位6-id（关联岗位要求画像表）
     */
    private Long transferJob6Id;

    /**
     * 换岗岗位6-描述
     */
    private String transferJob6Desc;

    /**
     * 换岗岗位6-技能差异
     */
    private String transferJob6SkillDiff;

    /**
     * 换岗岗位6-学历要求
     */
    private String transferJob6Education;

    /**
     * 换岗岗位6-经验要求
     */
    private String transferJob6Experience;

    /**
     * 换岗岗位6-学习周期（月）
     */
    private Integer transferJob6LearningCycle;

    /**
     * 换岗岗位6-适配难度：1-低，2-中，3-高
     */
    private Integer transferJob6Difficulty;

    /**
     * 换岗岗位7-id（关联岗位要求画像表）
     */
    private Long transferJob7Id;

    /**
     * 换岗岗位7-描述
     */
    private String transferJob7Desc;

    /**
     * 换岗岗位7-技能差异
     */
    private String transferJob7SkillDiff;

    /**
     * 换岗岗位7-学历要求
     */
    private String transferJob7Education;

    /**
     * 换岗岗位7-经验要求
     */
    private String transferJob7Experience;

    /**
     * 换岗岗位7-学习周期（月）
     */
    private Integer transferJob7LearningCycle;

    /**
     * 换岗岗位7-适配难度：1-低，2-中，3-高
     */
    private Integer transferJob7Difficulty;

    /**
     * 换岗岗位8-id（关联岗位要求画像表）
     */
    private Long transferJob8Id;

    /**
     * 换岗岗位8-描述
     */
    private String transferJob8Desc;

    /**
     * 换岗岗位8-技能差异
     */
    private String transferJob8SkillDiff;

    /**
     * 换岗岗位8-学历要求
     */
    private String transferJob8Education;

    /**
     * 换岗岗位8-经验要求
     */
    private String transferJob8Experience;

    /**
     * 换岗岗位8-学习周期（月）
     */
    private Integer transferJob8LearningCycle;

    /**
     * 换岗岗位8-适配难度：1-低，2-中，3-高
     */
    private Integer transferJob8Difficulty;

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
