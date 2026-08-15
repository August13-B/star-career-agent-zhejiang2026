package wwy.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import wwy.example.springboot.common.IdCategoryConstants;
import wwy.example.springboot.entity.JobPromotionGraph;
import wwy.example.springboot.mapper.JobPromotionGraphMapper;
import wwy.example.springboot.service.JobPromotionGraphService;
import wwy.example.springboot.tool.SnowIdCreater;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobPromotionGraphServiceImpl implements JobPromotionGraphService {

    private final JobPromotionGraphMapper jobPromotionGraphMapper;

    @Override
    public boolean add(JobPromotionGraph graph) {
        if (graph.getId() == null) {
            graph.setId(SnowIdCreater.generateId(IdCategoryConstants.JOB_PROMOTION_GRAPH));
        }
        return jobPromotionGraphMapper.insert(graph) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return jobPromotionGraphMapper.deleteById(id) > 0;
    }

    @Override
    public boolean update(JobPromotionGraph graph) {
        return jobPromotionGraphMapper.updateById(graph) > 0;
    }

    @Override
    public JobPromotionGraph findById(Long id) {
        return jobPromotionGraphMapper.selectById(id);
    }

    @Override
    public List<JobPromotionGraph> findByMainJobId(Long mainJobId) {
        LambdaQueryWrapper<JobPromotionGraph> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobPromotionGraph::getMainJobId, mainJobId);
        wrapper.orderByDesc(JobPromotionGraph::getCreateTime);
        return jobPromotionGraphMapper.selectList(wrapper);
    }

    @Override
    public IPage<JobPromotionGraph> pageQuery(long current, long size, Long mainJobId) {
        Page<JobPromotionGraph> page = new Page<>(current, size);
        LambdaQueryWrapper<JobPromotionGraph> wrapper = new LambdaQueryWrapper<>();
        if (mainJobId != null) {
            wrapper.eq(JobPromotionGraph::getMainJobId, mainJobId);
        }
        wrapper.orderByDesc(JobPromotionGraph::getCreateTime);
        return jobPromotionGraphMapper.selectPage(page, wrapper);
    }

    @Override
    public List<JobPromotionGraph> findAll() {
        LambdaQueryWrapper<JobPromotionGraph> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(JobPromotionGraph::getCreateTime);
        return jobPromotionGraphMapper.selectList(wrapper);
    }
}