package wwy.example.springboot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import wwy.example.springboot.entity.JobSkillRequirement;

public interface JobSkillRequirementService {

    /**
     * 新增技能需求
     * @param requirement 技能需求实体
     * @return 是否成功
     */
    boolean add(JobSkillRequirement requirement);

    /**
     * 根据ID逻辑删除
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

    /**
     * 根据岗位ID逻辑删除（当岗位被删除时调用）
     * @param jobId 岗位ID
     * @return 是否成功
     */
    boolean deleteByJobId(Long jobId);

    /**
     * 更新技能需求
     * @param requirement 实体（必须包含id）
     * @return 是否成功
     */
    boolean update(JobSkillRequirement requirement);

    /**
     * 根据ID查询
     * @param id 主键
     * @return 技能需求实体
     */
    JobSkillRequirement findById(Long id);

    /**
     * 根据岗位ID查询（一对一关系，返回唯一记录）
     * @param jobId 岗位ID
     * @return 技能需求实体，可能为null
     */
    JobSkillRequirement findByJobId(Long jobId);

    /**
     * 分页查询（可选按岗位ID过滤）
     * @param current 当前页
     * @param size 每页条数
     * @param jobId 岗位ID（可选）
     * @return 分页结果
     */
    IPage<JobSkillRequirement> pageQuery(long current, long size, Long jobId);
}