package wwy.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import wwy.example.springboot.common.IdCategoryConstants;
import wwy.example.springboot.entity.JobSkillRequirement;
import wwy.example.springboot.mapper.JobSkillRequirementMapper;
import wwy.example.springboot.service.JobSkillRequirementService;
import wwy.example.springboot.tool.SnowIdCreater;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSkillRequirementServiceImpl implements JobSkillRequirementService {

    private final JobSkillRequirementMapper jobSkillRequirementMapper;

    @Override
    public boolean add(JobSkillRequirement requirement) {
        if (requirement.getId() == null) {
            requirement.setId(SnowIdCreater.generateId(IdCategoryConstants.JOB_SKILL_REQUIREMENT));
        }
        return jobSkillRequirementMapper.insert(requirement) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return jobSkillRequirementMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteByJobId(Long jobId) {
        LambdaQueryWrapper<JobSkillRequirement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobSkillRequirement::getJobId, jobId);
        return jobSkillRequirementMapper.delete(wrapper) > 0;
    }

    @Override
    public boolean update(JobSkillRequirement requirement) {
        return jobSkillRequirementMapper.updateById(requirement) > 0;
    }

    @Override
    public JobSkillRequirement findById(Long id) {
        return jobSkillRequirementMapper.selectById(id);
    }

    @Override
    public JobSkillRequirement findByJobId(Long jobId) {
        LambdaQueryWrapper<JobSkillRequirement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobSkillRequirement::getJobId, jobId);
        return jobSkillRequirementMapper.selectOne(wrapper);
    }

    @Override
    public IPage<JobSkillRequirement> pageQuery(long current, long size, Long jobId) {
        Page<JobSkillRequirement> page = new Page<>(current, size);
        LambdaQueryWrapper<JobSkillRequirement> wrapper = new LambdaQueryWrapper<>();
        if (jobId != null) {
            wrapper.eq(JobSkillRequirement::getJobId, jobId);
        }
        wrapper.orderByDesc(JobSkillRequirement::getCreateTime);
        return jobSkillRequirementMapper.selectPage(page, wrapper);
    }
}