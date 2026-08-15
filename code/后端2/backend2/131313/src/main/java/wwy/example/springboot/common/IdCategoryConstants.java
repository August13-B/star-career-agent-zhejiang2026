package wwy.example.springboot.common;
/**
 * 雪花ID业务类别码常量
 * 值范围：0~31
 */
public class IdCategoryConstants {

    private IdCategoryConstants() {}  // 私有构造，防止实例化

    public static final int JOB_INFO = 13;                 // 岗位信息表
    public static final int JOB_PROMOTION_GRAPH = 14;      // 晋升图谱表
    public static final int JOB_TRANSFER_GRAPH = 15;       // 换岗图谱表
    public static final int JOB_REQUIREMENT_PROFILE = 8;  // 岗位需求主表
    public static final int JOB_HARD_REQUIREMENT = 9;     // 硬门槛需求表
    public static final int JOB_SKILL_REQUIREMENT = 10;    // 专业技能需求表
    public static final int JOB_SOFT_REQUIREMENT = 11;     // 软实力需求表
    public static final int JOB_MARKET_INFO = 12;   // 岗位市场信息表
}