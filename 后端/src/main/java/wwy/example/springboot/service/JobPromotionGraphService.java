package wwy.example.springboot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import wwy.example.springboot.entity.JobPromotionGraph;

import java.util.List;

public interface JobPromotionGraphService {

    /**
     * 新增晋升图谱
     * @param graph 图谱实体
     * @return 是否成功
     */
    boolean add(JobPromotionGraph graph);

    /**
     * 根据ID删除（逻辑删除）
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Long id);

    /**
     * 更新晋升图谱
     * @param graph 图谱实体（必须包含id）
     * @return 是否成功
     */
    boolean update(JobPromotionGraph graph);

    /**
     * 根据ID查询
     * @param id 主键
     * @return 图谱实体
     */
    JobPromotionGraph findById(Long id);

    /**
     * 根据主岗位ID查询所有晋升路径（一个主岗位可能有多条记录？根据表结构主岗位唯一？这里假设多条）
     * @param mainJobId 主岗位ID
     * @return 图谱列表
     */
    List<JobPromotionGraph> findByMainJobId(Long mainJobId);

    /**
     * 分页查询（可选条件：主岗位ID）
     * @param current 当前页
     * @param size 每页条数
     * @param mainJobId 主岗位ID（可为空）
     * @return 分页结果
     */
    IPage<JobPromotionGraph> pageQuery(long current, long size, Long mainJobId);

    List<JobPromotionGraph> findAll();

}