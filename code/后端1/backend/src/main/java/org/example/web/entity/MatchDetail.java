package org.example.web.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class MatchDetail {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 对应match_record.id
     */
    private Long matchId;

    /**
     * 维度代码dim1~dim10
     */
    private String dimCode;

    /**
     * 维度名称
     */
    private String dimName;

    /**
     * 1硬门槛 2专业技能 3软实力
     */
    private Integer dimType;

    /**
     * 学生情况
     */
    private String studentContent;

    /**
     * 岗位要求
     */
    private String jobRequire;

    /**
     * 得分
     */
    private BigDecimal score;

    /**
     * 差距分析
     */
    private String gapAnalysis;

    /**
     * 提升建议
     */
    private String improvementSuggestion;

    /**
     * 匹配结果说明
     */
    private String matchDesc;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
