package org.example.web.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class JobHardRequirement {
    /**
     * 主键
     */
    private Long id;

    /**
     * 关联岗位主表ID
     */
    private Long jobId;

    /**
     * 1.学历背景要求
     */
    private String educationRequirement;

    /**
     * 2.实习经历要求
     */
    private String internshipRequirement;

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
