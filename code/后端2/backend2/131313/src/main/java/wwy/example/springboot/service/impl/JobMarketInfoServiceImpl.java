package wwy.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import wwy.example.springboot.common.IdCategoryConstants;
import wwy.example.springboot.entity.JobMarketInfo;
import wwy.example.springboot.mapper.JobMarketInfoMapper;
import wwy.example.springboot.service.JobMarketInfoService;
import wwy.example.springboot.tool.SnowIdCreater;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobMarketInfoServiceImpl implements JobMarketInfoService {

    private final JobMarketInfoMapper jobMarketInfoMapper;

    @Override
    public boolean add(JobMarketInfo info) {
        if (info.getId() == null) {
            info.setId(SnowIdCreater.generateId(IdCategoryConstants.JOB_MARKET_INFO));
        }
        // 直接保存明文，不再加密
        return jobMarketInfoMapper.insert(info) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return jobMarketInfoMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteByJobId(Long jobId) {
        LambdaQueryWrapper<JobMarketInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobMarketInfo::getJobId, jobId);
        return jobMarketInfoMapper.delete(wrapper) > 0;
    }

    @Override
    public boolean update(JobMarketInfo info) {
        // 直接更新明文，不再加密
        return jobMarketInfoMapper.updateById(info) > 0;
    }

    @Override
    public JobMarketInfo findById(Long id) {
        return jobMarketInfoMapper.selectById(id);
    }

    @Override
    public JobMarketInfo findByJobId(Long jobId) {
        LambdaQueryWrapper<JobMarketInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobMarketInfo::getJobId, jobId);
        return jobMarketInfoMapper.selectOne(wrapper);
    }

    @Override
    public IPage<JobMarketInfo> pageQuery(long current, long size, Long jobId, String industry) {
        Page<JobMarketInfo> page = new Page<>(current, size);
        LambdaQueryWrapper<JobMarketInfo> wrapper = new LambdaQueryWrapper<>();
        if (jobId != null) {
            wrapper.eq(JobMarketInfo::getJobId, jobId);
        }
        if (StringUtils.hasText(industry)) {
            wrapper.like(JobMarketInfo::getIndustry, industry);
        }
        wrapper.orderByDesc(JobMarketInfo::getCreateTime);
        return jobMarketInfoMapper.selectPage(page, wrapper);
    }
}