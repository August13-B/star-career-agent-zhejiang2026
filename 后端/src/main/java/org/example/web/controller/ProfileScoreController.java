package org.example.web.controller;

import lombok.RequiredArgsConstructor;
import org.example.web.service.profile.ReportProfileScoreService;
import org.example.web.service.profile.ResumeParseException;
import org.example.web.tool.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import wwy.example.springboot.common.Result;

/**
 * 把「多智能体联合测评报告」的结论同步到能力画像。
 *
 * <p>幂等：同一份报告只计分一次。报告生成完成后由前端自动调用一次（也可手动重试）。
 */
@RestController
@RequestMapping("/profile/score")
@RequiredArgsConstructor
public class ProfileScoreController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileScoreController.class);

    private final ReportProfileScoreService reports;

    private Long user(String token) {
        try {
            return Long.valueOf(String.valueOf(JwtUtil.parseToken(token).get("id")));
        } catch (Exception e) {
            return null;
        }
    }

    @PostMapping("/report/{reportId}")
    public Result<?> fromReport(@PathVariable Long reportId,
                                @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = user(token);
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        try {
            return Result.success(reports.score(userId, reportId));
        } catch (ResumeParseException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            logger.error("【画像同步】联合测评报告计分失败，用户ID：{}，报告ID：{}", userId, reportId, e);
            return Result.error("画像同步失败，请稍后重试");
        }
    }
}
