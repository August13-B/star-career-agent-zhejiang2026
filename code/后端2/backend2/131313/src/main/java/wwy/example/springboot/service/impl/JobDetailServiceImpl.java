package wwy.example.springboot.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import wwy.example.springboot.dto.JobDetailVO;
import wwy.example.springboot.entity.JobInfo;
import wwy.example.springboot.entity.JobPromotionGraph;
import wwy.example.springboot.entity.JobRequirementProfile;
import wwy.example.springboot.entity.JobTransferGraph;
import wwy.example.springboot.service.*;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class JobDetailServiceImpl implements JobDetailService {

    private final JobRequirementProfileService jobRequirementProfileService;
    private final JobInfoService jobInfoService;
    private final JobHardRequirementService jobHardRequirementService;
    private final JobSkillRequirementService jobSkillRequirementService;
    private final JobSoftRequirementService jobSoftRequirementService;
    private final JobMarketInfoService jobMarketInfoService;
    private final JobPromotionGraphService jobPromotionGraphService;
    private final JobTransferGraphService jobTransferGraphService;

    @Override
    public JobDetailVO getJobDetailByProfileId(Long profileId) {
        JobDetailVO vo = new JobDetailVO();

        // 1. 岗位画像
        JobRequirementProfile requirementProfile = jobRequirementProfileService.findById(profileId);
        if (requirementProfile == null) {
            throw new RuntimeException("岗位画像不存在，profileId=" + profileId);
        }
        vo.setJobRequirementProfile(requirementProfile);

        // 2. 查询所有关联的 job_info 记录（一个画像可能对应多个岗位）
        List<JobInfo> jobInfoList = jobInfoService.findListByJobId(profileId);
        vo.setJobInfoList(jobInfoList);

        // 3. 子表查询（这些表的外键 job_id 指向画像ID，应有唯一约束）
        vo.setHardRequirement(jobHardRequirementService.findByJobId(profileId));
        vo.setSkillRequirement(jobSkillRequirementService.findByJobId(profileId));
        vo.setSoftRequirement(jobSoftRequirementService.findByJobId(profileId));
        vo.setMarketInfo(jobMarketInfoService.findByJobId(profileId));

        // 4. 晋升图谱和换岗图谱
        List<JobPromotionGraph> promotionGraphs = jobPromotionGraphService.findByMainJobId(profileId);
        vo.setPromotionGraphs(promotionGraphs);
        List<JobTransferGraph> transferGraphs = jobTransferGraphService.findByMainJobId(profileId);
        vo.setTransferGraphs(transferGraphs);

        return vo;
    }

    @Override
    public Long getProfileIdByJobInfoId(Long jobInfoId) {
        JobInfo jobInfo = jobInfoService.findById(jobInfoId);
        if (jobInfo == null) {
            throw new RuntimeException("岗位不存在，id=" + jobInfoId);
        }
        return jobInfo.getJobId();
    }

    @Override
    public JobDetailVO getJobDetailByJobInfoId(Long jobInfoId) {
        JobInfo jobInfo = jobInfoService.findById(jobInfoId);
        if (jobInfo == null) {
            throw new RuntimeException("岗位不存在，id=" + jobInfoId);
        }
        Long profileId = jobInfo.getJobId();
        if (profileId == null) {
            throw new RuntimeException("该岗位未关联画像，无法获取详情");
        }
        return getJobDetailByProfileId(profileId);
    }
}