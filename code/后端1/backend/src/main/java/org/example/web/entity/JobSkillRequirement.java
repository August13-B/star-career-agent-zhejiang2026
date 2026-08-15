package org.example.web.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class JobSkillRequirement {
    /**
     * 主键
     */
    private Long id;

    /**
     * 关联岗位主表ID
     */
    private Long jobId;

    /**
     * 3.专业技能要求（JSON格式存储标签化数据）
     */
    private String professionalSkill;

    /**
     * 4.证书资质要求（JSON格式存储标签化数据）
     */
    private String certificateRequirement;

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
