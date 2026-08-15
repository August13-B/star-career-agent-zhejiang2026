package wwy.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("job_market_info")
public class JobMarketInfo {

    @TableId(type = IdType.INPUT)
    private Long id;

    @TableField("job_id")
    private Long jobId;

    private String industry;

    @TableField("entry_threshold")
    private String entryThreshold;

    @TableField("market_supply_demand")
    private String marketSupplyDemand;   // 1-供大于求，2-供需平衡，3-供不应求

    @TableField("supply_demand_ratio")
    private BigDecimal supplyDemandRatio;

    @TableField("salary_entry")
    private String salaryEntry;

    @TableField("salary_1year")
    private String salary1year;

    @TableField("salary_3year")
    private String salary3year;

    @TableField("salary_5year")
    private String salary5year;

    @TableField("salary_trend")
    private String salaryTrend;

    @TableField("major_adaptation")
    private String majorAdaptation;   // JSON格式

    @TableField("city_distribution")
    private String cityDistribution;   // JSON格式

    @TableField("competition_level")
    private Integer competitionLevel;   // 1-低，2-中，3-高，4-极高

    @TableField("growth_rate")
    private BigDecimal growthRate;

    @TableField("future_outlook")
    private String futureOutlook;

    @TableField("data_source")
    private String dataSource;

    @TableField("data_update_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataUpdateDate;

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