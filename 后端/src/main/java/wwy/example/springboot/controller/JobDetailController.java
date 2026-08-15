package wwy.example.springboot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import wwy.example.springboot.common.Result;
import wwy.example.springboot.dto.JobDetailVO;
import wwy.example.springboot.dto.JobProfileSimpleVO;
import wwy.example.springboot.entity.JobRequirementProfile;
import wwy.example.springboot.service.JobDetailService;
import wwy.example.springboot.service.JobRequirementProfileService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/job-detail")
@RequiredArgsConstructor
public class JobDetailController {

    private final JobDetailService jobDetailService;
    @Autowired
    private JobRequirementProfileService profileService;


    @GetMapping("/by-profile/{profileId}")
    public Result<JobProfileSimpleVO> getJobProfileById(@PathVariable Long profileId) {
        // 使用注入的实例，而不是类名
        JobRequirementProfile profile = profileService.findById(profileId);
        if (profile == null) {
            return Result.error("岗位画像不存在");
        }
        JobProfileSimpleVO vo = new JobProfileSimpleVO();
        vo.setJobRequirementProfile(profile);
        return Result.success(vo);
    }

    /**
     * 通过 job_info 表的主键 ID 获取该岗位的完整详情（包含画像、子表、晋升/换岗图谱等）
     * @param jobInfoId job_info 表的主键
     * @return 完整岗位详情
     */
    @GetMapping("/by-job-info/{jobInfoId}")
    public Result<JobDetailVO> getJobDetailByJobInfoId(@PathVariable Long jobInfoId) {
        try {
            JobDetailVO detail = jobDetailService.getJobDetailByJobInfoId(jobInfoId);
            return Result.success(detail);
        } catch (Exception e) {
            return Result.error("获取岗位详情失败：" + e.getMessage());
        }
    }

}