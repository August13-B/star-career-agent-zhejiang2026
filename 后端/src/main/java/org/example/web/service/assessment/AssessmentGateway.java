package org.example.web.service.assessment;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 能力补充测评的平台网关。
 *
 * <p>与训练同构：**接口一次性 JSON**，后端下发冻结任务指令，平台只负责「出题 / 追问判断 / 评分」；
 * 深度校验与算分全在本仓库后端。
 */
public interface AssessmentGateway {

    /**
     * @param mode   {@code ask}=出题或追问判断；{@code evaluate}=六维评分
     * @param prompt 后端冻结的完整任务指令（学生基本情况/意向/硬实力/进度/历史问答/本轮要什么/冻结证据目录）
     * @return 归一化后的 JSON（顶层恰好一个键：{@code objective_question} / {@code subjective_question} / {@code ability_evaluation}）
     */
    JsonNode execute(String mode, String prompt);
}
