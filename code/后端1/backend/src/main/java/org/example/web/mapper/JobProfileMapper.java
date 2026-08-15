package org.example.web.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.web.entity.JobInfo;
import org.example.web.entity.JobRequirementProfile;

/**
 * 岗位画像Mapper接口
 * 提供对job_info和job_requirement_profile表的数据库操作
 * 注意：使用MyBatis注解方式编写SQL语句
 * 
 * @author 系统生成
 * @version 1.0
 */
@Mapper
public interface JobProfileMapper {

    // ====================== job_info表操作 ======================
    
    /**
     * 添加岗位信息
     * 向job_info表插入一条新记录
     * 
     * @param jobInfo 岗位信息实体
     * @return 插入成功返回1，失败返回0
     */
    @Insert("INSERT INTO job_info (id, job_id, job_name, address, salary_range, company_name, industry, " +
            "company_scale, company_type, job_code, job_detail, update_date, company_detail, job_source_url, " +
            "create_time, update_time, is_deleted) " +
            "VALUES (#{id}, #{jobId}, #{jobName}, #{address}, #{salaryRange}, #{companyName}, #{industry}, " +
            "#{companyScale}, #{companyType}, #{jobCode}, #{jobDetail}, #{updateDate}, #{companyDetail}, " +
            "#{jobSourceUrl}, #{createTime}, #{updateTime}, #{isDeleted})")
    int insertJobInfo(JobInfo jobInfo);

    /**
     * 根据ID修改岗位的job_id
     * 更新job_info表中指定记录的job_id字段
     * 
     * @param id 岗位信息ID
     * @param jobId 新的job_id值
     * @return 更新成功返回1，失败返回0
     */
    @Update("UPDATE job_info SET job_id = #{jobId}, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int updateJobIdById(@Param("id") Long id, @Param("jobId") Long jobId);

    // ====================== job_requirement_profile表操作 ======================
    
    /**
     * 添加岗位要求
     * 向job_requirement_profile表插入一条新记录
     * 
     * @param requirement 岗位要求实体
     * @return 插入成功返回1，失败返回0
     */
    @Insert("INSERT INTO job_requirement_profile (id, position_name, category, industry, description, level, " +
            "hard_weight, skill_weight, soft_weight, create_time, update_time, is_deleted) " +
            "VALUES (#{id}, #{positionName}, #{category}, #{industry}, #{description}, #{level}, " +
            "#{hardWeight}, #{skillWeight}, #{softWeight}, #{createTime}, #{updateTime}, #{isDeleted})")
    int insertRequirement(JobRequirementProfile requirement);

    /**
     * 删除岗位要求（逻辑删除）
     * 将job_requirement_profile表的is_deleted字段置为1
     * 
     * @param id 岗位要求ID
     * @return 删除成功返回1，失败返回0
     */
    @Update("UPDATE job_requirement_profile SET is_deleted = 1, update_time = NOW() WHERE id = #{id} AND is_deleted = 0")
    int deleteRequirementById(Long id);

    /**
     * 修改岗位要求
     * 更新job_requirement_profile表的多个字段
     * 
     * @param requirement 岗位要求实体
     * @return 更新成功返回1，失败返回0
     */
    @Update("UPDATE job_requirement_profile SET " +
            "position_name = #{positionName}, category = #{category}, industry = #{industry}, " +
            "description = #{description}, level = #{level}, hard_weight = #{hardWeight}, " +
            "skill_weight = #{skillWeight}, soft_weight = #{softWeight}, update_time = NOW() " +
            "WHERE id = #{id} AND is_deleted = 0")
    int updateRequirement(JobRequirementProfile requirement);

    /**
     * 根据ID查询岗位要求
     * 查询job_requirement_profile表中指定ID的记录
     * 
     * @param id 岗位要求ID
     * @return 岗位要求实体，未找到返回null
     */
    @Select("SELECT * FROM job_requirement_profile WHERE id = #{id} AND is_deleted = 0")
    JobRequirementProfile selectRequirementById(Long id);

    /*
    * 查询返回所有岗位基础信息
    *用List，Map嵌套形式返回
    */
    @Select("SELECT * FROM job_info WHERE is_deleted = 0")
    List<Map<String,Object>> selectJobInfo();

    /*
     * 查询返回所有岗位画像信息
     *用List，Map嵌套形式返回
     */
    @Select("SELECT * FROM job_requirement_profile WHERE is_deleted = 0")
    List<Map<String,Object>> selectJobProfile();

    /**
     * 查询所有岗位要求
     * 查询job_requirement_profile表中所有未删除的记录
     * 
     * @return 岗位要求列表
     */
    @Select("SELECT * FROM job_requirement_profile WHERE is_deleted = 0 ORDER BY create_time DESC")
    List<JobRequirementProfile> selectAllRequirements();

    /**
     * 根据岗位名称精确查询岗位要求
     * 
     * @param positionName 岗位名称（精确匹配）
     * @return 岗位要求实体，未找到返回null
     */
    @Select("SELECT * FROM job_requirement_profile WHERE position_name = #{positionName} AND is_deleted = 0 LIMIT 1")
    JobRequirementProfile selectRequirementByPositionName(String positionName);

    /**
     * 根据岗位名称模糊查询岗位要求
     * 
     * @param positionName 岗位名称关键词
     * @return 岗位要求列表
     */
    @Select("SELECT * FROM job_requirement_profile WHERE position_name LIKE CONCAT('%', #{positionName}, '%') AND is_deleted = 0")
    List<JobRequirementProfile> selectRequirementsByPositionName(String positionName);

    /**
     * 根据行业查询岗位要求
     * 
     * @param industry 行业名称
     * @return 岗位要求列表
     */
    @Select("SELECT * FROM job_requirement_profile WHERE industry = #{industry} AND is_deleted = 0")
    List<JobRequirementProfile> selectRequirementsByIndustry(String industry);

    /**
     * 统计岗位要求数量
     * 
     * @return 未删除的岗位要求总数
     */
    @Select("SELECT COUNT(*) FROM job_requirement_profile WHERE is_deleted = 0")
    int countRequirements();
}
