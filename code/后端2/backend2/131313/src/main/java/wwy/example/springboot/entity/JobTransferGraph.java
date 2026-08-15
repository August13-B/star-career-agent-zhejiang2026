package wwy.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("job_transfer_graph")
public class JobTransferGraph {

    @TableId(type = IdType.INPUT)
    private Long id;

    @TableField("main_job_id")
    private Long mainJobId;

    @TableField("transfer_job1_id")
    private Long transferJob1Id;

    @TableField("transfer_job1_desc")
    private String transferJob1Desc;

    @TableField("transfer_job1_skill_diff")
    private String transferJob1SkillDiff;

    @TableField("transfer_job1_education")
    private String transferJob1Education;

    @TableField("transfer_job1_experience")
    private String transferJob1Experience;

    @TableField("transfer_job1_learning_cycle")
    private Integer transferJob1LearningCycle;

    @TableField("transfer_job1_difficulty")
    private Integer transferJob1Difficulty;

    @TableField("transfer_job2_id")
    private Long transferJob2Id;

    @TableField("transfer_job2_desc")
    private String transferJob2Desc;

    @TableField("transfer_job2_skill_diff")
    private String transferJob2SkillDiff;

    @TableField("transfer_job2_education")
    private String transferJob2Education;

    @TableField("transfer_job2_experience")
    private String transferJob2Experience;

    @TableField("transfer_job2_learning_cycle")
    private Integer transferJob2LearningCycle;

    @TableField("transfer_job2_difficulty")
    private Integer transferJob2Difficulty;

    @TableField("transfer_job3_id")
    private Long transferJob3Id;

    @TableField("transfer_job3_desc")
    private String transferJob3Desc;

    @TableField("transfer_job3_skill_diff")
    private String transferJob3SkillDiff;

    @TableField("transfer_job3_education")
    private String transferJob3Education;

    @TableField("transfer_job3_experience")
    private String transferJob3Experience;

    @TableField("transfer_job3_learning_cycle")
    private Integer transferJob3LearningCycle;

    @TableField("transfer_job3_difficulty")
    private Integer transferJob3Difficulty;

    @TableField("transfer_job4_id")
    private Long transferJob4Id;

    @TableField("transfer_job4_desc")
    private String transferJob4Desc;

    @TableField("transfer_job4_skill_diff")
    private String transferJob4SkillDiff;

    @TableField("transfer_job4_education")
    private String transferJob4Education;

    @TableField("transfer_job4_experience")
    private String transferJob4Experience;

    @TableField("transfer_job4_learning_cycle")
    private Integer transferJob4LearningCycle;

    @TableField("transfer_job4_difficulty")
    private Integer transferJob4Difficulty;

    @TableField("transfer_job5_id")
    private Long transferJob5Id;

    @TableField("transfer_job5_desc")
    private String transferJob5Desc;

    @TableField("transfer_job5_skill_diff")
    private String transferJob5SkillDiff;

    @TableField("transfer_job5_education")
    private String transferJob5Education;

    @TableField("transfer_job5_experience")
    private String transferJob5Experience;

    @TableField("transfer_job5_learning_cycle")
    private Integer transferJob5LearningCycle;

    @TableField("transfer_job5_difficulty")
    private Integer transferJob5Difficulty;

    @TableField("transfer_job6_id")
    private Long transferJob6Id;

    @TableField("transfer_job6_desc")
    private String transferJob6Desc;

    @TableField("transfer_job6_skill_diff")
    private String transferJob6SkillDiff;

    @TableField("transfer_job6_education")
    private String transferJob6Education;

    @TableField("transfer_job6_experience")
    private String transferJob6Experience;

    @TableField("transfer_job6_learning_cycle")
    private Integer transferJob6LearningCycle;

    @TableField("transfer_job6_difficulty")
    private Integer transferJob6Difficulty;

    @TableField("transfer_job7_id")
    private Long transferJob7Id;

    @TableField("transfer_job7_desc")
    private String transferJob7Desc;

    @TableField("transfer_job7_skill_diff")
    private String transferJob7SkillDiff;

    @TableField("transfer_job7_education")
    private String transferJob7Education;

    @TableField("transfer_job7_experience")
    private String transferJob7Experience;

    @TableField("transfer_job7_learning_cycle")
    private Integer transferJob7LearningCycle;

    @TableField("transfer_job7_difficulty")
    private Integer transferJob7Difficulty;

    @TableField("transfer_job8_id")
    private Long transferJob8Id;

    @TableField("transfer_job8_desc")
    private String transferJob8Desc;

    @TableField("transfer_job8_skill_diff")
    private String transferJob8SkillDiff;

    @TableField("transfer_job8_education")
    private String transferJob8Education;

    @TableField("transfer_job8_experience")
    private String transferJob8Experience;

    @TableField("transfer_job8_learning_cycle")
    private Integer transferJob8LearningCycle;

    @TableField("transfer_job8_difficulty")
    private Integer transferJob8Difficulty;

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