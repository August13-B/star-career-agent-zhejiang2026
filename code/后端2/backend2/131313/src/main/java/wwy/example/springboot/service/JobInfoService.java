package wwy.example.springboot.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import wwy.example.springboot.entity.JobInfo;

import java.util.List;

public interface JobInfoService {

    /**
     * 新增岗位信息
     * @param jobInfo 岗位实体
     * @return 是否成功
     */
    boolean add(JobInfo jobInfo);

    /**
     * 根据ID逻辑删除
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

    /**
     * 更新岗位信息
     * @param jobInfo 岗位实体（必须包含id）
     * @return 是否成功
     */
    boolean update(JobInfo jobInfo);

    /**
     * 根据ID查询
     * @param id 主键
     * @return 岗位实体
     */
    JobInfo findById(Long id);

    /**
     * 查询所有（未删除的）
     * @return 岗位列表
     */
    List<JobInfo> findAll();

    /**
     * 根据岗位名称模糊查询
     * @param jobName 岗位名称（可为空）
     * @return 岗位列表
     */
    List<JobInfo> findByJobName(String jobName);

    /**
     * 分页查询
     * @param current 当前页
     * @param size 每页条数
     * @param jobName 岗位名称（可选模糊）
     * @return 分页结果
     */
    IPage<JobInfo> pageQuery(long current, long size, String jobName);

    JobInfo findByJobId(Long jobId);

    // JobInfoService.java
    Long getProfileIdByJobInfoId(Long jobInfoId);

    // JobInfoService.java
    List<JobInfo> findListByJobId(Long jobId);
}