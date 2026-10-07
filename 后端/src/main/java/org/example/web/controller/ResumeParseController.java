package org.example.web.controller;

import lombok.RequiredArgsConstructor;
import org.example.web.service.profile.ResumeParseException;
import org.example.web.service.profile.ResumeParseService;
import org.example.web.tool.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import wwy.example.springboot.common.Result;

/**
 * 简历 PDF 解析：读取 PDF → 抽取「五件套」→ 并入个人资料 → 硬实力四项按策略加分。
 *
 * <p>落库规则见 {@code ResumeOutcomeService}（不覆盖已填写的非空值；加分只加不减、单项 ≤+15）。
 */
@RestController
@RequestMapping("/resume")
@RequiredArgsConstructor
public class ResumeParseController {

    private static final Logger logger = LoggerFactory.getLogger(ResumeParseController.class);

    private final ResumeParseService resumes;

    private Long user(String token) {
        try {
            return Long.valueOf(String.valueOf(JwtUtil.parseToken(token).get("id")));
        } catch (Exception e) {
            return null;
        }
    }

    @PostMapping("/parse")
    public Result<?> parse(@RequestParam("file") MultipartFile file,
                           @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = user(token);
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        try {
            return Result.success(resumes.parseAndApply(userId, file));
        } catch (ResumeParseException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            logger.error("【简历解析】失败，用户ID：{}", userId, e);
            return Result.error("简历解析失败，请稍后重试或手动填写个人资料");
        }
    }
}
