package org.example.web.controller;

import java.util.Map;

import org.example.web.entity.Result;
import org.example.web.service.GrowPlanService;
import org.example.web.tool.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GrowController.class);

    @org.springframework.beans.factory.annotation.Autowired
    private org.example.web.security.AccessGuard access;


    @Autowired
    private GrowPlanService growPlanService;

    @Autowired
    private org.example.web.service.grow.GrowCycleService growCycleService;

    /** 查询用户的成长计划（含任务与完成情况） */
    @GetMapping("/plans")
    @CrossOrigin
    public Result<?> getPlans(@RequestParam Long userId) {
        access.self(userId);

        try {
            return Result.success("获取成长计划成功", growPlanService.getPlansWithTasks(userId));
        } catch (Exception e) {
            log.error("【成长计划】查询失败，userId={}", userId, e);
            return Result.error("成长计划查询失败：" + friendly(e));
        }
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
        access.task(id);

        boolean ok = growPlanService.updateTaskStatus(id, patch);
        return ok ? Result.success("任务状态更新成功") : Result.error("任务不存在或更新失败");
    }

    /** 提交任务文件（提交即完成；元数据存 grow_task.grow_recourse） */
    @PostMapping("/tasks/{id}/resource")
    @CrossOrigin
    public Result<?> submitTaskResource(@PathVariable Long id,
                                        @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                        @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return Result.error("登录状态无效");
        }
        try {
            return Result.success("文件已提交，任务已完成", growCycleService.submitResource(userId, id, file));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("文件提交失败，请重试");
        }
    }

    /** 选择本次完成难度：太简单 / 中等 / 困难 */
    @PostMapping("/tasks/{id}/difficulty")
    @CrossOrigin
    public Result<?> setTaskDifficulty(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                       @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return Result.error("登录状态无效");
        }
        try {
            String difficulty = body == null ? "" : String.valueOf(body.getOrDefault("difficulty", ""));
            return Result.success("已记录本次完成难度", growCycleService.chooseDifficulty(userId, id, difficulty));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 依据「上一周期计划 + 完成情况 + 本次难度」生成下一周期计划（新建 grow_plan） */
    @PostMapping("/plans/{id}/next-cycle")
    @CrossOrigin
    public Result<?> nextCycle(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body,
                               @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return Result.error("登录状态无效");
        }
        try {
            Map<String, Object> payload = body == null ? Map.of() : body;
            Long taskId = null;
            Object rawTaskId = payload.get("taskId");
            if (rawTaskId != null && !String.valueOf(rawTaskId).isBlank() && !"null".equals(String.valueOf(rawTaskId))) {
                taskId = Long.parseLong(String.valueOf(rawTaskId));
            }
            String difficulty = String.valueOf(payload.getOrDefault("difficulty", ""));
            boolean force = Boolean.parseBoolean(String.valueOf(payload.getOrDefault("force", false)));
            return Result.success("已生成下一周期计划", growCycleService.generateNextCycle(userId, id, taskId, difficulty, force));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("【成长下一周期】生成失败，userId={}，planId={}", userId, id, e);
            return Result.error(friendly(e));
        }
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

    /** 新增自定义规划（1/3/5 年） */
    @PostMapping("/plans")
    @CrossOrigin
    public Result<?> addPlan(@RequestBody Map<String, Object> body,
                             @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return Result.error("登录状态无效");
        }
        try {
            return Result.success("规划已创建", growPlanService.addPlan(userId, body));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 删除规划（连同其任务与记录） */
    @DeleteMapping("/plans/{id}")
    @CrossOrigin
    public Result<?> deletePlan(@PathVariable Long id,
                                @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return Result.error("登录状态无效");
        }
        return growPlanService.deletePlan(userId, id)
                ? Result.success("规划已删除") : Result.error("规划不存在或无权限");
    }

    /** 删除任务（连同其完成记录） */
    @DeleteMapping("/tasks/{id}")
    @CrossOrigin
    public Result<?> deleteTask(@PathVariable Long id,
                                @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return Result.error("登录状态无效");
        }
        return growPlanService.deleteTask(userId, id)
                ? Result.success("任务已删除") : Result.error("任务不存在或无权限");
    }

    /** 把底层异常翻译成可操作的提示（含"没跑迁移"、超时等情况），避免只剩一句"生成失败"。 */
    private static String friendly(Exception e) {
        StringBuilder chain = new StringBuilder();
        for (Throwable one = e; one != null; one = one.getCause()) {
            chain.append(one.getMessage()).append(" | ");
            if (one.getCause() == one) {
                break;
            }
        }
        String message = chain.toString();
        String lower = message.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("unknown column") || lower.contains("bad sql grammar") || lower.contains("doesn't exist")) {
            return "数据库缺少新字段：请先执行迁移（python manage.py db migrate）后重启后端再试";
        }
        if (lower.contains("cannot be null") || lower.contains("doesn't have a default")) {
            return "计划保存失败：缺少必填字段，请重试";
        }
        if (e instanceof java.util.concurrent.TimeoutException || lower.contains("timeout") || message.contains("超时")) {
            return "调用百宝箱生成计划超时，请稍后重试；若持续失败请确认平台侧应用与模型状态";
        }
        String shortMessage = message.length() > 120 ? message.substring(0, 120) + "…" : message;
        return "下一周期计划生成失败：" + (shortMessage.isBlank() ? "请查看后端日志（logs/backend.log）" : shortMessage);
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
