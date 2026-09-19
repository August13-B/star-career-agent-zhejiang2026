package org.example.web.controller;

import java.util.Map;

import org.example.web.entity.Result;
import org.example.web.service.GrowPlanService;
import org.example.web.tool.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    /** 新增自定义代办任务（归属某个 1/3/5 年计划） */
    @PostMapping("/tasks")
    @CrossOrigin
    public Result<?> addTask(@RequestBody Map<String, Object> body,
                             @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return Result.error("登录状态无效");
        }
        try {
            return Result.success("任务已创建", growPlanService.addTask(userId, body));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 为任务追加一条完成情况记录（时间线，可多条） */
    @PostMapping("/tasks/{id}/records")
    @CrossOrigin
    public Result<?> addTaskRecord(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                   @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return Result.error("登录状态无效");
        }
        try {
            String content = body == null ? "" : String.valueOf(body.getOrDefault("content", ""));
            return Result.success("记录已添加", growPlanService.addTaskRecord(userId, id, content));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    private Long currentUserId(String token) {
        try {
            Map<String, Object> claims = JwtUtil.parseToken(token);
            return Long.parseLong(String.valueOf(claims.get("id")));
        } catch (Exception e) {
            return null;
        }
    }
}
