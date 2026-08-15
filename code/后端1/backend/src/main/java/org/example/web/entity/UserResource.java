package org.example.web.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class UserResource {
    /**
     * 资源ID
     */
    private Long id;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 资源类型：1-证书，2-奖状，3-成绩单，4-作品集，5-其他
     */
    private Integer resourceType;

    /**
     * 资源名称
     */
    private String resourceName;

    /**
     * 资源描述
     */
    private String resourceDesc;

    /**
     * 文件存储路径（核心字段）
     */
    private String filePath;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型（MIME类型）
     */
    private String fileType;

    /**
     * 文件扩展名
     */
    private String fileExtension;

    /**
     * 证书/奖状颁发日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime issueDate;

    /**
     * 证书过期日期（如有）
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime expireDate;

    /**
     * 颁发机构
     */
    private String issuer;

    /**
     * 是否已验证：0-未验证，1-已验证
     */
    private Integer isVerified;

    /**
     * 验证时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime verifyTime;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

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
