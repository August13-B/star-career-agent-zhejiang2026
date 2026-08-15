package org.example.web.controller;

import org.example.web.entity.Result;
import org.example.web.entity.StudentAbilityScore;
import org.example.web.entity.request.AbilityAnalysisRequest;
import org.example.web.service.AIAnalysisService;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
// Spring Boot 3.x 唯一正确的Valid导入，彻底解决包不存在问题
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/ai/analysis")
@RequiredArgsConstructor
public class AIAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(AIAnalysisController.class);
    private final AIAnalysisService aiAnalysisService;

    /**
     * AI能力分析评分接口
     * 接收JSON格式: { "userId": 6, "message": "重点分析编程能力", "temperature": 0.1 }
     */
    @PostMapping("/ability/score")
    public Result<List<StudentAbilityScore>> analyzeAbilityScore(@RequestBody @Valid AbilityAnalysisRequest request) {
        logger.info("【AI分析接口】收到请求，userId: {}, message: {}, temperature: {}", 
                request.getUserId(), request.getMessage(), request.getTemperature());
        Result<List<StudentAbilityScore>> result = aiAnalysisService.analyzeAndSaveAbilityScore(
                request.getUserId(),
                request.getMessage(),
                request.getTemperature()
        );
        // 打印返回给前端的标准JSON格式数据
        logger.info("【AI分析接口】返回前端标准JSON数据：{}", JSONUtil.toJsonStr(result));
        return result;
    }
}
