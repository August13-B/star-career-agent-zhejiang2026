package org.example.web.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class StudentAbility {
    /**
     * 主键ID（雪花算法生成，非自增）
     */
    private Long id;

    /**
     * 关联用户ID（外键，一对一绑定用户）
     */
    private Long userId;

    /**
     * 关联学生画像ID
     */
    private Long profileId;

    /**
     * 1.学历背景
     */
    private String educationRequirement;

    /**
     * 2.实习经历
     */
    private String internshipAbility;

    /**
     * 3.专业技能（JSON格式存储标签化数据）
     */
    private String professionalSkill;

    /**
     * 4.证书资质（JSON格式存储标签化数据）
     */
    private String certificateRequirement;

    /**
     * 5.创新能力
     */
    private String innovationAbility;

    /**
     * 6.学习能力
     */
    private String learningAbility;

    /**
     * 7.抗压能力
     */
    private String pressureResistance;

    /**
     * 8.沟通能力
     */
    private String communicationAbility;

    /**
     * 9.问题解决能力
     */
    private String problemSolving;

    /**
     * 10.团队协作能力
     */
    private String teamworkAbility;

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
