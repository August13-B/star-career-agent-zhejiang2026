package wwy.example.springboot.service;

import wwy.example.springboot.dto.JobDetailVO;

public interface JobDetailService {

    /**
     * 通过画像ID获取完整的岗位详情（聚合所有关联表）
     * @param profileId 画像ID (job_requirement_profile.id)
     * @return 聚合后的详情对象
     */
    JobDetailVO getJobDetailByProfileId(Long profileId);

    // 新增：通过 job_info 的主键 ID 获取对应的画像 ID
    Long getProfileIdByJobInfoId(Long jobInfoId);

    JobDetailVO getJobDetailByJobInfoId(Long jobInfoId);
}