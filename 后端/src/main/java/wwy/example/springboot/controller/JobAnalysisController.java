package wwy.example.springboot.controller;

import lombok.RequiredArgsConstructor;
import org.example.web.security.AccessGuard;
import org.springframework.web.bind.annotation.*;
import wwy.example.springboot.common.Result;
import wwy.example.springboot.service.JobAIAnalysisService;

@RestController
@CrossOrigin
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class JobAnalysisController {

    private final JobAIAnalysisService jobAIAnalysisService;
    private final AccessGuard accessGuard;

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

    /**
     * 按「岗位画像」补全十维要求（学历/实习/技能/证书 + 六项软实力）。
     *
     * <p>用途：训练出题占位符（岗位关键技能）与「对照岗位要求说差距」、以及人岗匹配计算。
     * 幂等：已补全的画像直接跳过。仅管理员可调用。
     */
    @PostMapping("/profile/{profileId}")
    public Result<String> analyzeProfile(@PathVariable Long profileId) {
        accessGuard.admin();
        try {
            jobAIAnalysisService.analyzeProfileAndSave(profileId);
            return Result.success("画像要求补全完成");
        } catch (Exception e) {
            return Result.error("补全失败：" + e.getMessage());
        }
    }

    /**
     * 批量补全仍缺要求的岗位画像（串行调用 AI，建议分批执行，例如每次 20 个）。
     *
     * @param limit 本次最多处理多少个（1~100）
     * @return 汇总 {@code total / processed / skipped / failed / remaining}
     */
    @PostMapping("/batch")
    public Result<java.util.Map<String, Object>> analyzeMissingProfiles(@RequestParam(defaultValue = "20") int limit) {
        accessGuard.admin();
        return Result.success("批量补全完成", jobAIAnalysisService.analyzeMissingProfiles(limit));
    }
}
