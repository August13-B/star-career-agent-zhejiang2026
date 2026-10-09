package org.example.web.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.example.web.entity.GrowPlan;
import org.example.web.entity.GrowTask;
import org.example.web.entity.GrowTaskRecord;
import org.example.web.mapper.GrowPlanMapper;
import org.example.web.mapper.GrowTaskMapper;
import org.example.web.mapper.GrowTaskRecordMapper;
import org.example.web.service.GrowPlanService;
import org.example.web.tool.SnowIdCreater;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 成长规划 / 计划跟踪实现
 *
 * <p>落库策略：目标由用户在「查看报告」里**手动一键导入**，采用**追加**语义（不清理已有规划/待办）；
 * 同一份报告只导入一次（幂等判断见 {@link #isReportImported}）；任务状态变化时自动重算计划进度。
 */
@Slf4j
@Service
public class GrowPlanServiceImpl implements GrowPlanService {

    @Autowired
    private GrowPlanMapper growPlanMapper;

    @Autowired
    private GrowTaskMapper growTaskMapper;

    @Autowired
    private GrowTaskRecordMapper growTaskRecordMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importGoalsFromReport(Long userId, Long reportId, String targetJob, List<Map<String, Object>> goals) {
        Map<String, Object> stat = new LinkedHashMap<>();
        int planCount = 0;
        int taskCount = 0;
        if (userId == null || goals == null || goals.isEmpty()) {
            stat.put("plans", 0);
            stat.put("tasks", 0);
            return stat;
        }
        // 追加语义：不清理用户已有的规划/待办（旧实现为「生成报告时自动覆盖」，已改为手动导入 + 追加）

        String job = (targetJob == null || targetJob.isBlank()) ? "通用方向" : targetJob;
        LocalDateTime now = LocalDateTime.now();
        for (Map<String, Object> g : goals) {
            if (g == null) {
                continue;
            }
            String horizon = str(g.get("horizon"));
            int planType = horizonToType(horizon);
            int months = horizonToMonths(horizon);

            GrowPlan plan = new GrowPlan();
            plan.setId(SnowIdCreater.generateId(30));
            plan.setUserId(userId);
            plan.setMatchId(null);                       // 报告可不经匹配直接生成（见表迁移 006）
            plan.setReportId(reportId);
            plan.setTargetJob(job);
            plan.setPlanName(defaultStr(g.get("title"), horizon + " 目标"));
            plan.setPlanContent(toJson(g));              // 结构化内容（后端用）
            plan.setPlanType(planType);
            plan.setStartDate(now);
            plan.setEndDate(now.plusMonths(months));
            plan.setTotalStatus(0);
            plan.setProgress(BigDecimal.ZERO);
            plan.setCreateTime(now);
            plan.setUpdateTime(now);
            plan.setIsDeleted(0);
            growPlanMapper.insert(plan);
            planCount++;

            // keyActions → grow_task（完成情况的载体）
            Object actions = g.get("keyActions");
            if (actions instanceof List<?> list) {
                for (Object a : list) {
                    String action = str(a);
                    if (action.isBlank()) {
                        continue;
                    }
                    GrowTask task = new GrowTask();
                    task.setId(SnowIdCreater.generateId(31));
                    task.setPlanId(plan.getId());
                    task.setTaskName(truncate(action, 250));
                    task.setTaskType(1);                 // 1-技能（默认）
                    String goalText = str(g.get("goal"));
                    String criteriaText = str(g.get("criteria"));
                    String actionText = truncate(action, 200);
                    task.setTaskDesc("行动：" + actionText
                            + (goalText.isBlank() ? "" : "｜所属目标：" + goalText));
                    task.setTargetAbility(joinList(g.get("skills")));
                    // 逐条行动各自的验收标准：原来同一目标下所有任务都复制整段 criteria，导致"预期成果"条条一模一样
                    task.setExpectedOutcome("完成「" + actionText + "」"
                            + (criteriaText.isBlank() ? "" : "，并达到： " + criteriaText));
                    task.setStartDate(now);
                    task.setEndDate(now.plusMonths(months));
                    task.setProgress(BigDecimal.ZERO);
                    task.setStatus(0);
                    task.setCreateTime(now);
                    task.setUpdateTime(now);
                    task.setIsDeleted(0);
                    growTaskMapper.insert(task);
                    taskCount++;
                }
            }
        }
        stat.put("plans", planCount);
        stat.put("tasks", taskCount);
        log.info("已导入成长计划（追加）: userId={}, reportId={}, plans={}, tasks={}", userId, reportId, planCount, taskCount);
        return stat;
    }

    @Override
    public boolean isReportImported(Long userId, Long reportId) {
        if (userId == null || reportId == null) {
            return false;
        }
        return growPlanMapper.selectCount(new QueryWrapper<GrowPlan>()
                .eq("user_id", userId).eq("report_id", reportId)) > 0;
    }

    @Override
    public java.util.Set<Long> importedReportIds(Long userId) {
        java.util.Set<Long> ids = new java.util.HashSet<>();
        if (userId == null) {
            return ids;
        }
        List<GrowPlan> plans = growPlanMapper.selectList(new QueryWrapper<GrowPlan>()
                .select("report_id").eq("user_id", userId).isNotNull("report_id"));
        for (GrowPlan p : plans) {
            if (p.getReportId() != null) {
                ids.add(p.getReportId());
            }
        }
        return ids;
    }

    @Override
    public List<Map<String, Object>> getPlansWithTasks(Long userId) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (userId == null) {
            return result;
        }
        List<GrowPlan> plans = growPlanMapper.selectList(
                new QueryWrapper<GrowPlan>().eq("user_id", userId).orderByAsc("plan_type"));
        if (plans.isEmpty()) {
            return result;
        }
        List<Long> planIds = new ArrayList<>();
        for (GrowPlan p : plans) {
            planIds.add(p.getId());
        }
        List<GrowTask> tasks = growTaskMapper.selectList(
                new QueryWrapper<GrowTask>().in("plan_id", planIds).orderByAsc("id"));
        // 完成情况记录（时间线）：按 task_id 分组，时间升序
        Map<Long, List<GrowTaskRecord>> recMap = new HashMap<>();
        List<Long> taskIds = new ArrayList<>();
        for (GrowTask t : tasks) {
            taskIds.add(t.getId());
        }
        if (!taskIds.isEmpty()) {
            List<GrowTaskRecord> recs = growTaskRecordMapper.selectList(new QueryWrapper<GrowTaskRecord>()
                    .in("task_id", taskIds).orderByAsc("record_time").orderByAsc("id"));
            for (GrowTaskRecord r : recs) {
                recMap.computeIfAbsent(r.getTaskId(), k -> new ArrayList<>()).add(r);
            }
        }
        for (GrowPlan p : plans) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("plan", p);
            List<Map<String, Object>> own = new ArrayList<>();
            for (GrowTask t : tasks) {
                if (!p.getId().equals(t.getPlanId())) {
                    continue;
                }
                Map<String, Object> tm = new LinkedHashMap<>();
                tm.put("task", t);
                tm.put("records", recMap.getOrDefault(t.getId(), new ArrayList<>()));
                own.add(tm);
            }
            m.put("tasks", own);
            result.add(m);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addTask(Long userId, Map<String, Object> body) {
        if (userId == null) {
            throw new IllegalArgumentException("缺少用户ID");
        }
        Long planId = parseLong(body == null ? null : body.get("planId"));
        String taskName = str(body == null ? null : body.get("taskName"));
        if (planId == null || taskName.isBlank()) {
            throw new IllegalArgumentException("planId 与 taskName 不能为空");
        }
        GrowPlan plan = growPlanMapper.selectById(planId);
        if (plan == null || !userId.equals(plan.getUserId())) {
            throw new IllegalArgumentException("计划不存在或无权限");
        }
        LocalDateTime now = LocalDateTime.now();
        GrowTask t = new GrowTask();
        t.setId(SnowIdCreater.generateId(31));
        t.setPlanId(planId);
        t.setTaskName(defaultStr(body.get("taskName"), "待办任务"));
        t.setTaskType(parseInt(body.get("taskType"), 1));
        t.setTaskDesc(str(body.get("taskDesc")));
        t.setExpectedOutcome(str(body.get("expectedOutcome")));
        t.setTargetAbility(str(body.get("targetAbility")));
        t.setStartDate(now);
        t.setEndDate(parseDateOrNull(body.get("endDate"), plan.getEndDate()));
        t.setProgress(BigDecimal.ZERO);
        t.setStatus(0);
        t.setCreateTime(now);
        t.setUpdateTime(now);
        t.setIsDeleted(0);
        growTaskMapper.insert(t);
        recomputePlan(planId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("taskId", t.getId());
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addTaskRecord(Long userId, Long taskId, String content) {
        if (userId == null || taskId == null) {
            throw new IllegalArgumentException("参数不完整");
        }
        String c = content == null ? "" : content.trim();
        if (c.isEmpty()) {
            throw new IllegalArgumentException("记录内容不能为空");
        }
        GrowTask task = growTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        GrowPlan plan = growPlanMapper.selectById(task.getPlanId());
        if (plan == null || !userId.equals(plan.getUserId())) {
            throw new IllegalArgumentException("无权限");
        }
        GrowTaskRecord r = new GrowTaskRecord();
        r.setId(SnowIdCreater.generateId(31));
        r.setTaskId(taskId);
        r.setUserId(userId);
        r.setContent(c);
        r.setRecordTime(LocalDateTime.now());
        r.setIsDeleted(0);
        growTaskRecordMapper.insert(r);
        // 最后一次记录同步到 completion_detail（便于其它展示/导出）
        task.setCompletionDetail(c);
        task.setUpdateTime(LocalDateTime.now());
        growTaskMapper.updateById(task);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("recordId", r.getId());
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addPlan(Long userId, Map<String, Object> body) {
        if (userId == null) {
            throw new IllegalArgumentException("缺少用户ID");
        }
        int planType = parseInt(body == null ? null : body.get("planType"), 1);
        if (planType < 1 || planType > 3) {
            planType = 1;
        }
        String name = str(body == null ? null : body.get("planName"));
        if (name.isBlank()) {
            name = horizonName(planType) + "自定义目标";
        }
        int months = planType == 3 ? 5 : (planType == 2 ? 3 : 1);
        LocalDateTime now = LocalDateTime.now();
        GrowPlan plan = new GrowPlan();
        plan.setId(SnowIdCreater.generateId(30));
        plan.setUserId(userId);
        plan.setMatchId(null);
        plan.setReportId(null);
        plan.setTargetJob(defaultStr(body.get("targetJob"), "自定义"));
        plan.setPlanName(name.length() > 120 ? name.substring(0, 120) : name);
        plan.setPlanContent(str(body.get("planContent")));
        plan.setPlanType(planType);
        plan.setStartDate(now);
        plan.setEndDate(now.plusMonths(months));
        plan.setTotalStatus(0);
        plan.setProgress(BigDecimal.ZERO);
        plan.setCreateTime(now);
        plan.setUpdateTime(now);
        plan.setIsDeleted(0);
        growPlanMapper.insert(plan);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("planId", plan.getId());
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTask(Long userId, Long taskId) {
        if (userId == null || taskId == null) {
            return false;
        }
        GrowTask task = growTaskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        GrowPlan plan = growPlanMapper.selectById(task.getPlanId());
        if (plan == null || !userId.equals(plan.getUserId())) {
            return false;
        }
        growTaskRecordMapper.delete(new QueryWrapper<GrowTaskRecord>().eq("task_id", taskId));
        growTaskMapper.deleteById(taskId);
        recomputePlan(task.getPlanId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePlan(Long userId, Long planId) {
        if (userId == null || planId == null) {
            return false;
        }
        GrowPlan plan = growPlanMapper.selectById(planId);
        if (plan == null || !userId.equals(plan.getUserId())) {
            return false;
        }
        List<GrowTask> tasks = growTaskMapper.selectList(new QueryWrapper<GrowTask>().eq("plan_id", planId));
        for (GrowTask t : tasks) {
            growTaskRecordMapper.delete(new QueryWrapper<GrowTaskRecord>().eq("task_id", t.getId()));
        }
        growTaskMapper.delete(new QueryWrapper<GrowTask>().eq("plan_id", planId));
        growPlanMapper.deleteById(planId);
        return true;
    }

    private String horizonName(int planType) {
        return switch (planType) {
            case 2 -> "3 个月";
            case 3 -> "5 个月";
            default -> "1 个月";
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTaskStatus(Long taskId, Map<String, Object> patch) {
        if (taskId == null) {
            return false;
        }
        GrowTask task = growTaskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        if (patch != null) {
            if (patch.get("status") != null) {
                int status = Integer.parseInt(String.valueOf(patch.get("status")));
                task.setStatus(status);
                if (status == 2) {
                    task.setProgress(BigDecimal.valueOf(100));
                    if (task.getCompletionDate() == null) {
                        task.setCompletionDate(LocalDateTime.now());
                    }
                } else if (status == 0) {
                    task.setProgress(BigDecimal.ZERO);
                }
            }
            if (patch.get("progress") != null) {
                task.setProgress(new BigDecimal(String.valueOf(patch.get("progress"))));
            }
            if (patch.get("completionDetail") != null) {
                task.setCompletionDetail(String.valueOf(patch.get("completionDetail")));
            }
            if (patch.get("effectScore") != null) {
                task.setEffectScore(Integer.parseInt(String.valueOf(patch.get("effectScore"))));
            }
        }
        task.setUpdateTime(LocalDateTime.now());
        growTaskMapper.updateById(task);
        recomputePlan(task.getPlanId());
        return true;
    }

    /** 依据任务完成情况重算计划进度与状态 */
    private void recomputePlan(Long planId) {
        if (planId == null) {
            return;
        }
        GrowPlan plan = growPlanMapper.selectById(planId);
        if (plan == null) {
            return;
        }
        List<GrowTask> tasks = growTaskMapper.selectList(new QueryWrapper<GrowTask>().eq("plan_id", planId));
        if (tasks.isEmpty()) {
            return;
        }
        int done = 0;
        int inProgress = 0;
        for (GrowTask t : tasks) {
            int s = t.getStatus() == null ? 0 : t.getStatus();
            if (s == 2) {
                done++;
            } else if (s == 1 || s == 4) {
                inProgress++;
            }
        }
        BigDecimal progress = BigDecimal.valueOf(done * 100.0 / tasks.size()).setScale(2, RoundingMode.HALF_UP);
        plan.setProgress(progress);
        plan.setTotalStatus(done == tasks.size() ? 2 : (done > 0 || inProgress > 0 ? 1 : 0));
        plan.setUpdateTime(LocalDateTime.now());
        growPlanMapper.updateById(plan);
    }

    private int horizonToType(String horizon) {
        // 周期口径为「月」：兼容 1m/3m/5m、1/3/5，以及旧的 1y/3y/5y（同档位）
        return switch (horizon == null ? "" : horizon.trim().toLowerCase()) {
            case "3", "3m", "3y" -> 2;
            case "5", "5m", "5y" -> 3;
            default -> 1;
        };
    }

    private int horizonToMonths(String horizon) {
        // 周期口径已改为「月」：兼容 m/y 两种键，旧报告的 y 也按同档位处理
        return switch (horizon == null ? "" : horizon.trim().toLowerCase()) {
            case "3", "3m", "3y" -> 3;
            case "5", "5m", "5y" -> 5;
            default -> 1;
        };
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private Long parseLong(Object o) {
        try {
            return o == null ? null : Long.parseLong(String.valueOf(o).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private int parseInt(Object o, int def) {
        try {
            return o == null ? def : Integer.parseInt(String.valueOf(o).trim());
        } catch (Exception e) {
            return def;
        }
    }

    /** 解析 yyyy-MM-dd 日期；为空或非法时用默认值 */
    private LocalDateTime parseDateOrNull(Object o, LocalDateTime def) {
        String s = str(o);
        if (s.isEmpty()) {
            return def;
        }
        try {
            return java.time.LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
        } catch (Exception e) {
            return def;
        }
    }

    private String defaultStr(Object o, String def) {
        String s = str(o);
        return s.isEmpty() ? def : truncate(s, 120);
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    @SuppressWarnings("unchecked")
    private String joinList(Object o) {
        if (o instanceof List<?> list) {
            List<String> vals = new ArrayList<>();
            for (Object v : list) {
                String s = str(v);
                if (!s.isEmpty()) {
                    vals.add(s);
                }
            }
            return String.join(",", vals);
        }
        return str(o);
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }
}
