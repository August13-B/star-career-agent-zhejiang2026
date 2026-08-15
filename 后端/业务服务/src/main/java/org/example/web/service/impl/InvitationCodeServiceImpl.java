package org.example.web.service.impl;

import java.security.SecureRandom;
import java.util.Base64;

import org.example.web.entity.InvitationCode;
import org.example.web.mapper.InvitationCodeMapper;
import org.example.web.service.InvitationCodeService;
import org.example.web.tool.SnowIdCreater;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InvitationCodeServiceImpl implements InvitationCodeService {

    @Autowired
    private InvitationCodeMapper invitationCodeMapper;

    private static final SecureRandom random = new SecureRandom();
    private static final int CODE_LENGTH = 12;

    @Override
    public InvitationCode findByUserRole(Integer userRole) {
        return invitationCodeMapper.findByUserRole(userRole);
    }

    @Override
    public InvitationCode findByCode(String invitationCode) {
        return invitationCodeMapper.findByCode(invitationCode);
    }

    @Override
    public boolean validateInvitationCode(Integer userRole, String invitationCode) {
        if (userRole == null || invitationCode == null || invitationCode.isEmpty()) {
            return false;
        }
        
        // 如果是学生角色（1），不需要邀请码
        if (userRole == 1) {
            return true;
        }
        
        // 查找该角色对应的邀请码
        InvitationCode codeRecord = invitationCodeMapper.findByUserRole(userRole);
        if (codeRecord == null) {
            return false;
        }
        
        // 验证邀请码是否匹配
        return codeRecord.getInvitationCode().equals(invitationCode);
    }

    @Override
    public String regenerateInvitationCode(Integer userRole) {
        // 生成新的随机邀请码
        String newCode = generateRandomCode();
        
        // 更新数据库中的邀请码
        int updated = invitationCodeMapper.updateCodeByRole(userRole, newCode);
        
        if (updated > 0) {
            return newCode;
        } else {
            // 如果更新失败，尝试插入新记录
            InvitationCode codeRecord = new InvitationCode();
            codeRecord.setId(SnowIdCreater.generateId(27)); // 使用类别27表示邀请码
            codeRecord.setUserRole(userRole);
            codeRecord.setInvitationCode(newCode);
            
            try {
                invitationCodeMapper.insert(codeRecord);
                return newCode;
            } catch (Exception e) {
                throw new RuntimeException("重新生成邀请码失败", e);
            }
        }
    }

    @Override
    public void initInvitationCodes() {
        // 初始化三个角色的邀请码
        Integer[] roles = {2, 3, 4}; // 管理员、企业端、导师
        
        for (Integer role : roles) {
            InvitationCode existing = invitationCodeMapper.findByUserRole(role);
            if (existing == null) {
                InvitationCode codeRecord = new InvitationCode();
                codeRecord.setId(SnowIdCreater.generateId(27));
                codeRecord.setUserRole(role);
                codeRecord.setInvitationCode(generateRandomCode());
                
                invitationCodeMapper.insert(codeRecord);
                System.out.println("已初始化角色 " + role + " 的邀请码: " + codeRecord.getInvitationCode());
            }
        }
    }

    /**
     * 生成随机邀请码
     */
    private String generateRandomCode() {
        byte[] bytes = new byte[CODE_LENGTH];
        random.nextBytes(bytes);
        String base64 = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        
        // 取前12个字符作为邀请码
        return base64.substring(0, Math.min(base64.length(), CODE_LENGTH)).toUpperCase();
    }
}
