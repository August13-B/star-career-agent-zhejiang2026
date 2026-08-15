package wwy.example.springboot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import wwy.example.springboot.entity.JobSoftRequirement;

public interface JobSoftRequirementService {

    boolean add(JobSoftRequirement requirement);

    boolean deleteById(Long id);

    boolean deleteByJobId(Long jobId);

    boolean update(JobSoftRequirement requirement);

    JobSoftRequirement findById(Long id);

    JobSoftRequirement findByJobId(Long jobId);

    IPage<JobSoftRequirement> pageQuery(long current, long size, Long jobId);
}