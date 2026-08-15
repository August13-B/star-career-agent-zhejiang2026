package wwy.example.springboot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import wwy.example.springboot.entity.JobMarketInfo;

public interface JobMarketInfoService {

    boolean add(JobMarketInfo info);

    boolean deleteById(Long id);

    boolean deleteByJobId(Long jobId);

    boolean update(JobMarketInfo info);

    JobMarketInfo findById(Long id);

    JobMarketInfo findByJobId(Long jobId);

    IPage<JobMarketInfo> pageQuery(long current, long size, Long jobId, String industry);
}