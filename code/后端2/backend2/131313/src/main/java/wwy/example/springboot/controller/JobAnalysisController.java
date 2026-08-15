package wwy.example.springboot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import wwy.example.springboot.common.Result;
import wwy.example.springboot.service.JobAIAnalysisService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class JobAnalysisController {

    private final JobAIAnalysisService jobAIAnalysisService;

    /**
     * 手动触发AI分析指定岗位
     * @param jobInfoId job_info 表的主键ID
     */
    @PostMapping("/job/{jobInfoId}")
    public Result<String> analyzeJob(@PathVariable Long jobInfoId) {
        try {
            jobAIAnalysisService.analyzeAndSave(jobInfoId);
            return Result.success("分析完成");
        } catch (Exception e) {
            return Result.error("分析失败：" + e.getMessage());
        }
    }


}