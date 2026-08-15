package wwy.example.springboot.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import wwy.example.springboot.common.IdCategoryConstants;
import wwy.example.springboot.entity.JobInfo;
import wwy.example.springboot.mapper.JobInfoMapper;
import wwy.example.springboot.service.JobAIAnalysisService;
import wwy.example.springboot.service.JobInfoService;
import wwy.example.springboot.tool.SnowIdCreater;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.baomidou.mybatisplus.extension.ddl.DdlScriptErrorHandler.PrintlnLogErrorHandler.log;

@Service
public class JobInfoServiceImpl implements JobInfoService {

    private final JobInfoMapper jobInfoMapper;

    // 在类中新增依赖注入
    private final JobAIAnalysisService jobAIAnalysisService;
    // 可选：使用 Spring 的 @Async 或手动线程池，这里使用手动线程池
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // 手动构造器，对 jobAIAnalysisService 使用 @Lazy，移除 RSA_256 依赖
    public JobInfoServiceImpl(JobInfoMapper jobInfoMapper,
                              @Lazy JobAIAnalysisService jobAIAnalysisService) {
        this.jobInfoMapper = jobInfoMapper;
        this.jobAIAnalysisService = jobAIAnalysisService;
    }

    @Override
    public boolean add(JobInfo jobInfo) {
        if (jobInfo.getId() == null) {
            jobInfo.setId(SnowIdCreater.generateId(IdCategoryConstants.JOB_INFO));
        }
        // 直接保存明文，不再加密
        boolean success = jobInfoMapper.insert(jobInfo) > 0;
        if (success) {
            // 异步调用 AI 分析服务
            executor.submit(() -> {
                try {
                    jobAIAnalysisService.analyzeAndSave(jobInfo.getId());
                } catch (Exception e) {
                    log.error("AI 分析失败，jobId={}");
                }
            });
        }
        return success;
    }

    @Override
    public boolean deleteById(Long id) {
        // 逻辑删除（@TableLogic 自动处理）
        return jobInfoMapper.deleteById(id) > 0;
    }

    @Override
    public boolean update(JobInfo jobInfo) {
        // 直接更新明文，不再加密
        return jobInfoMapper.updateById(jobInfo) > 0;
    }

    @Override
    public JobInfo findById(Long id) {
        return jobInfoMapper.selectById(id);
    }

    @Override
    public List<JobInfo> findAll() {
        LambdaQueryWrapper<JobInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(JobInfo::getCreateTime);
        return jobInfoMapper.selectList(wrapper);
    }

    @Override
    public List<JobInfo> findByJobName(String jobName) {
        LambdaQueryWrapper<JobInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(jobName)) {
            wrapper.like(JobInfo::getJobName, jobName);
        }
        wrapper.orderByDesc(JobInfo::getCreateTime);
        return jobInfoMapper.selectList(wrapper);
    }

    @Override
    public IPage<JobInfo> pageQuery(long current, long size, String jobName) {
        Page<JobInfo> page = new Page<>(current, size);
        LambdaQueryWrapper<JobInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(jobName)) {
            wrapper.like(JobInfo::getJobName, jobName);
        }
        wrapper.orderByDesc(JobInfo::getCreateTime);
        return jobInfoMapper.selectPage(page, wrapper);
    }

    @Override
    public JobInfo findByJobId(Long jobId) {
        LambdaQueryWrapper<JobInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobInfo::getJobId, jobId);
        return jobInfoMapper.selectOne(wrapper);
    }

    @Override
    public Long getProfileIdByJobInfoId(Long jobInfoId) {
        JobInfo jobInfo = this.findById(jobInfoId);
        return jobInfo == null ? null : jobInfo.getJobId();
    }

    @Override
    public List<JobInfo> findListByJobId(Long jobId) {
        LambdaQueryWrapper<JobInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobInfo::getJobId, jobId);
        wrapper.orderByDesc(JobInfo::getCreateTime);
        return jobInfoMapper.selectList(wrapper);
    }
}