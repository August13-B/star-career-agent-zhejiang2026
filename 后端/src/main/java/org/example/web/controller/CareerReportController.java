package org.example.web.controller;

import java.util.List;
import java.util.Map;

import org.example.web.entity.CareerReport;
import org.example.web.entity.CareerReportHistory;
import org.example.web.entity.Result;
import org.example.web.service.CareerReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 职业报告控制器
 * 提供职业报告相关的RESTful API接口
 * 注意：实际报告内容生成由AI服务器完成，本控制器主要负责接收请求和返回结果
 * 包括报告创建、查询、更新、删除、分享等功能
 * 
 * @author 系统生成
 * @version 1.0
 */
@RestController
@RequestMapping("/career-report")
public class CareerReportController {

    @Autowired
    private org.example.web.service.TboxAgentService tboxAgentService;

    @Autowired
    private org.example.web.mapper.CareerReportMapper careerReportMapper;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private org.example.web.mapper.CareerReportHistoryMapper careerReportHistoryMapper;

    @Autowired
    private org.example.web.service.impl.StudentProfileContextService studentProfileContextService;

    @Autowired
    private org.example.web.service.impl.CareerReportPdfService careerReportPdfService;

    /** 智能体 key → 中文名（用于落库与前端展示） */
    private static final java.util.Map<String, String> AGENT_NAMES = java.util.Map.of(
            "profile_analysis", "画像分析",
            "career_exploration", "职业探索",
            "goal_setting", "目标设定",
            "path_planning", "路径规划",
            "action_planning", "行动计划",
            "report_composition", "报告整合");

    /**
     * ① 启动职业报告异步任务（秒回）。
     *
     * <p>后端：拼「画像上下文 + 用户本次诉求」→ 平台 {@code POST /api/report} → 返回 jobId。
     * 任务在平台侧独立运行，前端拿 jobId 自行轮询；刷新页面也不会丢。
     */
    @org.springframework.web.bind.annotation.PostMapping("/start")
    @org.springframework.web.bind.annotation.CrossOrigin
    public Result<?> startReport(@RequestBody java.util.Map<String, Object> request) {
        Object uidRaw = request.get("user_id");
        if (uidRaw == null) {
            return Result.error("缺少 user_id 参数");
        }
        Long userId;
        try {
            userId = Long.parseLong(String.valueOf(uidRaw));
        } catch (NumberFormatException e) {
            return Result.error("user_id 格式错误");
        }
        String userInput = request.get("content") == null ? "" : String.valueOf(request.get("content"));
        String targetJob = request.get("target_job") == null ? null : String.valueOf(request.get("target_job"));
        String message = studentProfileContextService.build(userId, targetJob, userInput);
        try {
            String jobId = tboxAgentService.startReportJob(userId, message);
            java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("jobId", jobId);
            data.put("userId", String.valueOf(userId));
            return Result.success("报告任务已创建", data);
        } catch (Exception e) {
            System.err.println("创建报告任务失败: " + e.getMessage());
            return Result.error("创建报告任务失败：" + e.getMessage());
        }
    }

    /** ② 查询报告任务进度/结果（前端每 1.5s 轮询；带 offsets 拿正文增量；done 时幂等落库） */
    @org.springframework.web.bind.annotation.GetMapping("/jobs/{jobId}")
    @org.springframework.web.bind.annotation.CrossOrigin
    public Result<?> reportJobStatus(@org.springframework.web.bind.annotation.PathVariable String jobId,
                                     @org.springframework.web.bind.annotation.RequestParam(value = "offsets", required = false) String offsets,
                                     @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String token) {
        try {
            String json = tboxAgentService.fetchReportJob(jobId, offsets);
            com.fasterxml.jackson.databind.JsonNode n = objectMapper.readTree(json);
            String status = n.path("status").asText("");
            java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
            out.put("status", status);
            out.put("progressChars", n.path("progressChars").asInt(0));
            out.put("currentAgent", n.path("currentAgent").asText(""));
            out.put("agentsDone", objectMapper.convertValue(n.path("agentsDone"), java.util.List.class));
            // 增量正文：按 offsets 切片的 deltas（不重不漏） + 各段长度
            out.put("deltas", objectMapper.convertValue(n.path("deltas"), java.util.List.class));
            out.put("segmentChars", objectMapper.convertValue(n.path("segmentChars"), java.util.Map.class));

            if ("done".equalsIgnoreCase(status)) {
                Long reportId = saveReportOnce(jobId, n, currentUserId(token));
                out.put("platformReportId", n.path("reportId").asText(null));
                out.put("reportName", n.path("reportName").asText(null));
                out.put("content", objectMapper.convertValue(n.path("content"), java.util.Map.class));
                if (reportId != null) {
                    out.put("reportId", reportId);
                    out.put("saved", true);
                } else {
                    out.put("saved", false);
                }
            } else if ("error".equalsIgnoreCase(status) || n.has("error")) {
                out.put("error", n.path("error").asText(n.path("message").asText("报告任务失败")));
            }
            return Result.success(out);
        } catch (Exception e) {
            System.err.println("查询报告任务失败: " + e.getMessage());
            return Result.error("查询报告任务失败：" + e.getMessage());
        }
    }

    /** jobId → 我们 MySQL reportId 缓存（保证同一次任务只落库一次） */
    private final java.util.concurrent.ConcurrentHashMap<String, Long> savedReportByJob =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** 幂等落库：同一个 jobId 只写一次。userId 优先取登录态，避免平台回传不一致导致外键失败 */
    private Long saveReportOnce(String jobId, com.fasterxml.jackson.databind.JsonNode n, Long tokenUserId) {
        Long cached = savedReportByJob.get(jobId);
        if (cached != null) {
            return cached;
        }
        Long userId = tokenUserId;
        if (userId == null) {
            try {
                userId = Long.parseLong(n.path("userId").asText(""));
            } catch (Exception ignore) {
                userId = null;
            }
        }
        if (userId == null) {
            System.err.println("报告任务缺少 userId，跳过落库: jobId=" + jobId);
            return null;
        }
        java.util.Map<String, Object> info = saveReport(userId,
                n.path("content").path("agents"), n.path("reportName").asText(null),
                n.path("reportId").asText(null));
        if (info != null && info.get("reportId") != null) {
            Long id = Long.parseLong(String.valueOf(info.get("reportId")));
            savedReportByJob.put(jobId, id);
            return id;
        }
        return null;
    }

    /** ③ 取消报告任务（随时停止）：中止平台流水线，已生成内容丢弃、不落库 */
    @org.springframework.web.bind.annotation.PostMapping("/jobs/{jobId}/cancel")
    @org.springframework.web.bind.annotation.CrossOrigin
    public Result<?> cancelReportJob(@org.springframework.web.bind.annotation.PathVariable String jobId) {
        try {
            String resp = tboxAgentService.cancelReportJob(jobId);
            java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
            try {
                out.put("platform", objectMapper.convertValue(objectMapper.readTree(resp), java.util.Map.class));
            } catch (Exception ignore) {
                out.put("platform", resp);
            }
            return Result.success("已请求取消", out);
        } catch (Exception e) {
            System.err.println("取消报告任务失败: " + e.getMessage());
            return Result.error("取消失败：" + e.getMessage());
        }
    }

    /** ④ 批量删除报告：我们侧逻辑删除 + 平台侧物理删除（平台失败不阻塞） */
    @org.springframework.web.bind.annotation.PostMapping("/batch-delete")
    @org.springframework.web.bind.annotation.CrossOrigin
    public Result<?> batchDelete(@RequestBody java.util.Map<String, Object> request,
                                 @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String token) {
        Object idsRaw = request.get("ids");
        if (!(idsRaw instanceof java.util.List) || ((java.util.List<?>) idsRaw).isEmpty()) {
            return Result.error("ids 不能为空");
        }
        Long userId;
        try {
            java.util.Map<String, Object> claims = org.example.web.tool.JwtUtil.parseToken(token);
            userId = Long.parseLong(String.valueOf(claims.get("id")));
        } catch (Exception e) {
            return Result.error("登录状态无效");
        }
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (Object o : (java.util.List<?>) idsRaw) {
            try {
                ids.add(Long.parseLong(String.valueOf(o)));
            } catch (Exception ignore) {
            }
        }
        if (ids.isEmpty()) {
            return Result.error("ids 格式错误");
        }
        // 只允许删除当前用户自己的报告
        java.util.List<CareerReport> reports = careerReportMapper.selectByIds(ids);
        java.util.List<Long> ownIds = new java.util.ArrayList<>();
        java.util.List<String> platformIds = new java.util.ArrayList<>();
        for (CareerReport r : reports) {
            if (r.getUserId() != null && r.getUserId().equals(userId)) {
                ownIds.add(r.getId());
                if (r.getPlatformReportId() != null && !r.getPlatformReportId().isBlank()) {
                    platformIds.add(r.getPlatformReportId());
                }
            }
        }
        if (ownIds.isEmpty()) {
            return Result.error("没有可删除的报告");
        }
        int deleted = careerReportMapper.logicDeleteByIds(ownIds);

        int platformDeleted = 0;
        java.util.List<String> platformFailed = new java.util.ArrayList<>();
        if (!platformIds.isEmpty()) {
            try {
                String resp = tboxAgentService.deleteReports(platformIds, userId);
                com.fasterxml.jackson.databind.JsonNode n = objectMapper.readTree(resp);
                platformDeleted = n.path("deleted").size();
                for (com.fasterxml.jackson.databind.JsonNode f : n.path("failed")) {
                    platformFailed.add(f.path("reportId").asText(""));
                }
            } catch (Exception e) {
                System.err.println("平台删除报告失败（不阻塞我们侧删除）: " + e.getMessage());
                platformFailed.addAll(platformIds);
            }
        }
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("deleted", deleted);
        out.put("platformRequested", platformIds.size());
        out.put("platformDeleted", platformDeleted);
        out.put("platformFailed", platformFailed);
        // 历史报告（无 platform_report_id）：仅在本地逻辑删除
        out.put("platformSkipped", ownIds.size() - platformIds.size());
        return Result.success("删除完成", out);
    }

    /**
     * 导出报告为 PDF（服务端生成，PDFBox + 系统中文字体）
     */
    @org.springframework.web.bind.annotation.GetMapping("/{id}/export/pdf")
    @org.springframework.web.bind.annotation.CrossOrigin
    public org.springframework.http.ResponseEntity<byte[]> exportReportPdf(
            @org.springframework.web.bind.annotation.PathVariable Long id) {
        CareerReport report = careerReportMapper.selectById(id);
        if (report == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        java.util.List<java.util.Map<String, String>> agents = new java.util.ArrayList<>();
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(report.getReportContent());
            String finalText = root.path("final").asText("");
            if (!finalText.isBlank()) {
                // 最终报告 = 第 6 段整合后的简介
                java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
                m.put("name", report.getReportName());
                m.put("content", finalText);
                agents.add(m);
            } else {
                for (com.fasterxml.jackson.databind.JsonNode a : root.path("agents")) {
                    java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
                    m.put("name", a.path("name").asText("报告章节"));
                    m.put("content", a.path("content").asText(""));
                    agents.add(m);
                }
            }
        } catch (Exception e) {
            System.err.println("解析报告内容失败，导出纯文本: " + e.getMessage());
        }
        byte[] pdf = careerReportPdfService.render(report.getReportName(), agents);
        String filename = "career-report-" + id + ".pdf";
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /** 汇总落库：career_report（最新） + career_report_history（版本快照）；返回 {reportId, reportName}，失败返回 null */
    private java.util.Map<String, Object> saveReport(Long userId,
                                                     com.fasterxml.jackson.databind.JsonNode agentsNode,
                                                     String platformName,
                                                     String platformReportId) {
        if (agentsNode == null || !agentsNode.isArray() || agentsNode.isEmpty()) {
            System.err.println("报告内容为空，跳过落库");
            return null;
        }
        try {
            java.util.List<java.util.Map<String, Object>> agents = new java.util.ArrayList<>();
            StringBuilder fullText = new StringBuilder();
            String finalText = null;
            java.util.List<java.util.Map<String, Object>> goals = new java.util.ArrayList<>();
            String targetJob = null;
            for (com.fasterxml.jackson.databind.JsonNode a : agentsNode) {
                String key = a.path("key").asText("");
                String name = a.path("name").asText(AGENT_NAMES.getOrDefault(key, key));
                String content = a.path("content").asText("");
                java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
                item.put("key", key);
                item.put("name", name);
                item.put("content", content);
                agents.add(item);
                fullText.append(content).append("\n\n");
                // 最终报告 = 第 6 段（report_composition）；其中可能含结构化 1/3/5 目标块（前端不展示）
                if ("report_composition".equals(key)) {
                    final String M_START = "<<<GOALS_JSON>>>";
                    final String M_END = "<<<END_GOALS_JSON>>>";
                    int s = content.indexOf(M_START);
                    int e = content.indexOf(M_END);
                    if (s >= 0 && e > s) {
                        String goalJson = content.substring(s + M_START.length(), e).trim();
                        finalText = (content.substring(0, s) + content.substring(e + M_END.length())).trim();
                        try {
                            com.fasterxml.jackson.databind.JsonNode g = objectMapper.readTree(goalJson);
                            targetJob = g.path("targetJob").asText(null);
                            for (com.fasterxml.jackson.databind.JsonNode gi : g.path("goals")) {
                                goals.add(objectMapper.convertValue(gi, java.util.Map.class));
                            }
                        } catch (Exception ex) {
                            System.err.println("解析结构化目标失败（降级，不影响报告）: " + ex.getMessage());
                        }
                    } else {
                        finalText = content;
                    }
                }
            }
            if (finalText == null || finalText.isBlank()) {
                finalText = fullText.toString().trim();   // 降级：无第 6 段则用全文
            }
            java.util.Map<String, Object> content = new java.util.LinkedHashMap<>();
            content.put("agents", agents);          // 6 段过程（multi-agent 页 / 过程留档）
            content.put("final", finalText);         // 最终报告（简介，用户可见）
            content.put("goals", goals);             // 结构化 1/3/5 目标（仅后端用）
            content.put("fullText", finalText);

            String reportName = (platformName != null && !platformName.isBlank())
                    ? platformName
                    : "职业规划报告 · " + java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            CareerReport report = new CareerReport();
            report.setId(org.example.web.tool.SnowIdCreater.generateId(23));
            report.setUserId(userId);
            report.setReportName(reportName);
            report.setReportType(1);
            report.setVersion(1);
            report.setStatus(2);
            report.setReportContent(objectMapper.writeValueAsString(content));
            report.setPlatformReportId(platformReportId);
            report.setCreateTime(java.time.LocalDateTime.now());
            report.setUpdateTime(java.time.LocalDateTime.now());
            careerReportMapper.insert(report);
            // 版本快照（历史表）
            CareerReportHistory history = new CareerReportHistory();
            history.setId(org.example.web.tool.SnowIdCreater.generateId(23));
            history.setReportId(report.getId());
            history.setVersion(1);
            history.setReportContent(report.getReportContent());
            history.setChangeReason("多智能体生成");
            history.setCreateTime(java.time.LocalDateTime.now());
            careerReportHistoryMapper.insert(history);
            System.out.println("职业报告已落库, id=" + report.getId() + ", 智能体数=" + agents.size());
            // 结构化 1/3/5 年目标 → grow_plan / grow_task（完成情况后续由 /api/grow 更新）
            if (!goals.isEmpty()) {
                try {
                    growPlanService.saveGoalsFromReport(userId, report.getId(), targetJob, goals);
                } catch (Exception ex) {
                    System.err.println("写入成长计划失败（不影响报告落库）: " + ex.getMessage());
                }
            }
            java.util.Map<String, Object> info = new java.util.LinkedHashMap<>();
            info.put("reportId", report.getId());
            info.put("reportName", report.getReportName());
            return info;
        } catch (Exception e) {
            System.err.println("报告落库失败: " + e);
            e.printStackTrace();
            return null;
        }
    }

    /** 从登录 token 解析当前用户ID（失败返回 null） */
    private Long currentUserId(String token) {
        try {
            java.util.Map<String, Object> claims = org.example.web.tool.JwtUtil.parseToken(token);
            return Long.parseLong(String.valueOf(claims.get("id")));
        } catch (Exception e) {
            return null;
        }
    }


    @Autowired
    private CareerReportService careerReportService;

    @Autowired
    private org.example.web.service.GrowPlanService growPlanService;

    /**
     * 创建职业报告
     * 
     * @param careerReport 报告对象
     * @return 包含报告信息的Result对象
     */
    @PostMapping("/create")
    public Result createCareerReport(@RequestBody CareerReport careerReport) {
        try {
            CareerReport createdReport = careerReportService.createCareerReport(careerReport);
            if (createdReport != null) {
                return Result.success("职业报告创建成功", createdReport);
            } else {
                return Result.error("职业报告创建失败");
            }
        } catch (Exception e) {
            return Result.error("创建职业报告时发生错误: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取职业报告
     * 
     * @param id 报告ID
     * @return 包含报告信息的Result对象
     */
    @GetMapping("/{id}")
    public Result getCareerReport(@PathVariable Long id) {
        try {
            CareerReport report = careerReportService.getCareerReportById(id);
            if (report != null) {
                return Result.success("获取职业报告成功", report);
            } else {
                return Result.error("未找到职业报告");
            }
        } catch (Exception e) {
            return Result.error("获取职业报告时发生错误: " + e.getMessage());
        }
    }

    /**
     * 根据用户ID获取职业报告列表
     * 
     * @param userId 用户ID
     * @return 包含报告列表的Result对象
     */
    @GetMapping("/user/{userId}")
    public Result getCareerReportsByUser(@PathVariable Long userId) {
        try {
            List<CareerReport> reports = careerReportService.getCareerReportsByUserId(userId);
            return Result.success("获取用户职业报告成功", reports);
        } catch (Exception e) {
            return Result.error("获取用户职业报告时发生错误: " + e.getMessage());
        }
    }

    /**
     * 根据匹配ID获取职业报告
     * 
     * @param matchId 匹配记录ID
     * @return 包含报告信息的Result对象
     */
    @GetMapping("/match/{matchId}")
    public Result getCareerReportByMatch(@PathVariable Long matchId) {
        try {
            CareerReport report = careerReportService.getCareerReportByMatchId(matchId);
            if (report != null) {
                return Result.success("获取匹配报告成功", report);
            } else {
                return Result.error("未找到匹配报告");
            }
        } catch (Exception e) {
            return Result.error("获取匹配报告时发生错误: " + e.getMessage());
        }
    }

    /**
     * 生成职业报告
     * 启动异步报告生成流程，调用AI服务器生成报告内容
     * 注意：实际报告生成由AI服务器完成，本接口负责触发AI生成
     * 
     * @param userId 用户ID
     * @param matchId 匹配记录ID
     * @param reportType 报告类型：1-职业探索报告，2-目标设定报告，3-完整职业规划报告
     * @return 包含报告生成状态和报告ID的Result对象
     */
    @PostMapping("/generate")
    public Result generateCareerReport(@RequestParam Long userId,
                                       @RequestParam(required = false) Long matchId,
                                       @RequestParam Integer reportType) {
        try {
            Map<String, Object> result = careerReportService.generateCareerReport(userId, matchId, reportType);
            if (result != null && !result.containsKey("error")) {
                return Result.success("报告生成请求已发送到AI服务器", result);
            } else {
                return Result.error(result != null ? (String) result.get("error") : "报告生成失败");
            }
        } catch (Exception e) {
            return Result.error("调用AI服务器生成报告时发生错误: " + e.getMessage());
        }
    }

    /**
     * 获取报告生成状态
     * 
     * @param reportId 报告ID
     * @return 包含生成状态的Result对象
     */
    @GetMapping("/status/{reportId}")
    public Result getReportGenerationStatus(@PathVariable Long reportId) {
        try {
            Map<String, Object> status = careerReportService.getReportGenerationStatus(reportId);
            if (status.containsKey("error")) {
                return Result.error((String) status.get("error"));
            }
            return Result.success("获取报告生成状态成功", status);
        } catch (Exception e) {
            return Result.error("获取报告生成状态时发生错误: " + e.getMessage());
        }
    }

    /**
     * 更新报告内容
     * 
     * @param reportId 报告ID
     * @param reportContent 报告内容（JSON格式）
     * @param changeReason 修改原因
     * @return 包含更新后报告的Result对象
     */
    @PutMapping("/content/{reportId}")
    public Result updateReportContent(@PathVariable Long reportId,
                                      @RequestParam String reportContent,
                                      @RequestParam String changeReason) {
        try {
            CareerReport updatedReport = careerReportService.updateReportContent(reportId, reportContent, changeReason);
            if (updatedReport != null) {
                return Result.success("报告内容更新成功", updatedReport);
            } else {
                return Result.error("报告内容更新失败");
            }
        } catch (Exception e) {
            return Result.error("更新报告内容时发生错误: " + e.getMessage());
        }
    }

    /**
     * 更新报告状态
     * 
     * @param reportId 报告ID
     * @param status 报告状态：1-草稿，2-生成中，3-已生成，4-已修改，5-已确认
     * @return 包含操作结果的Result对象
     */
    @PutMapping("/status/{reportId}")
    public Result updateReportStatus(@PathVariable Long reportId,
                                     @RequestParam Integer status) {
        try {
            boolean success = careerReportService.updateReportStatus(reportId, status);
            if (success) {
                return Result.success("报告状态更新成功");
            } else {
                return Result.error("报告状态更新失败");
            }
        } catch (Exception e) {
            return Result.error("更新报告状态时发生错误: " + e.getMessage());
        }
    }

    /**
     * 逻辑删除职业报告
     * 
     * @param id 报告ID
     * @return 包含操作结果的Result对象
     */
    @DeleteMapping("/{id}")
    public Result deleteCareerReport(@PathVariable Long id) {
        try {
            boolean success = careerReportService.deleteCareerReport(id);
            if (success) {
                return Result.success("职业报告删除成功");
            } else {
                return Result.error("职业报告删除失败");
            }
        } catch (Exception e) {
            return Result.error("删除职业报告时发生错误: " + e.getMessage());
        }
    }

    /**
     * 获取报告历史版本列表
     * 
     * @param reportId 报告ID
     * @return 包含历史版本列表的Result对象
     */
    @GetMapping("/history/{reportId}")
    public Result getReportHistory(@PathVariable Long reportId) {
        try {
            List<CareerReportHistory> history = careerReportService.getReportHistory(reportId);
            return Result.success("获取报告历史版本成功", history);
        } catch (Exception e) {
            return Result.error("获取报告历史版本时发生错误: " + e.getMessage());
        }
    }

    /**
     * 获取特定版本报告
     * 
     * @param reportId 报告ID
     * @param version 版本号
     * @return 包含历史版本报告的Result对象
     */
    @GetMapping("/history/{reportId}/version/{version}")
    public Result getReportByVersion(@PathVariable Long reportId,
                                     @PathVariable Integer version) {
        try {
            CareerReportHistory history = careerReportService.getReportByVersion(reportId, version);
            if (history != null) {
                return Result.success("获取报告历史版本成功", history);
            } else {
                return Result.error("未找到指定版本的报告");
            }
        } catch (Exception e) {
            return Result.error("获取报告历史版本时发生错误: " + e.getMessage());
        }
    }

    /**
     * 恢复报告到指定历史版本
     * 
     * @param reportId 报告ID
     * @param version 要恢复的版本号
     * @param changeReason 恢复原因
     * @return 包含恢复后报告的Result对象
     */
    @PostMapping("/restore/{reportId}")
    public Result restoreReportToVersion(@PathVariable Long reportId,
                                         @RequestParam Integer version,
                                         @RequestParam String changeReason) {
        try {
            CareerReport restoredReport = careerReportService.restoreReportToVersion(reportId, version, changeReason);
            if (restoredReport != null) {
                return Result.success("报告恢复成功", restoredReport);
            } else {
                return Result.error("报告恢复失败");
            }
        } catch (Exception e) {
            return Result.error("恢复报告时发生错误: " + e.getMessage());
        }
    }

    /**
     * 批量生成报告
     * 
     * @param userIds 用户ID列表
     * @param matchIds 匹配记录ID列表
     * @param reportType 报告类型
     * @return 包含批量生成结果的Result对象
     */
    @PostMapping("/batch-generate")
    public Result batchGenerateReports(@RequestParam List<Long> userIds,
                                       @RequestParam(required = false) List<Long> matchIds,
                                       @RequestParam Integer reportType) {
        try {
            List<CareerReport> reports = careerReportService.batchGenerateReports(userIds, matchIds, reportType);
            return Result.success("批量报告生成请求已发送", reports);
        } catch (Exception e) {
            return Result.error("批量生成报告时发生错误: " + e.getMessage());
        }
    }

    // ============== 高级报告功能API ==============
    
    /**
     * 生成指定类型的职业报告（高级）
     * 
     * @param userId 用户ID
     * @param matchId 匹配记录ID（可选）
     * @param reportType 报告类型
     * @param templateId 模板ID（可选）
     * @return 包含生成结果的Result对象
     */
    @PostMapping("/generate-by-type")
    public Result generateReportByType(@RequestParam Long userId,
                                       @RequestParam(required = false) Long matchId,
                                       @RequestParam Integer reportType,
                                       @RequestParam(required = false) Long templateId) {
        try {
            Map<String, Object> result = careerReportService.generateReportByType(userId, matchId, reportType, templateId);
            return Result.success("高级报告生成请求已发送", result);
        } catch (Exception e) {
            return Result.error("高级报告生成时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 重试报告生成
     * 
     * @param reportId 报告ID
     * @return 包含操作结果的Result对象
     */
    @PostMapping("/retry/{reportId}")
    public Result retryReportGeneration(@PathVariable Long reportId) {
        try {
            boolean success = careerReportService.retryReportGeneration(reportId);
            if (success) {
                return Result.success("报告重试生成成功");
            } else {
                return Result.error("报告重试生成失败");
            }
        } catch (Exception e) {
            return Result.error("重试报告生成时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 比较两个报告版本的差异
     * 
     * @param reportId 报告ID
     * @param version1 版本1
     * @param version2 版本2
     * @return 包含版本差异的Result对象
     */
    @GetMapping("/compare/{reportId}")
    public Result compareReportVersions(@PathVariable Long reportId,
                                        @RequestParam Integer version1,
                                        @RequestParam Integer version2) {
        try {
            Map<String, Object> comparison = careerReportService.compareReportVersions(reportId, version1, version2);
            if (comparison.containsKey("error")) {
                return Result.error((String) comparison.get("error"));
            }
            return Result.success("版本比较成功", comparison);
        } catch (Exception e) {
            return Result.error("比较报告版本时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 生成能力排名分析
     * 
     * @param userId 用户ID
     * @param reportId 报告ID（可选）
     * @return 包含能力排名分析的Result对象
     */
    @GetMapping("/ability-ranking/{userId}")
    public Result generateAbilityRankingAnalysis(@PathVariable Long userId,
                                                 @RequestParam(required = false) Long reportId) {
        try {
            Map<String, Object> analysis = careerReportService.generateAbilityRankingAnalysis(userId, reportId);
            return Result.success("能力排名分析请求已发送", analysis);
        } catch (Exception e) {
            return Result.error("生成能力排名分析时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 导出报告为指定格式
     * 
     * @param reportId 报告ID
     * @param format 导出格式：pdf, word, html
     * @return 包含导出信息的Result对象
     */
    @GetMapping("/export/{reportId}")
    public Result exportReport(@PathVariable Long reportId,
                               @RequestParam String format) {
        try {
            Map<String, Object> exportInfo = careerReportService.exportReport(reportId, format);
            if (exportInfo.containsKey("error")) {
                return Result.error((String) exportInfo.get("error"));
            }
            return Result.success("导出请求已处理", exportInfo);
        } catch (Exception e) {
            return Result.error("导出报告时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 生成报告分享链接
     * 
     * @param reportId 报告ID
     * @param permissionLevel 权限等级：1-仅自己可见，2-对指导老师可见，3-对授权企业可见
     * @param expireHours 过期时间（小时），0表示永不过期
     * @return 包含分享链接信息的Result对象
     */
    @PostMapping("/share/{reportId}")
    public Result generateShareLink(@PathVariable Long reportId,
                                    @RequestParam Integer permissionLevel,
                                    @RequestParam(defaultValue = "0") Integer expireHours) {
        try {
            Map<String, Object> shareInfo = careerReportService.generateShareLink(reportId, permissionLevel, expireHours);
            if (shareInfo.containsKey("error")) {
                return Result.error((String) shareInfo.get("error"));
            }
            return Result.success("分享链接生成成功", shareInfo);
        } catch (Exception e) {
            return Result.error("生成分享链接时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 验证报告分享链接
     * 
     * @param shareToken 分享令牌
     * @return 包含验证结果的Result对象
     */
    @GetMapping("/share/validate")
    public Result validateShareLink(@RequestParam String shareToken) {
        try {
            Map<String, Object> validation = careerReportService.validateShareLink(shareToken);
            return Result.success("分享链接验证完成", validation);
        } catch (Exception e) {
            return Result.error("验证分享链接时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 撤销报告分享链接
     * 
     * @param shareToken 分享令牌
     * @return 包含操作结果的Result对象
     */
    @DeleteMapping("/share/revoke")
    public Result revokeShareLink(@RequestParam String shareToken) {
        try {
            boolean success = careerReportService.revokeShareLink(shareToken);
            if (success) {
                return Result.success("分享链接撤销成功");
            } else {
                return Result.error("分享链接撤销失败");
            }
        } catch (Exception e) {
            return Result.error("撤销分享链接时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取报告的所有分享链接
     * 
     * @param reportId 报告ID
     * @return 包含分享链接列表的Result对象
     */
    @GetMapping("/share/{reportId}/all")
    public Result getReportShareLinks(@PathVariable Long reportId) {
        try {
            List<Map<String, Object>> shareLinks = careerReportService.getReportShareLinks(reportId);
            return Result.success("获取分享链接成功", shareLinks);
        } catch (Exception e) {
            return Result.error("获取分享链接时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户的报告列表（支持按类型和状态筛选）
     * 
     * @param userId 用户ID
     * @param reportType 报告类型（可选）
     * @param status 报告状态（可选）
     * @return 包含筛选后报告列表的Result对象
     */
    @GetMapping("/user/{userId}/filtered")
    public Result getUserReportsWithFilter(@PathVariable Long userId,
                                           @RequestParam(required = false) Integer reportType,
                                           @RequestParam(required = false) Integer status) {
        try {
            List<CareerReport> reports = careerReportService.getUserReportsWithFilter(userId, reportType, status);
            return Result.success("获取筛选后的报告列表成功", reports);
        } catch (Exception e) {
            return Result.error("获取筛选后的报告列表时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 确认报告（用户确认报告内容）
     * 
     * @param reportId 报告ID
     * @return 包含操作结果的Result对象
     */
    @PostMapping("/confirm/{reportId}")
    public Result confirmReport(@PathVariable Long reportId) {
        try {
            boolean success = careerReportService.confirmReport(reportId);
            if (success) {
                return Result.success("报告确认成功");
            } else {
                return Result.error("报告确认失败");
            }
        } catch (Exception e) {
            return Result.error("确认报告时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 提交报告反馈
     * 
     * @param reportId 报告ID
     * @param feedback 反馈内容
     * @param feedbackScore 反馈评分（1-5分）
     * @return 包含操作结果的Result对象
     */
    @PostMapping("/feedback/{reportId}")
    public Result submitReportFeedback(@PathVariable Long reportId,
                                       @RequestParam String feedback,
                                       @RequestParam Integer feedbackScore) {
        try {
            boolean success = careerReportService.submitReportFeedback(reportId, feedback, feedbackScore);
            if (success) {
                return Result.success("报告反馈提交成功");
            } else {
                return Result.error("报告反馈提交失败");
            }
        } catch (Exception e) {
            return Result.error("提交报告反馈时发生错误: " + e.getMessage());
        }
    }
}
