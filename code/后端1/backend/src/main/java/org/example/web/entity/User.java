package org.example.web.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class User {
    /**
     * 用户唯一主键ID（非自增，用雪花ID）
     */
    private Long id;

    /**
     * 用户账号（学号/自定义，唯一）
     */
    private String userAccount;

    private String nickname;

    /**
     * 密码（加密存储，如SHA256+盐值）
     */
    private String userPassword;

    /**
     * 用户角色：1-学生（默认），2-管理员，3-企业端，4-导师
     */
    private Integer userRole;

    /**
     * 账号状态：1-正常，2-未激活，3-冻结，4-注销
     */
    private Integer userStatus;

    private Integer RegisterType;

    private String phone;

    private  String email;

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
     * 逻辑删除：0=未删除，1=已删除
     */
    private Integer isDeleted;
}
