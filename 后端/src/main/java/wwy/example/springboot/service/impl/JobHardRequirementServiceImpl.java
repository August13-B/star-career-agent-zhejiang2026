package wwy.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import wwy.example.springboot.common.IdCategoryConstants;
import wwy.example.springboot.entity.JobHardRequirement;
import wwy.example.springboot.mapper.JobHardRequirementMapper;
import wwy.example.springboot.service.JobHardRequirementService;
import wwy.example.springboot.tool.SnowIdCreater;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobHardRequirementServiceImpl implements JobHardRequirementService {

    private final JobHardRequirementMapper jobHardRequirementMapper;

    @Override
    public boolean add(JobHardRequirement requirement) {
        if (requirement.getId() == null) {
            requirement.setId(SnowIdCreater.generateId(IdCategoryConstants.JOB_HARD_REQUIREMENT));
        }
        // 直接保存明文，不再加密
        return jobHardRequirementMapper.insert(requirement) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return jobHardRequirementMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteByJobId(Long jobId) {
        LambdaQueryWrapper<JobHardRequirement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobHardRequirement::getJobId, jobId);
        return jobHardRequirementMapper.delete(wrapper) > 0;
    }

    @Override
    public boolean update(JobHardRequirement requirement) {
        // 直接更新明文，不再加密
        return jobHardRequirementMapper.updateById(requirement) > 0;
    }

    @Override
    public JobHardRequirement findById(Long id) {
        return jobHardRequirementMapper.selectById(id);
    }

    @Override
    public JobHardRequirement findByJobId(Long jobId) {
        LambdaQueryWrapper<JobHardRequirement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobHardRequirement::getJobId, jobId);
        return jobHardRequirementMapper.selectOne(wrapper);
    }

    @Override
    public IPage<JobHardRequirement> pageQuery(long current, long size, Long jobId) {
        Page<JobHardRequirement> page = new Page<>(current, size);
        LambdaQueryWrapper<JobHardRequirement> wrapper = new LambdaQueryWrapper<>();
        if (jobId != null) {
            wrapper.eq(JobHardRequirement::getJobId, jobId);
        }
        wrapper.orderByDesc(JobHardRequirement::getCreateTime);
        return jobHardRequirementMapper.selectPage(page, wrapper);
    }
}