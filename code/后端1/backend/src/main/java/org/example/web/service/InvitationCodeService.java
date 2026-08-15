package org.example.web.service;

import org.example.web.entity.InvitationCode;

/**
 * 邀请码服务接口
 */
public interface InvitationCodeService {
    /**
     * 根据用户角色查找邀请码
     */
    InvitationCode findByUserRole(Integer userRole);

    /**
     * 根据邀请码查找
     */
    InvitationCode findByCode(String invitationCode);

    /**
     * 验证邀请码是否有效
     * @param userRole 用户角色
     * @param invitationCode 邀请码
     * @return true=有效，false=无效
     */
    boolean validateInvitationCode(Integer userRole, String invitationCode);

    /**
     * 重新生成指定角色的邀请码
     * @param userRole 用户角色（2-管理员，3-企业端，4-导师）
     * @return 新的邀请码
     */
    String regenerateInvitationCode(Integer userRole);

    /**
     * 初始化邀请码表（如果不存在则创建）
     */
    void initInvitationCodes();
}
