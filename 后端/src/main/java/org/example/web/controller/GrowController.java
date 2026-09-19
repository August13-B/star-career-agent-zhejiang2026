package org.example.web.controller;

import java.util.Map;

import org.example.web.entity.Result;
import org.example.web.service.GrowPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 成长规划 / 计划跟踪接口
 *
 * <p>数据来自职业报告第 6 段的结构化 1/3/5 年目标（前端不展示结构化原文）。
 * 本轮只提供后端 API，前端 UI 后续接入。
 */
@RestController
@RequestMapping("/grow")
public class GrowController {

    @Autowired
    private GrowPlanService growPlanService;

    /** 查询用户的成长计划（含任务与完成情况） */
    @GetMapping("/plans")
    @CrossOrigin
    public Result<?> getPlans(@RequestParam Long userId) {
        return Result.success("获取成长计划成功", growPlanService.getPlansWithTasks(userId));
    }

    /**
     * 更新任务完成情况（并自动重算计划进度）。
     *
     * <p>请求体可含：{@code status}（0未开始/1进行中/2已完成/3已暂停/4已延期）、
     * {@code progress}、{@code completionDetail}、{@code effectScore}。
     */
    @PatchMapping("/tasks/{id}")
    @CrossOrigin
    public Result<?> updateTask(@PathVariable Long id, @RequestBody Map<String, Object> patch) {
        boolean ok = growPlanService.updateTaskStatus(id, patch);
        return ok ? Result.success("任务状态更新成功") : Result.error("任务不存在或更新失败");
    }
}
