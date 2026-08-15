package org.example.web.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class StudentProfile {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 当前版本号
     */
    private Integer version;

    /**
     * 真实姓名
     */
    private String userName;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 性别：1-男，2-女，0-未填写
     */
    private Integer gender;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 所属院校
     */
    private String college;

    /**
     * 所学专业
     */
    private String major;

    /**
     * 年级
     */
    private String grade;

    /**
     * 画像状态
     */
    private Integer profileStatus;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 毕业日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime graduationDate;

    /**
     * 职业意向（岗位名称，逗号分隔）
     */
    private String careerIntentions;

    /**
     * 求职意向详细描述
     */
    private String jobIntentionDetail;

    /**
     * 目标城市（逗号分隔）
     */
    private String targetCity;

    /**
     * 期望薪资范围
     */
    private String expectedSalary;

    /**
     * 行业偏好（逗号分隔）
     */
    private String industryPreference;

    /**
     * 工作性质偏好：1-国企，2-私企，3-考公，4-外企
     */
    private Integer workTypePreference;

    /**
     * 可接受的最长学习周期（月）
     */
    private Integer maxLearningCycle;

    /**
     * 学历
     */
    private String education;

    /**
     * 工作/实习经历（文本描述）
     */
    private String workExperience;

    /**
     * 项目经历（文本描述）
     */
    private String projectExperience;

    /**
     * 技能特长（逗号分隔）
     */
    private String skill;

    /**
     * 持有证书（逗号分隔）
     */
    private String certificate;

    /**
     * 学生群体：1-低年级，2-应届毕业生，3-考研/考公失利，4-跨专业求职
     */
    private Integer studentGroup;

    /**
     * 隐私等级：1-仅自己可见，2-对指导老师可见，3-对授权企业可见
     */
    private Integer privacyLevel;

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
