package wwy.example.springboot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import wwy.example.springboot.common.Result;
import wwy.example.springboot.dto.JobTenDimensionScoreDTO;
import wwy.example.springboot.service.JobTenDimensionScoreService;

@RestController
@RequestMapping("/job-score")
@RequiredArgsConstructor
public class JobTenDimensionScoreController {

    private final JobTenDimensionScoreService scoreService;

    @GetMapping("/ten-dimension/{profileId}")
    public Result<JobTenDimensionScoreDTO> getTenDimensionScores(@PathVariable Long profileId) {
        JobTenDimensionScoreDTO scores = scoreService.calculateScores(profileId);
        return Result.success(scores);
    }
}