package org.example.web.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class StudentAbilityScoreHistory {
    /**
     * 历史分数ID（雪花算法）
     */
    private Long id;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 关联学生能力维度ID
     */
    private Long abilityId;

    /**
     * 关联原分数表ID
     */
    private Long scoreId;

    /**
     * 关联对应的画像历史版本ID
     */
    private Long profileHistoryId;

    /**
     * 分数版本号（与画像版本号保持一致）
     */
    private Integer version;

    /**
     * 1.学历背景评分
     */
    private Integer educationScore;

    /**
     * 2.实习经历评分
     */
    private Integer internshipScore;

    /**
     * 3.专业技能评分
     */
    private Integer professionalScore;

    /**
     * 4.证书资质评分
     */
    private Integer certificateScore;

    /**
     * 5.创新能力评分
     */
    private Integer innovationScore;

    /**
     * 6.学习能力评分
     */
    private Integer learningScore;

    /**
     * 7.抗压能力评分
     */
    private Integer pressureScore;

    /**
     * 8.沟通能力评分
     */
    private Integer communicationScore;

    /**
     * 9.问题解决评分
     */
    private Integer problemSolvingScore;

    /**
     * 10.团队协作评分
     */
    private Integer teamworkScore;

    /**
     * 能力总评分
     */
    private BigDecimal totalScore;

    /**
     * 行业排名百分比
     */
    private Integer industryRank;

    /**
     * 同届学生排名百分比
     */
    private Integer peerRank;

    /**
     * 评分类型：1-系统自动评分 2-导师评分 3-企业评分
     */
    private Integer scoreType;

    /**
     * 评分评语
     */
    private String scoreComment;

    /**
     * 分数变更原因
     */
    private String changeReason;

    /**
     * 分数记录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
