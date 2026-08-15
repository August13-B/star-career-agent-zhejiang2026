package wwy.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import wwy.example.springboot.common.IdCategoryConstants;
import wwy.example.springboot.entity.JobRequirementProfile;
import wwy.example.springboot.mapper.JobRequirementProfileMapper;
import wwy.example.springboot.service.JobRequirementProfileService;
import wwy.example.springboot.tool.SnowIdCreater;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobRequirementProfileServiceImpl implements JobRequirementProfileService {

    private final JobRequirementProfileMapper jobRequirementProfileMapper;

    @Override
    public boolean add(JobRequirementProfile profile) {
        if (profile.getId() == null) {
            profile.setId(SnowIdCreater.generateId(IdCategoryConstants.JOB_REQUIREMENT_PROFILE));
        }
        return jobRequirementProfileMapper.insert(profile) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return jobRequirementProfileMapper.deleteById(id) > 0;
    }

    @Override
    public boolean update(JobRequirementProfile profile) {
        return jobRequirementProfileMapper.updateById(profile) > 0;
    }

    @Override
    public JobRequirementProfile findById(Long id) {
        return jobRequirementProfileMapper.selectById(id);
    }

    @Override
    public List<JobRequirementProfile> findAll() {
        LambdaQueryWrapper<JobRequirementProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(JobRequirementProfile::getCreateTime);
        return jobRequirementProfileMapper.selectList(wrapper);
    }

    @Override
    public List<JobRequirementProfile> findByPositionName(String positionName) {
        LambdaQueryWrapper<JobRequirementProfile> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(positionName)) {
            wrapper.like(JobRequirementProfile::getPositionName, positionName);
        }
        wrapper.orderByDesc(JobRequirementProfile::getCreateTime);
        return jobRequirementProfileMapper.selectList(wrapper);
    }

    @Override
    public List<JobRequirementProfile> findByCategory(String category) {
        LambdaQueryWrapper<JobRequirementProfile> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(category)) {
            wrapper.eq(JobRequirementProfile::getCategory, category);
        }
        wrapper.orderByDesc(JobRequirementProfile::getCreateTime);
        return jobRequirementProfileMapper.selectList(wrapper);
    }

    @Override
    public List<JobRequirementProfile> findByLevel(Integer level) {
        LambdaQueryWrapper<JobRequirementProfile> wrapper = new LambdaQueryWrapper<>();
        if (level != null) {
            wrapper.eq(JobRequirementProfile::getLevel, level);
        }
        wrapper.orderByDesc(JobRequirementProfile::getCreateTime);
        return jobRequirementProfileMapper.selectList(wrapper);
    }

    @Override
    public IPage<JobRequirementProfile> pageQuery(long current, long size, String positionName, String category, Integer level) {
        Page<JobRequirementProfile> page = new Page<>(current, size);
        LambdaQueryWrapper<JobRequirementProfile> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(positionName)) {
            wrapper.like(JobRequirementProfile::getPositionName, positionName);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(JobRequirementProfile::getCategory, category);
        }
        if (level != null) {
            wrapper.eq(JobRequirementProfile::getLevel, level);
        }
        wrapper.orderByDesc(JobRequirementProfile::getCreateTime);
        return jobRequirementProfileMapper.selectPage(page, wrapper);
    }

    @Override
    public JobRequirementProfile findByJobId(Long jobId) {
        return jobRequirementProfileMapper.selectById(jobId);
    }

    @Override
    public JobRequirementProfile findByPositionNameExact(String positionName) {
        if (positionName == null || positionName.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<JobRequirementProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobRequirementProfile::getPositionName, positionName);
        return jobRequirementProfileMapper.selectOne(wrapper);
    }
}