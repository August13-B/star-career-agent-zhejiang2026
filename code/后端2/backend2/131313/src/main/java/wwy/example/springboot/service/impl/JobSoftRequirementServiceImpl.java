package wwy.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import wwy.example.springboot.common.IdCategoryConstants;
import wwy.example.springboot.entity.JobSoftRequirement;
import wwy.example.springboot.mapper.JobSoftRequirementMapper;
import wwy.example.springboot.service.JobSoftRequirementService;
import wwy.example.springboot.tool.SnowIdCreater;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSoftRequirementServiceImpl implements JobSoftRequirementService {

    private final JobSoftRequirementMapper jobSoftRequirementMapper;

    @Override
    public boolean add(JobSoftRequirement requirement) {
        if (requirement.getId() == null) {
            requirement.setId(SnowIdCreater.generateId(IdCategoryConstants.JOB_SOFT_REQUIREMENT));
        }
        return jobSoftRequirementMapper.insert(requirement) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return jobSoftRequirementMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteByJobId(Long jobId) {
        LambdaQueryWrapper<JobSoftRequirement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobSoftRequirement::getJobId, jobId);
        return jobSoftRequirementMapper.delete(wrapper) > 0;
    }

    @Override
    public boolean update(JobSoftRequirement requirement) {
        return jobSoftRequirementMapper.updateById(requirement) > 0;
    }

    @Override
    public JobSoftRequirement findById(Long id) {
        return jobSoftRequirementMapper.selectById(id);
    }

    @Override
    public JobSoftRequirement findByJobId(Long jobId) {
        LambdaQueryWrapper<JobSoftRequirement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobSoftRequirement::getJobId, jobId);
        return jobSoftRequirementMapper.selectOne(wrapper);
    }

    @Override
    public IPage<JobSoftRequirement> pageQuery(long current, long size, Long jobId) {
        Page<JobSoftRequirement> page = new Page<>(current, size);
        LambdaQueryWrapper<JobSoftRequirement> wrapper = new LambdaQueryWrapper<>();
        if (jobId != null) {
            wrapper.eq(JobSoftRequirement::getJobId, jobId);
        }
        wrapper.orderByDesc(JobSoftRequirement::getCreateTime);
        return jobSoftRequirementMapper.selectPage(page, wrapper);
    }
}