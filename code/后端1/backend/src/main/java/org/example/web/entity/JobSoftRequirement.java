package org.example.web.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class JobSoftRequirement {
    /**
     * 主键
     */
    private Long id;

    /**
     * 关联岗位主表ID
     */
    private Long jobId;

    /**
     * 5.创新能力要求
     */
    private String innovationAbility;

    /**
     * 6.学习能力要求
     */
    private String learningAbility;

    /**
     * 7.抗压能力要求
     */
    private String pressureResistance;

    /**
     * 8.沟通能力要求
     */
    private String communicationAbility;

    /**
     * 9.问题解决能力要求
     */
    private String problemSolving;

    /**
     * 10.团队协作能力要求
     */
    private String teamworkAbility;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    private Integer isDeleted;
}
