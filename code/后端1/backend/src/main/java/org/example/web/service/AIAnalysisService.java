package org.example.web.service;

import org.example.web.entity.Result;
import org.example.web.entity.StudentAbilityScore;

import java.util.List;

public interface AIAnalysisService {
    /**
     * 统一格式的AI能力分析评分接口
     * @param userId 用户ID（必填）
     * @param userMessage 用户补充需求（可选）
     * @param temperature 模型温度值（必填，DTO已默认0.3）
     * @return 评分结果
     */
    Result<List<StudentAbilityScore>> analyzeAndSaveAbilityScore(Long userId, String userMessage, Float temperature);
}