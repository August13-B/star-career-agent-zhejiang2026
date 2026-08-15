package org.example.web.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class CareerReportHistory {
    /**
     * 历史记录ID
     */
    private Long id;

    /**
     * 关联报告ID
     */
    private Long reportId;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 报告内容快照
     */
    private String reportContent;

    /**
     * 变更原因
     */
    private String changeReason;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
