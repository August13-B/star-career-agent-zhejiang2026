package org.example.web.service.grow;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.example.web.entity.GrowPlan;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 成长计划「下一周期 / 任务文件提交」的落库（本地短事务；AI 调用在调用方完成）。
 *
 * <p>字段复用：
 * <ul>
 *   <li>任务提交的文件元数据 → {@code grow_task.grow_recourse}（JSON 数组）</li>
 *   <li>本次完成难度 → {@code grow_task.effect_score}（太简单=5 / 中等=3 / 困难=1）+ {@code adjustment_reason}</li>
 *   <li>下一周期计划 → 新建 {@code grow_plan}（{@code parent_plan_id} 指回上一周期，{@code cycle_round}+1）</li>
 * </ul>
 */
@Service
public class GrowCycleWriter {

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    /** 迁移 016 新增列是否存在（探测一次并缓存）：有就用，没有就自动降级，避免整功能依赖迁移。 */
    private final Map<String, Boolean> columnCache = new java.util.concurrent.ConcurrentHashMap<>();

    public GrowCycleWriter(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** 列是否存在（information_schema 探测，结果缓存）。 */
    boolean hasColumn(String table, String column) {
        return columnCache.computeIfAbsent(table + "." + column, key -> {
            try {
                Integer count = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() "
                                + "AND table_name=? AND column_name=?", Integer.class, table, column);
                return count != null && count > 0;
            } catch (Exception e) {
                return false;
            }
        });
    }

    /** 任务必须属于该用户。 */
    public Map<String, Object> requireTask(Long userId, Long taskId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT t.* FROM grow_task t JOIN grow_plan p ON p.id = t.plan_id "
                        + "WHERE t.id=? AND t.is_deleted=0 AND p.is_deleted=0 AND p.user_id=?", taskId, userId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("任务不存在或无权访问");
        }
        return rows.get(0);
    }

    /** 追加一份提交文件并把任务标记为已完成（提交即完成），同时重算计划进度。 */
    @Transactional
    public Map<String, Object> attachResource(Long userId, Long taskId, Map<String, Object> meta) {
        Map<String, Object> task = requireTask(userId, taskId);
        if (hasColumn("grow_task", "grow_recourse")) {
            List<Map<String, Object>> files = readJsonArray((String) task.get("grow_recourse"));
            files.add(meta);
            jdbc.update("UPDATE grow_task SET grow_recourse=?, status=2, progress=100, "
                            + "completion_date=COALESCE(completion_date, CURRENT_TIMESTAMP), "
                            + "completion_detail=CASE WHEN completion_detail IS NULL OR completion_detail='' THEN ? ELSE completion_detail END "
                            + "WHERE id=?",
                    write(files), "已提交任务文件：" + meta.get("fileName"), taskId);
        } else {
            // 历史库没有该列：只写完成说明（前端从完成说明里读文件名，展示不受影响）
            jdbc.update("UPDATE grow_task SET status=2, progress=100, "
                            + "completion_date=COALESCE(completion_date, CURRENT_TIMESTAMP), "
                            + "completion_detail=? WHERE id=?",
                    "已提交任务文件：" + meta.get("fileName"), taskId);
        }
        recalcPlan(number(task.get("plan_id")));
        return jdbc.queryForMap("SELECT * FROM grow_task WHERE id=?", taskId);
    }

    /**
     * 记录本次完成难度。
     *
     * @param label 太简单 / 中等 / 困难
     * @param score 太简单=5、中等=3、困难=1（越高越轻松，沿用 effect_score 口径）
     */
    @Transactional
    public Map<String, Object> setDifficulty(Long userId, Long taskId, String label, int score) {
        requireTask(userId, taskId);
        jdbc.update("UPDATE grow_task SET effect_score=?, adjustment_reason=? WHERE id=?", score, "本次完成难度：" + label, taskId);
        return jdbc.queryForMap("SELECT * FROM grow_task WHERE id=?", taskId);
    }

    /** 已生成的下一周期计划（幂等判断用）。 */
    public Optional<Map<String, Object>> existingNextCycle(Long userId, Long parentPlanId) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT * FROM grow_plan WHERE user_id=? AND parent_plan_id=? AND is_deleted=0 "
                            + "ORDER BY cycle_round DESC, id DESC LIMIT 1", userId, parentPlanId);
            return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
        } catch (Exception e) {
            // 迁移 016 未执行时 parent_plan_id 列不存在：当作"没有下一周期"，
            // 由后续 INSERT 给出明确提示，而不是把成长页其它接口一起带崩
            return Optional.empty();
        }
    }

    /**
     * 新建「下一周期」计划及其任务。
     *
     * @param tasks 每个元素：taskName / taskType(1技能 2证书 3项目 4实习) / taskDesc / expectedOutcome /
     *              targetAbility / resource / startDate / endDate
     */
    @Transactional
    public Long createNextCycle(Long userId, GrowPlan parent, String planName, String planContent,
                                LocalDate start, LocalDate end, int cycleMonths, int planType,
                                List<Map<String, Object>> tasks) {
        long planId = org.example.web.tool.SnowIdCreater.generateId(17);
        // 实体没有 cycle_round 字段（迁移 016 新增的列），这里直接查库取上一轮轮次
        Integer parentRound = null;
        try {
            parentRound = jdbc.queryForObject(
                    "SELECT COALESCE(cycle_round,1) FROM grow_plan WHERE id=?", Integer.class, parent.getId());
        } catch (Exception ignored) {
            // 迁移 016 未执行时该列不存在：按首轮处理
        }
        int round = parentRound == null || parentRound <= 0 ? 2 : parentRound + 1;
        // grow_plan.target_job / plan_name / plan_content 都是 NOT NULL：自己创建的计划可能没填目标岗位，
        // 计划思路也可能为空 → 这里做兜底，避免整个生成因"缺字段"失败
        String targetJob = text(parent.getTargetJob());
        if (targetJob == null || targetJob.isEmpty()) {
            targetJob = "未指定";
        }
        targetJob = fit(targetJob, 64);
        String name = fit(text(planName), 128);
        String content = text(planContent);
        if (content == null || content.isEmpty()) {
            content = name;
        }
        // 周期口径：一轮长度沿 1→3→5 个月递进（由调用方按上一周期档位算出 cycleMonths）；
        // 新周期从上一周期结束后**往后推**，而不是另起"第二阶段"
        LocalDate pushStart = forwardStart(parent);
        if (start == null || start.isBefore(pushStart)) {
            start = pushStart;
        }
        int windowMonths = cycleMonths <= 0 ? 2 : cycleMonths;
        LocalDate pushEnd = start.plusMonths(windowMonths).minusDays(1);
        if (end == null || end.isBefore(start)) {
            end = pushEnd;
        }
        boolean hasCycleColumns = hasColumn("grow_plan", "parent_plan_id") && hasColumn("grow_plan", "cycle_round");
        int nextPlanType = planType <= 0 ? 1 : planType;
        Object planTypeValue = nextPlanType;
        String startText = start == null ? null : start.toString();
        String endText = end == null ? null : end.toString();
        if (hasCycleColumns) {
            jdbc.update("INSERT INTO grow_plan(id,user_id,report_id,target_job,plan_name,plan_content,plan_type,"
                            + "start_date,end_date,total_status,progress,parent_plan_id,cycle_round) "
                            + "VALUES(?,?,?,?,?,?,?,?,?,0,0,?,?)",
                    planId, userId, parent.getReportId(), targetJob, name, content, planTypeValue, startText, endText,
                    parent.getId(), round);
        } else {
            // 历史库未跑迁移 016：不带新列插入（来源关系改由任务的 adjustment_reason 记录）
            jdbc.update("INSERT INTO grow_plan(id,user_id,report_id,target_job,plan_name,plan_content,plan_type,"
                            + "start_date,end_date,total_status,progress) VALUES(?,?,?,?,?,?,?,?,?,0,0)",
                    planId, userId, parent.getReportId(), targetJob, name, content, planTypeValue, startText, endText);
        }
        int order = 1;
        for (Map<String, Object> task : tasks) {
            jdbc.update("INSERT INTO grow_task(id,plan_id,task_name,task_type,task_desc,target_ability,expected_outcome,"
                            + "resource,start_date,end_date,progress,status,adjustment_reason) VALUES(?,?,?,?,?,?,?,?,?,?,0,0,?)",
                    org.example.web.tool.SnowIdCreater.generateId(18), planId,
                    fit(text(task.get("taskName")), 255), clamp(number(task.get("taskType")), 1, 4, 1),
                    text(task.get("taskDesc")), text(task.get("targetAbility")), text(task.get("expectedOutcome")),
                    resource(task.get("resource")),
                    clampDate(dateText(task.get("startDate"), start), start, end, true),
                    clampDate(dateText(task.get("endDate"), end), start, end, false),
                    "由上一周期计划生成（parent=" + (parent.getId() == null ? "?" : parent.getId()) + "）");
            order++;
        }
        return planId;
    }

    /** 任务资源统一存 JSON 文本（AI 可能给字符串或数组）。 */
    private String resource(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text.isBlank() ? null : write(List.of(text));
        }
        return write(value);
    }

    /** 计划进度 = 已完成任务数 / 任务总数（与前端展示口径一致）。 */
    private void recalcPlan(long planId) {
        if (planId <= 0) {
            return;
        }
        jdbc.update("UPDATE grow_plan SET progress=(SELECT IFNULL(ROUND(100*SUM(t.status=2)/NULLIF(COUNT(*),0),2),0) "
                + "FROM grow_task t WHERE t.plan_id=? AND t.is_deleted=0) WHERE id=?", planId, planId);
    }

    private List<Map<String, Object>> readJsonArray(String value) {
        List<Map<String, Object>> files = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return files;
        }
        try {
            var node = json.readTree(value);
            if (node.isArray()) {
                for (var item : node) {
                    if (item.isObject()) {
                        files.add(json.convertValue(item, Map.class));
                    }
                }
            }
        } catch (Exception ignored) {
            // 历史内容不可解析时视为空，不阻断新提交
        }
        return files;
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("任务资源保存失败，请重试");
        }
    }

    /**
     * 把任务日期收进本轮窗口（[start, end]）：
     * 早于起点取下限、晚于终点取上限；顺带保证任务 start ≤ end。
     */
    private static String clampDate(String value, LocalDate start, LocalDate end, boolean isStart) {
        LocalDate date;
        try {
            date = value == null || value.isBlank() ? null : LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value);
        } catch (Exception e) {
            date = null;
        }
        if (date == null) {
            date = isStart ? start : end;
        }
        if (date == null) {
            return null;
        }
        if (start != null && date.isBefore(start)) {
            date = start;
        }
        if (end != null && date.isAfter(end)) {
            date = end;
        }
        return date.toString();
    }

    /** 下一周期起点：上一周期结束日的次日；上一周期没有结束日则用今天。 */
    private static LocalDate forwardStart(GrowPlan parent) {
        LocalDate base = parent.getEndDate() == null ? LocalDate.now() : parent.getEndDate().toLocalDate();
        LocalDate start = base.plusDays(1);
        LocalDate today = LocalDate.now();
        return start.isBefore(today) ? today : start;
    }

    /** 按数据库列宽截断（平台侧不做长度校验，落库前由我们兜底）。 */
    private static String fit(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value).strip();
    }

    private static String dateText(Object value, LocalDate fallback) {
        String text = text(value);
        if (text != null && !text.isEmpty()) {
            return text.length() > 10 ? text.substring(0, 10) : text;
        }
        return fallback == null ? null : fallback.toString();
    }

    private static int number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value).strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int clamp(int value, int min, int max, int fallback) {
        if (value < min || value > max) {
            return fallback;
        }
        return value;
    }
}
