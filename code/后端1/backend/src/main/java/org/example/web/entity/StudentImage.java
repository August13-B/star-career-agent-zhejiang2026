package org.example.web.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class StudentImage {
    /**
     * 主键ID（雪花算法生成）
     */
    private Long id;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 图片文件名
     */
    private String fileName;

    /**
     * 图片存储路径
     */
    private String filePath;

    /**
     * 图片类型（如：avatar, certificate等）
     */
    private String imageType;

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