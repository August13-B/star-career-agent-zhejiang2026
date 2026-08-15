package org.example.web.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 邀请码实体类
 * 存储管理员、企业端、导师的邀请码
 */
@Data
public class InvitationCode {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户角色：2-管理员，3-企业端，4-导师
     */
    private Integer userRole;

    /**
     * 当前邀请码（随机生成）
     */
    private String invitationCode;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0=未删除，1=已删除
     */
    private Integer isDeleted;

    // 手动添加getter和setter方法以解决Lombok编译问题
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUserRole() {
        return userRole;
    }

    public void setUserRole(Integer userRole) {
        this.userRole = userRole;
    }

    public String getInvitationCode() {
        return invitationCode;
    }

    public void setInvitationCode(String invitationCode) {
        this.invitationCode = invitationCode;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }
}
