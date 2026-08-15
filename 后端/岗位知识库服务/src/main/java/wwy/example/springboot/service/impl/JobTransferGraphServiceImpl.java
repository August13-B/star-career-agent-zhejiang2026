package wwy.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import wwy.example.springboot.common.IdCategoryConstants;
import wwy.example.springboot.entity.JobTransferGraph;
import wwy.example.springboot.mapper.JobTransferGraphMapper;
import wwy.example.springboot.service.JobTransferGraphService;
import wwy.example.springboot.tool.SnowIdCreater;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobTransferGraphServiceImpl implements JobTransferGraphService {

    private final JobTransferGraphMapper jobTransferGraphMapper;

    @Override
    public boolean add(JobTransferGraph graph) {
        if (graph.getId() == null) {
            graph.setId(SnowIdCreater.generateId(IdCategoryConstants.JOB_TRANSFER_GRAPH));
        }
        return jobTransferGraphMapper.insert(graph) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return jobTransferGraphMapper.deleteById(id) > 0;
    }

    @Override
    public boolean update(JobTransferGraph graph) {
        return jobTransferGraphMapper.updateById(graph) > 0;
    }

    @Override
    public JobTransferGraph findById(Long id) {
        return jobTransferGraphMapper.selectById(id);
    }

    @Override
    public List<JobTransferGraph> findByMainJobId(Long mainJobId) {
        LambdaQueryWrapper<JobTransferGraph> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobTransferGraph::getMainJobId, mainJobId);
        wrapper.orderByDesc(JobTransferGraph::getCreateTime);
        return jobTransferGraphMapper.selectList(wrapper);
    }

    @Override
    public IPage<JobTransferGraph> pageQuery(long current, long size, Long mainJobId) {
        Page<JobTransferGraph> page = new Page<>(current, size);
        LambdaQueryWrapper<JobTransferGraph> wrapper = new LambdaQueryWrapper<>();
        if (mainJobId != null) {
            wrapper.eq(JobTransferGraph::getMainJobId, mainJobId);
        }
        wrapper.orderByDesc(JobTransferGraph::getCreateTime);
        return jobTransferGraphMapper.selectPage(page, wrapper);
    }

    @Override
    public List<JobTransferGraph> findAll() {
        LambdaQueryWrapper<JobTransferGraph> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(JobTransferGraph::getCreateTime);
        return jobTransferGraphMapper.selectList(wrapper);
    }
}