package org.example.web.service;

import java.util.List;
import java.util.Map;

/**
 * 成长规划 / 计划跟踪服务
 *
 * <p>数据来源：职业报告第 6 段（report_composition）输出的结构化 1/3/5 年目标
 * （前端不展示，仅供后端落库与后续功能对接）。
 */
public interface GrowPlanService {

    /**
     * 把报告里的结构化目标落库（覆盖该用户旧的 active 计划）。
     *
     * @param userId    用户ID
     * @param reportId  关联的职业报告ID
     * @param targetJob 目标岗位（可空，落库时兜底）
     * @param goals     结构化目标列表，每项含 horizon/title/goal/criteria/metrics/keyActions/skills
     */
    void saveGoalsFromReport(Long userId, Long reportId, String targetJob, List<Map<String, Object>> goals);

    /** 查询用户的成长计划（含任务），供计划跟踪/动态调整使用 */
    List<Map<String, Object>> getPlansWithTasks(Long userId);

    /**
     * 更新任务完成情况，并自动重算所属计划的 progress / total_status。
     *
     * @param taskId 任务ID
     * @param patch  可含 status / progress / completionDetail / effectScore
     * @return 是否更新成功
     */
    boolean updateTaskStatus(Long taskId, Map<String, Object> patch);
}
