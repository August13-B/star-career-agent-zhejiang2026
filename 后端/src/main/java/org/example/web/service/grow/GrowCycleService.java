package org.example.web.service.grow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import org.example.web.config.FileUploadConfig;
import org.example.web.tool.JsonRepair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import org.example.web.entity.GrowPlan;
import org.example.web.entity.GrowTask;
import org.example.web.service.GrowPlanService;
import org.example.web.service.TboxAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 个人成长「下一周期」编排：**文件提交 → 完成 → 选择难度 → AI 生成下一周期计划**。
 *
 * <p>顺序固定：本地校验/落盘 → 读上一周期数据 → AI（事务外）→ 短事务写库。
 * 下一周期**新建一条 {@code grow_plan}**（{@code parent_plan_id} 指回上一周期），历史计划整体保留。
 */
@Service
public class GrowCycleService {

    private static final Logger logger = LoggerFactory.getLogger(GrowCycleService.class);

    /**
     * 下一个周期的长度（月）：沿 **1 → 3 → 5 个月** 递进。
     * 上一周期是 1 个月档 → 下一个周期 3 个月；3 个月档 → 5 个月；5 个月档 → 仍为 5 个月。
     */
    static int nextCycleMonths(GrowPlan parent) {
        int type = parent.getPlanType() == null ? 1 : parent.getPlanType();
        return switch (type) {
            case 1 -> 3;
            case 2 -> 5;
            default -> 5;
        };
    }

    /** 下一个周期对应的档位（1=1 个月 / 2=3 个月 / 3=5 个月），用于计划徽标展示。 */
    static int nextCyclePlanType(GrowPlan parent) {
        int type = parent.getPlanType() == null ? 1 : parent.getPlanType();
        return switch (type) {
            case 1 -> 2;
            case 2 -> 3;
            default -> 3;
        };
    }

    /** 难度 → effect_score（越高越轻松，沿用现有字段口径）。 */
    public static final Map<String, Integer> DIFFICULTY_SCORE = Map.of("太简单", 5, "中等", 3, "困难", 1);
    private static final Set<String> ALLOWED_EXT = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "md", "png", "jpg", "jpeg", "zip");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final GrowCycleWriter writer;
    private final GrowPlanService plans;
    private final TboxAgentService ai;
    private final FileUploadConfig uploadConfig;
    private final ObjectMapper json;
    private final WebClient.Builder webClientBuilder;
    /** 平台侧「下一周期计划」独立接口路径（未配置时回退通用对话接口）。 */
    @Value("${tbox.growth-path:}")
    private String growthPath;
    /** 平台侧令牌：对方配置 GROWTH_API_TOKEN 后必须带（同时兼容 X-Growth-Token 与 Authorization: Bearer）。 */
    @Value("${tbox.growth-token:}")
    private String growthToken;

    public GrowCycleService(GrowCycleWriter writer, GrowPlanService plans, TboxAgentService ai,
                            FileUploadConfig uploadConfig, ObjectMapper json,
                            WebClient.Builder webClientBuilder) {
        this.writer = writer;
        this.plans = plans;
        this.ai = ai;
        this.uploadConfig = uploadConfig;
        this.json = json;
        this.webClientBuilder = webClientBuilder;
    }

    // ── ① 提交任务文件（提交即完成）──────────────────────────────────

    public Map<String, Object> submitResource(Long userId, Long taskId, MultipartFile file) {
        String original = file == null || file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要提交的文件");
        }
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("暂不支持该文件类型，请提交 PDF / 图片 / Word / PPT / Excel / 压缩包");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("文件过大（超过 10MB），请压缩后再提交");
        }
        writer.requireTask(userId, taskId);
        String stored = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        Path dir = Path.of(uploadConfig.getUploadDir(), "grow");
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(stored).toFile());
        } catch (Exception e) {
            throw new IllegalArgumentException("文件保存失败，请重试");
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("fileName", original);
        meta.put("storedName", stored);
        meta.put("size", file.getSize());
        meta.put("ext", ext);
        meta.put("uploadedAt", java.time.LocalDateTime.now().toString());
        return writer.attachResource(userId, taskId, meta);
    }

    // ── ② 本次完成难度 ────────────────────────────────────────────────

    public Map<String, Object> chooseDifficulty(Long userId, Long taskId, String difficulty) {
        String label = difficulty == null ? "" : difficulty.strip();
        Integer score = DIFFICULTY_SCORE.get(label);
        if (score == null) {
            throw new IllegalArgumentException("请选择本次完成难度：太简单 / 中等 / 困难");
        }
        return writer.setDifficulty(userId, taskId, label, score);
    }

    // ── ③ 生成下一周期计划 ────────────────────────────────────────────

    /** force=false 时若已生成过下一周期，直接返回已有计划（幂等，避免重复消耗）。 */
    public Map<String, Object> generateNextCycle(Long userId, Long planId, Long taskId, String difficulty, boolean force) {
        GrowPlan parent = findPlan(userId, planId);
        if (!force) {
            Optional<Map<String, Object>> existing = writer.existingNextCycle(userId, planId);
            if (existing.isPresent()) {
                return result("exists", existing.get(), "这个计划已经生成过下一周期了；如确需重做，请点「重新生成」");
            }
        }
        if (difficulty != null && !difficulty.isBlank() && taskId != null) {
            chooseDifficulty(userId, taskId, difficulty);
        }
        int cycleMonths = nextCycleMonths(parent);
        int nextPlanType = nextCyclePlanType(parent);
        String answer = callPlatform(userId, parent, difficulty, cycleMonths);
        logger.info("【成长下一周期】平台返回长度={}，前 200 字：{}", answer == null ? 0 : answer.length(),
                answer == null ? "null" : answer.substring(0, Math.min(200, answer.length())));
        JsonNode node = parse(answer);
        String planName = node.path("planName").asText("").strip();
        JsonNode taskNodes = node.path("tasks");
        // 平台侧不做条数硬校验（见其 growth-service 说明），这里只要求"有任务"，超出上限按 8 条截断
        if (planName.isEmpty() || !taskNodes.isArray() || taskNodes.isEmpty()) {
            logger.warn("【成长下一周期】模型输出不符合契约：{}", answer);
            throw new IllegalArgumentException("这次没能生成有效的下一周期计划，请稍后重试");
        }
        List<Map<String, Object>> tasks = new ArrayList<>();
        int taskLimit = 0;
        for (JsonNode task : taskNodes) {
            if (taskLimit++ >= 8) {
                break;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("taskName", task.path("taskName").asText("").strip());
            item.put("taskType", taskTypeOf(task.path("taskType")));
            item.put("taskDesc", task.path("taskDesc").asText(""));
            item.put("expectedOutcome", task.path("expectedOutcome").asText(""));
            item.put("targetAbility", task.path("targetAbility").asText(""));
            item.put("resource", json.convertValue(task.path("resource"), Object.class));
            item.put("startDate", task.path("startDate").asText(""));
            item.put("endDate", task.path("endDate").asText(""));
            if (!String.valueOf(item.get("taskName")).isBlank()) {
                tasks.add(item);
            }
        }
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("这次没能生成有效的任务清单，请稍后重试");
        }
        LocalDate start = date(node.path("startDate").asText(""));
        LocalDate end = date(node.path("endDate").asText(""));
        Long newPlanId = writer.createNextCycle(userId, parent, planName,
                node.path("planContent").asText("").strip(), start, end, cycleMonths, nextPlanType, tasks);
        GrowPlan created = findPlan(userId, newPlanId);
        return result("created", created, "已生成下一周期计划：" + planName);
    }

    // ── 内部 ────────────────────────────────────────────────────────

    /** 按 id 在「当前用户的计划列表」里找出实体（getPlansWithTasks 返回的是实体对象，不是 Map）。 */
    private GrowPlan findPlan(Long userId, Long planId) {
        for (Map<String, Object> item : plans.getPlansWithTasks(userId)) {
            if (item.get("plan") instanceof GrowPlan plan && plan.getId() != null && plan.getId().equals(planId)) {
                return plan;
            }
        }
        throw new IllegalArgumentException("成长计划不存在或无权访问");
    }

    /** 交给 AI 的材料 + 输出契约（上一周期的任务完成情况、难度自评）。 */
    private String prompt(Long userId, GrowPlan parent, String difficulty) {
        StringBuilder material = new StringBuilder();
        for (Map<String, Object> item : plans.getPlansWithTasks(userId)) {
            if (!(item.get("plan") instanceof GrowPlan plan) || !plan.getId().equals(parent.getId())) {
                continue;
            }
            Object holderList = item.get("tasks");
            if (holderList instanceof List<?> list) {
                int index = 1;
                for (Object one : list) {
                    if (!(one instanceof Map<?, ?> holder) || !(holder.get("task") instanceof GrowTask task)) {
                        continue;
                    }
                    material.append(index++).append(". ").append(text(task.getTaskName()));
                    material.append(" | 状态=").append(statusText(task.getStatus() == null ? 0 : task.getStatus()));
                    material.append(" | 进度=").append(task.getProgress() == null ? 0 : task.getProgress()).append('%');
                    if (task.getEffectScore() != null) {
                        material.append(" | 难度自评=").append(difficultyLabel(task.getEffectScore()));
                    }
                    if (!text(task.getCompletionDetail()).isEmpty()) {
                        material.append(" | 完成说明=").append(text(task.getCompletionDetail()));
                    }
                    material.append('\n');
                }
            }
            return INSTRUCTION
                    + "【上一周期计划】" + text(plan.getPlanName()) + "（周期 " + dateText(plan.getStartDate())
                    + " ~ " + dateText(plan.getEndDate()) + "，目标岗位 " + text(plan.getTargetJob()) + "）\n"
                    + "计划总体思路：" + text(plan.getPlanContent()) + "\n"
                    + "【任务与完成情况】\n" + material + "\n"
                    + "【本次完成难度自评】" + (difficulty == null || difficulty.isBlank() ? "未选择" : difficulty) + "\n";
        }
        return INSTRUCTION + "【上一周期计划】" + text(parent.getPlanName()) + "\n";
    }

    /** taskType 兜底：数字直接用；中文（技能/证书/项目/实习）映射为 1~4；其它一律 1。 */
    static int taskTypeOf(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return 1;
        }
        if (value.isNumber()) {
            return Math.max(1, Math.min(4, value.asInt()));
        }
        String text = value.asText("").strip();
        if (text.matches("[1-4]")) {
            return Integer.parseInt(text);
        }
        if (text.contains("技能")) return 1;
        if (text.contains("证书")) return 2;
        if (text.contains("项目")) return 3;
        if (text.contains("实习") || text.contains("工作")) return 4;
        return 1;
    }

    private static String dateText(java.time.LocalDateTime value) {
        return value == null ? "未设置" : value.toLocalDate().toString();
    }

    /** 给模型的固定指令（输出契约与难度含义）。 */
    private static final String INSTRUCTION = """
            你在帮一名学生做「成长计划的下一个周期」。根据他上一周期的计划、任务完成情况与本次难度自评，
            安排**具体、可验收**的下一周期任务。
            难度自评的含义：太简单 = 可加量/提速/提高目标；中等 = 保持节奏、在原有基础上小幅加难；
            困难 = 把任务拆得更小、降低单步难度、适当延长周期。
            只依据给定材料安排，不得编造他已有的成果；不要输出与上一周期完全重复的任务。
            输出纯 JSON（不要解释、不要 markdown 代码块）：
            {"planName":"下一周期计划名称","planContent":"总体思路（3~5 句）",
             "startDate":"YYYY-MM-DD","endDate":"YYYY-MM-DD",
             "tasks":[{"taskName":"","taskType":1,"taskDesc":"","expectedOutcome":"",
             "targetAbility":"","resource":["资料或链接"],"startDate":"YYYY-MM-DD","endDate":"YYYY-MM-DD"}]}
            taskType：1技能 2证书 3项目 4实习。任务 3~6 条；整体周期 4~12 周（上一周期更长时可延长）；
            日期必须落在下一周期内且 startDate ≤ endDate。
            """;

    /** 调用平台：配置了独立接口就走接口，否则回退通用对话接口。 */
    private String callPlatform(Long userId, GrowPlan parent, String difficulty, int cycleMonths) {
        String request = buildRequest(parent, difficulty, cycleMonths);
        if (growthPath != null && !growthPath.isBlank()) {
            try {
                var spec = webClientBuilder.build().post().uri(growthPath)
                        .header("Content-Type", "application/json");
                if (growthToken != null && !growthToken.isBlank()) {
                    // 对方两种头任选其一，这里都带上，确保鉴权通过
                    spec = spec.header("X-Growth-Token", growthToken)
                               .header("Authorization", "Bearer " + growthToken);
                }
                return spec.bodyValue(request)
                        .retrieve().bodyToMono(String.class)
                        .block(java.time.Duration.ofSeconds(180));
            } catch (Exception e) {
                logger.warn("【成长下一周期】独立接口调用失败，回退通用对话接口：{}", e.getMessage());
            }
        }
        return ai.chatSync(userId, null, prompt(userId, parent, difficulty));
    }

    /** 独立接口的请求体（契约见 百宝箱/提示词-成长下一周期计划接口.md）。 */
    private String buildRequest(GrowPlan parent, String difficulty, int cycleMonths) {
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("planName", parent.getPlanName());
        plan.put("targetJob", parent.getTargetJob());
        plan.put("planType", parent.getPlanType());
        plan.put("startDate", dateText(parent.getStartDate()));
        plan.put("endDate", dateText(parent.getEndDate()));
        plan.put("planContent", parent.getPlanContent());
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (Map<String, Object> item : plans.getPlansWithTasks(parent.getUserId())) {
            if (!(item.get("plan") instanceof GrowPlan one) || !one.getId().equals(parent.getId())) {
                continue;
            }
            if (item.get("tasks") instanceof List<?> list) {
                for (Object holder : list) {
                    if (!(holder instanceof Map<?, ?> map) || !(map.get("task") instanceof GrowTask task)) {
                        continue;
                    }
                    Map<String, Object> t = new LinkedHashMap<>();
                    t.put("taskName", task.getTaskName());
                    t.put("taskType", task.getTaskType());
                    t.put("status", statusText(task.getStatus() == null ? 0 : task.getStatus()));
                    t.put("progress", task.getProgress());
                    t.put("difficultySelfRating", task.getEffectScore() == null ? null : difficultyLabel(task.getEffectScore()));
                    t.put("completionDetail", task.getCompletionDetail());
                    // 把"这条任务原本要求达到什么"一并交给平台，否则新计划容易泛泛而谈
                    t.put("expectedOutcome", task.getExpectedOutcome());
                    t.put("taskDesc", task.getTaskDesc());
                    t.put("targetAbility", task.getTargetAbility());
                    tasks.add(t);
                }
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mode", "next_cycle");
        body.put("difficulty", difficulty == null ? "" : difficulty);
        body.put("plan", plan);
        body.put("tasks", tasks);
        // 质量要求：平台系统提示词锁定 JSON 模板，这里把"别太简陋"的硬要求随材料一起交给模型
        body.put("cycleMonths", cycleMonths);
        body.put("requirements", java.util.List.of(
                "本周期长度固定为 " + cycleMonths + " 个月（约 " + Math.round(cycleMonths * 4.35) + " 周）：startDate 从上一周期结束之后顺延，endDate = startDate + " + cycleMonths + " 个月 - 1 天",
                "上一周期已完成且达标的任务不要重复安排",
                "每条任务必须写明：怎么做（≥2 句，含工具/频率/投入时长）、可验收的产出（有数量或有地址，避免了解/熟悉这类说法）",
                "整体周期 4~12 周，任务 3~6 条，任务之间不要重叠",
                "难度为太简单时提高目标与产出量；困难时拆小步并适当延长周期"));
        try {
            return json.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalArgumentException("下一周期请求体组装失败");
        }
    }

    /**
     * 解析平台返回：兼容「独立接口」与「通用对话」两种形态，并清理常见脏数据。
     * <ol>
     *   <li>去掉 ```json 围栏与前后解释文字；</li>
     *   <li>解包 {"response":"<JSON字符串>"} / {"data":"…"} / {"content":"…"} 包裹；</li>
     *   <li>截取最外层 { … } 后用 JsonRepair 兜底（裸引号、末尾多括号等）；</li>
     *   <li>若返回带 next_cycle_plan 外壳则取出内层。</li>
     * </ol>
     */
    static JsonNode parseAnswer(ObjectMapper json, String answer) {
        if (answer == null || answer.indexOf('{') < 0) {
            throw new IllegalArgumentException("AI 未返回有效的下一周期计划，请稍后重试");
        }
        String text = stripFence(answer).strip();

        // 解包一层 {"response":"<json>"} / {"data":"…"} / {"content":"…"}
        try {
            JsonNode outer = json.readTree(cut(text));
            for (String key : List.of("response", "data", "content", "result", "answer")) {
                if (outer.path(key).isTextual() && outer.path(key).asText().indexOf('{') >= 0) {
                    text = stripFence(outer.path(key).asText()).strip();
                    break;
                }
            }
        } catch (Exception ignored) {
            // 外层解析失败则按原文继续
        }

        JsonNode node = readWithRepair(json, text);
        if (node.path("next_cycle_plan").isObject()) {
            return node.path("next_cycle_plan");
        }
        if (node.path("plan").isObject() && node.path("tasks").isEmpty()) {
            return node.path("plan");
        }
        return node;
    }

    /** 截取最外层花括号之间的内容。 */
    private static String cut(String text) {
        int from = text.indexOf('{');
        int to = text.lastIndexOf('}');
        return from < 0 || to <= from ? text : text.substring(from, to + 1);
    }

    private static String stripFence(String text) {
        return text.replace("```json", "```").replaceAll("(?s)```[a-zA-Z]*\s*", "").replace("```", "");
    }

    /** 直接解析，失败后用 JsonRepair 修复再试（平台常见裸引号 / 尾随括号）。 */
    private static JsonNode readWithRepair(ObjectMapper json, String text) {
        String body = cut(text);
        try {
            return json.readTree(body);
        } catch (Exception first) {
            try {
                String repaired = JsonRepair.repairUnescapedQuotes(body);
                return json.readTree(repaired);
            } catch (Exception second) {
                throw new IllegalArgumentException("下一周期计划格式异常，请重试一次");
            }
        }
    }

    private JsonNode parse(String answer) {
        return parseAnswer(json, answer);
    }

    private static Map<String, Object> result(String status, Object plan, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", status);
        result.put("plan", plan);
        result.put("message", message);
        return result;
    }

    static String difficultyLabel(int score) {
        return DIFFICULTY_SCORE.entrySet().stream()
                .filter(entry -> entry.getValue() == score).map(Map.Entry::getKey).findFirst().orElse("未选择");
    }

    private static String statusText(int status) {
        return switch (status) {
            case 1 -> "进行中";
            case 2 -> "已完成";
            case 3 -> "已暂停";
            case 4 -> "已延期";
            default -> "未开始";
        };
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).strip();
    }

    private static LocalDate date(String value) {
        try {
            return value == null || value.isBlank() ? null : LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value);
        } catch (Exception e) {
            return null;
        }
    }

    private static long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? 0L : Long.parseLong(String.valueOf(value).strip());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
