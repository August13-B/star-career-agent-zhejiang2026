package org.example.web.entity;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 报告分享链接表
 * 用于生成和管理职业报告的分享链接，支持权限控制和有效期管理
 * 
 * @author 系统生成
 * @version 1.0
 */
@Data
public class ReportShareLink {
    /**
     * 主键ID（雪花算法生成）
     */
    private Long id;
    
    /**
     * 报告ID
     */
    private Long reportId;
    
    /**
     * 分享令牌（唯一标识）
     */
    private String shareToken;
    
    /**
     * 分享链接（完整URL）
     */
    private String shareUrl;
    
    /**
     * 权限等级：1-仅自己可见，2-对指导老师可见，3-对授权企业可见
     */
    private Integer permissionLevel;
    
    /**
     * 创建者用户ID
     */
    private Long creatorId;
    
    /**
     * 访问密码（可选）
     */
    private String accessPassword;
    
    /**
     * 有效期开始时间
     */
    private LocalDateTime validFrom;
    
    /**
     * 有效期结束时间（为空表示永不过期）
     */
    private LocalDateTime validTo;
    
    /**
     * 最大访问次数（0表示无限制）
     */
    private Integer maxAccessCount;
    
    /**
     * 已访问次数
     */
    private Integer accessedCount;
    
    /**
     * 是否启用（0-禁用，1-启用）
     */
    private Integer isEnabled;
    
    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessedTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 逻辑删除标识（0-未删除，1-已删除）
     */
    private Integer isDeleted;
}
