package wwy.example.springboot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import wwy.example.springboot.entity.JobRequirementProfile;

import java.util.List;

public interface JobRequirementProfileService {

    /**
     * 新增岗位需求
     */
    boolean add(JobRequirementProfile profile);

    /**
     * 根据ID逻辑删除
     */
    boolean deleteById(Long id);

    /**
     * 更新岗位需求
     */
    boolean update(JobRequirementProfile profile);

    /**
     * 根据ID查询
     */
    JobRequirementProfile findById(Long id);

    /**
     * 查询所有（未删除的）
     */
    List<JobRequirementProfile> findAll();

    /**
     * 根据岗位名称模糊查询
     */
    List<JobRequirementProfile> findByPositionName(String positionName);

    /**
     * 根据分类查询
     */
    List<JobRequirementProfile> findByCategory(String category);

    /**
     * 根据职级查询
     */
    List<JobRequirementProfile> findByLevel(Integer level);

    /**
     * 分页查询（支持多条件组合）
     * @param current 当前页
     * @param size 每页条数
     * @param positionName 岗位名称（可选模糊）
     * @param category 分类（可选）
     * @param level 职级（可选）
     */
    IPage<JobRequirementProfile> pageQuery(long current, long size,
                                           String positionName,
                                           String category,
                                           Integer level);

    JobRequirementProfile findByJobId(Long jobId);

    // JobRequirementProfileService.java
    JobRequirementProfile findByPositionNameExact(String positionName);


}