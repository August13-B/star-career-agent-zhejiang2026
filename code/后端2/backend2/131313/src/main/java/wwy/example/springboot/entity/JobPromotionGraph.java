package wwy.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("job_promotion_graph")
public class JobPromotionGraph {

    @TableId(type = IdType.INPUT)
    private Long id;

    @TableField("main_job_id")
    private Long mainJobId;

    @TableField("promotion_job1_id")
    private Long promotionJob1Id;

    @TableField("promotion_job1_desc")
    private String promotionJob1Desc;

    @TableField("promotion_job1_skill_diff")
    private String promotionJob1SkillDiff;

    @TableField("promotion_job1_experience")
    private String promotionJob1Experience;

    @TableField("promotion_job1_learning_cycle")
    private Integer promotionJob1LearningCycle;

    @TableField("promotion_job2_id")
    private Long promotionJob2Id;

    @TableField("promotion_job2_desc")
    private String promotionJob2Desc;

    @TableField("promotion_job2_skill_diff")
    private String promotionJob2SkillDiff;

    @TableField("promotion_job2_experience")
    private String promotionJob2Experience;

    @TableField("promotion_job2_learning_cycle")
    private Integer promotionJob2LearningCycle;

    @TableField("promotion_job3_id")
    private Long promotionJob3Id;

    @TableField("promotion_job3_desc")
    private String promotionJob3Desc;

    @TableField("promotion_job3_skill_diff")
    private String promotionJob3SkillDiff;

    @TableField("promotion_job3_experience")
    private String promotionJob3Experience;

    @TableField("promotion_job3_learning_cycle")
    private Integer promotionJob3LearningCycle;

    @TableField("promotion_job4_id")
    private Long promotionJob4Id;

    @TableField("promotion_job4_desc")
    private String promotionJob4Desc;

    @TableField("promotion_job4_skill_diff")
    private String promotionJob4SkillDiff;

    @TableField("promotion_job4_experience")
    private String promotionJob4Experience;

    @TableField("promotion_job4_learning_cycle")
    private Integer promotionJob4LearningCycle;

    @TableField("promotion_job5_id")
    private Long promotionJob5Id;

    @TableField("promotion_job5_desc")
    private String promotionJob5Desc;

    @TableField("promotion_job5_skill_diff")
    private String promotionJob5SkillDiff;

    @TableField("promotion_job5_experience")
    private String promotionJob5Experience;

    @TableField("promotion_job5_learning_cycle")
    private Integer promotionJob5LearningCycle;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}