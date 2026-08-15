package org.example.web.service;

import org.example.web.entity.User;

import jakarta.servlet.http.HttpSession;

public interface UserService {
    /**
     * 注册用户（支持邀请码验证）
     * @param user 用户信息
     * @param invitationCode 邀请码（明文，非学生角色必须提供）
     */
    void register(User user, String invitationCode);

    /**
     * 根据账号查找用户
     * @param userAccount 用户账号
     * @return 用户信息
     */
    User findByUserAccount(String userAccount);

    /**
     * 根据ID查找用户
     * @param id 用户ID
     * @return 用户信息
     */
    User findById(Long id);

    /**
     * 根据ID范围删除用户
     * @param start 起始ID
     * @param end 结束ID
     */
    void deleteById(Integer start, Integer end);

    /**
     * 发送验证码邮件（暂不实现，留空）
     * @param email 邮箱
     * @param session HTTP会话
     */
    void sendmail(String email, HttpSession session);

    /**
     * 获取验证码发送时间
     * @param session HTTP会话
     * @return 时间戳
     */
    Long getcodetime(HttpSession session);

    /**
     * 临时数据操作（暂不实现，留空）
     */
    int addTemData(String user, Integer dtat1, String data2);
    void deleteByUser(String user);

    /**
     * 根据邮箱查找用户
     * @param email 邮箱
     * @return 用户信息
     */
    User findByEmail(String email);

    /**
     * 根据手机号查找用户
     * @param phone 手机号
     * @return 用户信息
     */
    User findByPhone(String phone);

    /**
     * 忘记密码发送邮件（针对邮箱）
     */
    void forget_password_sendmail(String email, HttpSession session);

    /**
     * 根据邮箱更新密码
     */
    void updatePasswordByEmail(String email, String newPassword);

    /**
     * 根据手机号更新密码
     */
    void updatePasswordByPhone(String phone, String newPassword);

    /**
     * 检查邮箱是否已存在
     */
    boolean isEmailExist(String email);

    /**
     * 检查手机号是否已存在
     */
    boolean isPhoneExist(String phone);

    /**
     * 检查用户账号是否已存在
     */
    boolean isUserAccountExist(String userAccount);

    /**
     * 根据用户ID更新密码
     * @param userId 用户ID
     * @param newPassword 新密码（SHA256加密后的）
     */
    void updatePasswordById(Long userId, String newPassword);
}
