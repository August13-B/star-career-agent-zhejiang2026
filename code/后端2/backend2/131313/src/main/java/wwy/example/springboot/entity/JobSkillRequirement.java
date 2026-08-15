package wwy.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("job_skill_requirement")
public class JobSkillRequirement {

    @TableId(type = IdType.INPUT)
    private Long id;

    @TableField("job_id")
    private Long jobId;

    @TableField("professional_skill")
    private String professionalSkill;   // JSON格式

    @TableField("certificate_requirement")
    private String certificateRequirement; // JSON格式

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}