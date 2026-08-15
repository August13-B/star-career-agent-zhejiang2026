package wwy.example.springboot.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class JobTenDimensionScoreDTO {
    private Long jobId;
    private String jobName;

    // 10个维度分数（0-100）
    private BigDecimal educationScore;        // 学历
    private BigDecimal internshipScore;       // 实习经历
    private BigDecimal professionalSkillScore;// 专业技能
    private BigDecimal certificateScore;      // 证书要求
    private BigDecimal innovationScore;       // 创新能力
    private BigDecimal learningScore;         // 学习能力
    private BigDecimal pressureScore;         // 抗压能力
    private BigDecimal communicationScore;    // 沟通能力
    private BigDecimal problemSolvingScore;   // 问题解决
    private BigDecimal teamworkScore;         // 团队协作

    // 可选：综合总分（10个维度平均）
    private BigDecimal totalScore;
}