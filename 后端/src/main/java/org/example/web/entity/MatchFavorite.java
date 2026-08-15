package org.example.web.entity;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 匹配收藏表
 * 学生可以收藏高匹配度的岗位，方便后续查看和管理
 * 
 * @author 系统生成
 * @version 1.0
 */
@Data
public class MatchFavorite {
    /**
     * 主键ID（雪花算法生成）
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 匹配记录ID
     */
    private Long matchId;
    
    /**
     * 收藏时间
     */
    private LocalDateTime favoriteTime;
    
    /**
     * 是否置顶（0-否，1-是）
     */
    private Integer isPinned;
    
    /**
     * 置顶时间
     */
    private LocalDateTime pinnedTime;
    
    /**
     * 备注信息
     */
    private String note;
    
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
