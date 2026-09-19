package org.example.web.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 成长任务完成情况记录（时间线，可多条）
 */
@Data
public class GrowTaskRecord {

    /** 记录ID（雪花） */
    private Long id;

    /** 关联成长任务ID */
    private Long taskId;

    /** 用户ID */
    private Long userId;

    /** 完成情况记录内容 */
    private String content;

    /** 记录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTime;

    /** 逻辑删除：0-未删除，1-已删除 */
    private Integer isDeleted;
}
