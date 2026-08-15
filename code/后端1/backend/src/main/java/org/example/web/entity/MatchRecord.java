package org.example.web.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class MatchRecord {
    /**
     * 匹配ID
     */
    private Long id;

    /**
     * 学生ID
     */
    private Long userId;

    /**
     * 岗位ID
     */
    private Long jobId;

    /**
     * 职级 1入门 2中级 3高级
     */
    private Integer level;

    /**
     * 硬门槛得分
     */
    private BigDecimal hardScore;

    /**
     * 专业技能得分
     */
    private BigDecimal skillScore;

    /**
     * 软实力得分
     */
    private BigDecimal softScore;

    /**
     * 1.学历背景
     */
    private BigDecimal educationScore;

    /**
     * 2.实习经历
     */
    private BigDecimal internshipScore;

    /**
     * 3.专业技能
     */
    private BigDecimal professionalScore;

    /**
     * 4.证书资质
     */
    private BigDecimal certificateScore;

    /**
     * 5.创新能力
     */
    private BigDecimal innovationScore;

    /**
     * 6.学习能力
     */
    private BigDecimal learningScore;

    /**
     * 7.抗压能力
     */
    private BigDecimal pressureScore;

    /**
     * 8.沟通能力
     */
    private BigDecimal communicationScore;

    /**
     * 9.问题解决能力
     */
    private BigDecimal problemSolvingScore;

    /**
     * 10.团队协作能力
     */
    private BigDecimal teamworkScore;

    /**
     * 综合总分
     */
    private BigDecimal totalScore;

    /**
     * 1强烈推荐 2推荐 3一般 4不推荐
     */
    private Integer matchResult;

    /**
     * 匹配状态 0未生成 1生成中 2已完成 3失败
     */
    private Integer matchStatus;

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
