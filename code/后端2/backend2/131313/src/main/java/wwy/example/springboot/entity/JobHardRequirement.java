package wwy.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("job_hard_requirement")
public class JobHardRequirement {

    @TableId(type = IdType.INPUT)
    private Long id;

    @TableField("job_id")
    private Long jobId;

    @TableField("education_requirement")
    private String educationRequirement;

    @TableField("internship_requirement")
    private String internshipRequirement;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
}