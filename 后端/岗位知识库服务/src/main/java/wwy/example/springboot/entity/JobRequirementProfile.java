package wwy.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("job_requirement_profile")
public class JobRequirementProfile {

    @TableId(type = IdType.INPUT)
    private Long id;

    @TableField("position_name")
    private String positionName;

    private String category;

    private String industry;

    private String description;

    private Integer level;  // 1-入门 2-中级 3-高级

    @TableField("hard_weight")
    private BigDecimal hardWeight;

    @TableField("skill_weight")
    private BigDecimal skillWeight;

    @TableField("soft_weight")
    private BigDecimal softWeight;

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