package wwy.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("job_info")
public class JobInfo {

    @TableId(type = IdType.INPUT)
    private Long id;

    @TableField("job_id")
    private Long jobId;

    @TableField("job_name")
    private String jobName;

    private String address;

    @TableField("salary_range")
    private String salaryRange;

    @TableField("company_name")
    private String companyName;

    private String industry;

    @TableField("company_scale")
    private String companyScale;

    @TableField("company_type")
    private String companyType;

    @TableField("job_code")
    private String jobCode;

    @TableField("job_detail")
    private String jobDetail;

    @TableField("update_date")
    private String updateDate;

    @TableField("company_detail")
    private String companyDetail;

    @TableField("job_source_url")
    private String jobSourceUrl;

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