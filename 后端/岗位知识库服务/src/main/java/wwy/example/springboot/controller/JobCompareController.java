package wwy.example.springboot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import wwy.example.springboot.common.Result;
import wwy.example.springboot.dto.JobCompareResult;
import wwy.example.springboot.dto.JobIdRequest;
import wwy.example.springboot.service.JobCompareService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/job-compare")
@RequiredArgsConstructor
public class JobCompareController {

    private final JobCompareService jobCompareService;

    @PostMapping("/analyze-new-job")
    public Result<JobCompareResult> analyzeNewJob(@RequestBody JobIdRequest request) {
        try {
            JobCompareResult result = jobCompareService.compareWithGraph(request.getNewJobId());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("分析失败: " + e.getMessage());
        }
    }
}