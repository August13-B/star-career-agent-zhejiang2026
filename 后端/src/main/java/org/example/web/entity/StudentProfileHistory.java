package org.example.web.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class StudentProfileHistory {
    /**
     * 历史记录ID
     */
    private Long id;

    /**
     * 关联学生画像ID
     */
    private Long profileId;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 画像数据快照（JSON格式）
     */
    private String profileData;

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
