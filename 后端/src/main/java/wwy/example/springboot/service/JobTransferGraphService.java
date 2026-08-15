package wwy.example.springboot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import wwy.example.springboot.entity.JobTransferGraph;

import java.util.List;

public interface JobTransferGraphService {

    /**
     * 新增换岗路径
     * @param graph 图谱实体
     * @return 是否成功
     */
    boolean add(JobTransferGraph graph);

    /**
     * 根据ID逻辑删除
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

    /**
     * 更新换岗路径
     * @param graph 图谱实体（必须包含id）
     * @return 是否成功
     */
    boolean update(JobTransferGraph graph);

    /**
     * 根据ID查询
     * @param id 主键
     * @return 图谱实体
     */
    JobTransferGraph findById(Long id);

    /**
     * 根据主岗位ID查询所有换岗路径（一个主岗位可能对应多条记录）
     * @param mainJobId 主岗位ID
     * @return 图谱列表
     */
    List<JobTransferGraph> findByMainJobId(Long mainJobId);

    /**
     * 分页查询（可选条件：主岗位ID）
     * @param current 当前页
     * @param size 每页条数
     * @param mainJobId 主岗位ID（可为空）
     * @return 分页结果
     */
    IPage<JobTransferGraph> pageQuery(long current, long size, Long mainJobId);

    List<JobTransferGraph> findAll();
}