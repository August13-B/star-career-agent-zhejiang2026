package org.example.web.service.impl;

import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpSession;
import org.example.web.entity.User;
import org.example.web.mapper.UserMapper;
import org.example.web.service.InvitationCodeService;
import org.example.web.service.UserService;
import org.example.web.tool.AccountGenerator;
import org.example.web.tool.RSA_256;
import org.example.web.tool.SHA_256;
import org.example.web.tool.SnowIdCreater;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RSA_256 rsa256;

    @Autowired
    private InvitationCodeService invitationCodeService;

    @Override
    public void register(User user, String invitationCode) {
        // 验证用户角色和邀请码
        Integer userRole = user.getUserRole();
        if (userRole == null) {
            userRole = 1; // 默认学生角色
            user.setUserRole(userRole);
        }
        
        // 学生角色（1）不需要邀请码，其他角色必须提供邀请码
        if (userRole != 1) {
            if (invitationCode == null || invitationCode.isEmpty()) {
                throw new RuntimeException("非学生角色必须提供邀请码");
            }
            
            // 验证邀请码
            boolean isValid = invitationCodeService.validateInvitationCode(userRole, invitationCode);
            if (!isValid) {
                throw new RuntimeException("邀请码无效或已过期");
            }
        }
        
        // 使用雪花ID生成器生成ID，类别为0（用户类别）
        Long id = SnowIdCreater.generateId(1);
        user.setId(id);
        
        // 如果userAccount为空，则使用账号生成工具生成
        if (user.getUserAccount() == null || user.getUserAccount().isEmpty()) {
            String userAccount = AccountGenerator.generateDefaultAccount();
            user.setUserAccount(userAccount);
        }
        
        // 使用SHA256加密密码
        if (user.getUserPassword() != null && !user.getUserPassword().isEmpty()) {
            String encryptedPassword = SHA_256.sha256(user.getUserPassword());
            user.setUserPassword(encryptedPassword);
        }
        
        // 对邮箱和手机号进行固定AES加密存储（如果存在）
        try {
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                String encryptedEmail = rsa256.encryptForDB(user.getEmail());
                user.setEmail(encryptedEmail);
            }
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                String encryptedPhone = rsa256.encryptForDB(user.getPhone());
                user.setPhone(encryptedPhone);
            }
        } catch (Exception e) {
            throw new RuntimeException("加密邮箱或手机号失败", e);
        }
        
        // 设置默认值
        if (user.getUserStatus() == null) {
            user.setUserStatus(1); // 默认正常状态
        }
        if (user.getRegisterType() == null) {
            user.setRegisterType(2); // 默认邮箱注册类型
        }
        
        // 插入数据库
        userMapper.insert(user);
        
        // 注册成功后，重新生成该角色的邀请码（非学生角色）
        if (userRole != 1) {
            try {
                String newCode = invitationCodeService.regenerateInvitationCode(userRole);
                System.out.println("角色 " + userRole + " 的邀请码已重新生成: " + newCode);
            } catch (Exception e) {
                // 重新生成邀请码失败不影响用户注册，但记录日志
                System.err.println("重新生成邀请码失败: " + e.getMessage());
            }
        }
    }

    @Override
    public User findByUserAccount(String userAccount) {
        return userMapper.findByUserAccount(userAccount);
    }

    @Override
    public User findById(Long id) {
        return userMapper.findById(id);
    }

    @Override
    public void deleteById(Integer start, Integer end) {
        userMapper.deleteById(start, end);
    }

    @Autowired
    JavaMailSender mailSender;
    @Async
    @Override
    public void sendmail(String email, HttpSession session) {
        System.out.println(email);
        Integer randomNumber = (int) (Math.random() * 900000) + 100000;
        session.setAttribute("email", email);
        session.setAttribute("emailcode", randomNumber);
        session.setAttribute("emailcodetime", System.currentTimeMillis());
        System.out.println("Session attributes set - email: " + email + ", emailcode: " + randomNumber + ", emailcodetime: " + System.currentTimeMillis());
        System.out.println("Session ID: " + session.getId());
        MimeMessagePreparator preparator = new MimeMessagePreparator() {
            public void prepare(MimeMessage mimeMessage) throws Exception {
                mimeMessage.setRecipient(Message.RecipientType.TO,
                        new InternetAddress(email));
                mimeMessage.setFrom(new InternetAddress("2385290553@qq.com"));
                mimeMessage.setSubject("【星职业】验证码" + randomNumber + "用于星职业网站登录邮箱绑定，3分钟内有效");
                mimeMessage.setText("【星职业】" + "验证码  " + randomNumber + "  用于星职业网站登录邮箱绑定，3分钟内有效，请勿泄露或转发。如非本人操作，请忽略此邮件，感谢您使用星职业网站");

            }
        };
        mailSender.send(preparator);
    }

    @Override
    public Long getcodetime(HttpSession session) {
        return (Long) session.getAttribute("emailcodetime");
    }

    @Override
    public int addTemData(String user, Integer data1, String data2) {
        return userMapper.addTemData(user, data1, data2);
    }

    @Override
    public void deleteByUser(String user) {
        userMapper.deleteByUser(user);
    }

    @Override
    public User findByEmail(String email) {
        try {
            String encryptedEmail = rsa256.encryptForDB(email);
            User user = userMapper.findByEmail(encryptedEmail);
            if (user != null) {
                // 解密邮箱和手机号（邮箱就是传入的明文，手机号需要解密）
                user.setEmail(email); // 直接使用明文
                if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                    String decryptedPhone = rsa256.decryptFromDB(user.getPhone());
                    user.setPhone(decryptedPhone);
                }
            }
            return user;
        } catch (Exception e) {
            throw new RuntimeException("邮箱查询失败", e);
        }
    }

    @Override
    public User findByPhone(String phone) {
        try {
            String encryptedPhone = rsa256.encryptForDB(phone);
            User user = userMapper.findByPhone(encryptedPhone);
            if (user != null) {
                // 解密邮箱和手机号（手机号就是传入的明文，邮箱需要解密）
                user.setPhone(phone); // 直接使用明文
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    String decryptedEmail = rsa256.decryptFromDB(user.getEmail());
                    user.setEmail(decryptedEmail);
                }
            }
            return user;
        } catch (Exception e) {
            throw new RuntimeException("手机号查询失败", e);
        }
    }
    @Async
    @Override
    public void forget_password_sendmail(String email, HttpSession session) {
        System.out.println(email);
        Integer randomNumber = (int) (Math.random() * 900000) + 100000;
        session.setAttribute("email", email);
        session.setAttribute("emailcode", randomNumber);
        session.setAttribute("emailcodetime", System.currentTimeMillis());
        session.setAttribute("forget_email", email);
        System.out.println("Session attributes set - email: " + email + ", emailcode: " + randomNumber + ", emailcodetime: " + System.currentTimeMillis());
        System.out.println("Session ID: " + session.getId());
        MimeMessagePreparator preparator = new MimeMessagePreparator() {
            public void prepare(MimeMessage mimeMessage) throws Exception {
                mimeMessage.setRecipient(Message.RecipientType.TO,
                        new InternetAddress(email));
                mimeMessage.setFrom(new InternetAddress("2385290553@qq.com"));
                mimeMessage.setSubject("【星职业】验证码" + randomNumber + "用于星职业网站登录密码重置，3分钟内有效");
                mimeMessage.setText("【星职业】" + "验证码  " + randomNumber + "  用于星职业网站登录密码重置，3分钟内有效，请勿泄露或转发。如非本人操作，请忽略此邮件，感谢您使用星职业网站");

            }
        };
        mailSender.send(preparator);
    }

    @Override
    public void updatePasswordByEmail(String email, String newPassword) {
        try {
            String encryptedEmail = rsa256.encryptForDB(email);
            userMapper.updatePasswordByEmail(encryptedEmail, newPassword);
        } catch (Exception e) {
            throw new RuntimeException("更新密码失败", e);
        }
    }

    @Override
    public void updatePasswordByPhone(String phone, String newPassword) {
        try {
            String encryptedPhone = rsa256.encryptForDB(phone);
            userMapper.updatePasswordByPhone(encryptedPhone, newPassword);
        } catch (Exception e) {
            throw new RuntimeException("更新密码失败", e);
        }
    }

    @Override
    public boolean isEmailExist(String email) {
        try {
            String encryptedEmail = rsa256.encryptForDB(email);
            int count = userMapper.countByEmail(encryptedEmail);
            return count > 0;
        } catch (Exception e) {
            throw new RuntimeException("检查邮箱是否存在失败", e);
        }
    }

    @Override
    public boolean isPhoneExist(String phone) {
        try {
            String encryptedPhone = rsa256.encryptForDB(phone);
            int count = userMapper.countByPhone(encryptedPhone);
            return count > 0;
        } catch (Exception e) {
            throw new RuntimeException("检查手机号是否存在失败", e);
        }
    }

    @Override
    public boolean isUserAccountExist(String userAccount) {
        int count = userMapper.countByUserAccount(userAccount);
        return count > 0;
    }

    @Override
    public void updatePasswordById(Long userId, String newPassword) {
        int affectedRows = userMapper.updatePasswordById(userId, newPassword);
        if (affectedRows == 0) {
            throw new RuntimeException("更新密码失败，用户不存在");
        }
    }
}
