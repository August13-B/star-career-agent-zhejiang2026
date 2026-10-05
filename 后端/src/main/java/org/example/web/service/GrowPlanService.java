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
     * 把报告里的结构化目标**追加**导入个人成长（生成 1/3/5 年规划 + 待办）。
     *
     * <p>与旧的自动「覆盖」不同：**不清理用户已有规划/待办**；同一份报告只应导入一次
     * （由调用方先查 {@link #isReportImported}）。
     *
     * @param userId    用户ID
     * @param reportId  关联的职业报告ID
     * @param targetJob 目标岗位（可空，落库时兜底）
     * @param goals     结构化目标列表，每项含 horizon/title/goal/criteria/metrics/keyActions/skills
     * @return 导入统计：{@code {plans: 规划数, tasks: 待办数}}
     */
    java.util.Map<String, Object> importGoalsFromReport(Long userId, Long reportId, String targetJob, List<Map<String, Object>> goals);

    /** 该报告是否已导入过个人成长（按 grow_plan.report_id 判定） */
    boolean isReportImported(Long userId, Long reportId);

    /** 该用户已导入过个人成长的全部报告ID（供报告列表标记「已导入」） */
    java.util.Set<Long> importedReportIds(Long userId);

    /** 查询用户的成长计划（含任务与完成情况记录） */
    List<Map<String, Object>> getPlansWithTasks(Long userId);

    /** 新增自定义代办任务（归属某个 1/3/5 年计划） */
    Map<String, Object> addTask(Long userId, Map<String, Object> body);

    /** 新增自定义规划（1/3/5 年） */
    Map<String, Object> addPlan(Long userId, Map<String, Object> body);

    /** 删除任务（逻辑删除，同时删除其完成记录） */
    boolean deleteTask(Long userId, Long taskId);

    /** 删除规划（逻辑删除，同时删除其任务与记录） */
    boolean deletePlan(Long userId, Long planId);

    /** 为任务追加一条完成情况记录（时间线，可多条） */
    Map<String, Object> addTaskRecord(Long userId, Long taskId, String content);

    /**
     * 更新任务完成情况，并自动重算所属计划的 progress / total_status。
     *
     * @param taskId 任务ID
     * @param patch  可含 status / progress / completionDetail / effectScore
     * @return 是否更新成功
     */
    boolean updateTaskStatus(Long taskId, Map<String, Object> patch);
}
