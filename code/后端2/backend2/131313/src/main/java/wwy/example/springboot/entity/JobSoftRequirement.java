package wwy.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("job_soft_requirement")
public class JobSoftRequirement {

    @TableId(type = IdType.INPUT)
    private Long id;

    @TableField("job_id")
    private Long jobId;

    @TableField("innovation_ability")
    private String innovationAbility;

    @TableField("learning_ability")
    private String learningAbility;

    @TableField("pressure_resistance")
    private String pressureResistance;

    @TableField("communication_ability")
    private String communicationAbility;

    @TableField("problem_solving")
    private String problemSolving;

    @TableField("teamwork_ability")
    private String teamworkAbility;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}