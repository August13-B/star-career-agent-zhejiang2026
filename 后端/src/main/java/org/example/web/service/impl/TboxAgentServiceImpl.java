package org.example.web.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.web.config.TboxProperties;
import org.example.web.entity.AiConversation;
import org.example.web.mapper.AiConversationMapper;
import org.example.web.service.TboxAgentService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 百宝箱应用对接实现（WebSocket / AG-UI 事件流 → {"data":...} 流）
 *
 * <p>协议（实测）：
 * <pre>
 * 客户端：HELLO{sessionId} → SEND_MESSAGE{content, sessionId, conversationId}
 * 服务端：RUN_STARTED → TOOL_CALL* → TEXT_MESSAGE_CONTENT(delta)*
 *         → CUSTOM('tbox:card') → RUN_FINISHED{requestId} / RUN_ERROR
 * </pre>
 */
@Slf4j
@Service
public class TboxAgentServiceImpl implements TboxAgentService {

    private final TboxProperties props;
    private final AiConversationMapper conversationMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient http;
    private final WebSocketClient wsClient = new ReactorNettyWebSocketClient();

    /** 最近一次运行的平台 ID（按本地对话ID缓存，供保存消息时回填） */
    private final Map<Long, RunIds> runIdsCache = new ConcurrentHashMap<>();

    public TboxAgentServiceImpl(TboxProperties props, AiConversationMapper conversationMapper) {
        this.props = props;
        this.conversationMapper = conversationMapper;
        String base = props.getApiUrl();
        if (base != null && base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.http = WebClient.builder().baseUrl(base == null ? "" : base).build();
    }

    // ====================== 对外主入口 ======================

    @Override
    public Flux<String> chatStream(Long userId, Long localConversationId, String message) {
        if (!props.isConfigured()) {
            log.warn("百宝箱未配置（TBOX_API_URL 为空），返回不可用提示");
            return Flux.just(chunk("抱歉，AI 服务暂不可用：后端未配置百宝箱地址（TBOX_API_URL）。"));
        }
        return Mono.fromCallable(() -> resolveSession(userId, localConversationId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(session -> streamFromPlatform(session, localConversationId, message))
                .timeout(Duration.ofSeconds(Math.max(5, props.getTimeoutSeconds())))
                .onErrorResume(e -> {
                    log.error("百宝箱对话失败: {}", e.toString());
                    return Flux.just(chunk(translateError(e)));
                });
    }

    @Override
    public void rememberRunIds(Long localConversationId, String tboxMessageId, String tboxRequestId) {
        if (localConversationId == null || (tboxMessageId == null && tboxRequestId == null)) {
            return;
        }
        runIdsCache.merge(localConversationId, new RunIds(tboxMessageId, tboxRequestId),
                (old, neu) -> new RunIds(
                        neu.tboxMessageId() != null ? neu.tboxMessageId() : old.tboxMessageId(),
                        neu.tboxRequestId() != null ? neu.tboxRequestId() : old.tboxRequestId()));
    }

    @Override
    public RunIds consumeRunIds(Long localConversationId) {
        return localConversationId == null ? null : runIdsCache.remove(localConversationId);
    }

    @Override
    public String fetchRawHistory(Long localConversationId, int pageSize) {
        try {
            AiConversation conv = conversationMapper.selectConversationById(localConversationId);
            if (conv == null) {
                return "{\"error\":\"本地对话不存在\"}";
            }
            String userId = String.valueOf(conv.getUserId());
            return http.get()
                    .uri(uri -> uri.path("/api/conversation/messages")
                            .queryParam("userId", userId)
                            .queryParam("pageSize", pageSize)
                            .queryParam("format", "raw")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(20));
        } catch (Exception e) {
            log.error("拉取百宝箱历史失败", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    // ====================== 会话映射（落库，支持续会话） ======================

    private record Session(String sessionId, String conversationId) {
    }

    private Session resolveSession(Long userId, Long localConversationId) {
        AiConversation conv = conversationMapper.selectConversationById(localConversationId);
        if (conv != null && notBlank(conv.getTboxSessionId()) && notBlank(conv.getTboxConversationId())) {
            log.info("复用百宝箱会话 localConversation={} session={} tboxConversation={}",
                    localConversationId, conv.getTboxSessionId(), conv.getTboxConversationId());
            return new Session(conv.getTboxSessionId(), conv.getTboxConversationId());
        }

        String platformUserId = String.valueOf(userId);
        // 1) 平台会话
        Map<String, Object> sessionResp = http.get()
                .uri(uri -> uri.path("/api/tbox/session").queryParam("userId", platformUserId).build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .block(Duration.ofSeconds(20));
        String sessionId = sessionResp == null ? null : String.valueOf(sessionResp.get("sessionId"));

        // 2) 平台会话（对话）
        Map<String, Object> convResp = http.post()
                .uri("/api/conversation/create")
                .bodyValue(Map.of("userId", platformUserId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .block(Duration.ofSeconds(20));
        String tboxConversationId = convResp == null ? null : String.valueOf(convResp.get("conversationId"));

        if (!notBlank(sessionId) || !notBlank(tboxConversationId)) {
            throw new IllegalStateException("创建百宝箱会话失败：" + sessionResp + " / " + convResp);
        }

        // 3) 落库（下次直接复用 → 续会话）
        conversationMapper.updateTboxIds(localConversationId, sessionId, tboxConversationId);
        log.info("已建立百宝箱会话映射 localConversation={} session={} tboxConversation={}",
                localConversationId, sessionId, tboxConversationId);
        return new Session(sessionId, tboxConversationId);
    }

    // ====================== WebSocket 事件流 ======================

    private Flux<String> streamFromPlatform(Session session, Long localConversationId, String message) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        AtomicBoolean done = new AtomicBoolean(false);
        String wsUrl = props.webSocketUrl();

        String hello = toJson(Map.of("type", "HELLO", "sessionId", session.sessionId()));
        Map<String, Object> sendBody = new LinkedHashMap<>();
        sendBody.put("type", "SEND_MESSAGE");
        sendBody.put("content", message);
        sendBody.put("sessionId", session.sessionId());
        sendBody.put("conversationId", session.conversationId());
        String send = toJson(sendBody);

        log.info("连接百宝箱 WS: {} (localConversation={})", wsUrl, localConversationId);

        Mono<Void> sessionMono = wsClient.execute(URI.create(wsUrl), ws ->
                ws.send(Flux.concat(
                                Mono.just(ws.textMessage(hello)),
                                Mono.just(ws.textMessage(send))
                                        .delayElement(Duration.ofMillis(Math.max(0, props.getHelloDelayMillis())))))
                        .thenMany(ws.receive()
                                .map(WebSocketMessage::getPayloadAsText)
                                .doOnNext(raw -> handleEvent(raw, sink, done, localConversationId))
                                .takeUntil(m -> done.get()))
                        .then());

        sessionMono.subscribe(
                null,
                err -> {
                    log.error("百宝箱 WS 异常", err);
                    sink.tryEmitNext(chunk(translateError(err)));
                    sink.tryEmitComplete();
                },
                () -> {
                    log.debug("百宝箱 WS 会话结束 localConversation={}", localConversationId);
                    sink.tryEmitComplete();
                });

        return sink.asFlux();
    }

    private void handleEvent(String raw, Sinks.Many<String> sink, AtomicBoolean done, Long localConversationId) {
        log.debug("百宝箱事件: {}", raw);
        try {
            JsonNode n = objectMapper.readTree(raw);
            String type = n.path("type").asText("");
            switch (type) {
                case "TEXT_MESSAGE_CONTENT" -> {
                    String delta = firstNonBlank(n, "delta", "content", "text");
                    if (delta != null && !delta.isEmpty()) {
                        sink.tryEmitNext(chunk(delta));
                    }
                }
                case "CUSTOM" -> {
                    // CUSTOM('tbox:card', {id, messageId, cardType, data, status})
                    JsonNode value = n.has("value") ? n.get("value") : n;
                    String messageId = firstNonBlank(value, "messageId", "id");
                    rememberRunIds(localConversationId, messageId, null);
                    String md = cardToMarkdown(value);
                    if (md != null && !md.isEmpty()) {
                        sink.tryEmitNext(chunk(md));
                    }
                }
                case "RUN_FINISHED" -> {
                    String requestId = n.path("rawEvent").path("requestId").asText(null);
                    rememberRunIds(localConversationId, null, requestId);
                    done.set(true);
                }
                case "RUN_ERROR" -> {
                    String msg = n.path("message").asText("");
                    // RUN_ERROR 同样携带 rawEvent.requestId，需一并记录（三方对账）
                    String reqId = n.path("rawEvent").path("requestId").asText(null);
                    rememberRunIds(localConversationId, firstNonBlank(n, "messageId"), reqId);
                    log.warn("百宝箱返回错误: {} (raw={})", msg, raw);
                    sink.tryEmitNext(chunk(translatePlatformMessage(msg)));
                    done.set(true);
                }
                case "TOOL_CALL_START" -> log.debug("平台检索中: {}", raw);
                default -> {
                    // RUN_STARTED / TEXT_MESSAGE_START|END / TOOL_CALL_* 等：A 阶段不额外下发
                }
            }
        } catch (Exception e) {
            log.warn("解析百宝箱事件失败: {}", raw, e);
        }
    }

    // ====================== 结构化卡片 → Markdown（前端零改动） ======================

    private String cardToMarkdown(JsonNode value) {
        JsonNode data = value.has("data") ? value.get("data") : value;
        try {
            if (data.has("career_pathway")) {
                return careerPathwayToMarkdown(data.get("career_pathway"));
            }
            if (data.has("title") && data.has("phases")) {
                return careerPathwayToMarkdown(data);
            }
            if (data.has("scenario_score")) {
                return scenarioScoreToMarkdown(data.get("scenario_score"));
            }
            if (data.has("dimensions") && data.has("scenario")) {
                return scenarioScoreToMarkdown(data);
            }
            if (data.has("response")) {
                return data.get("response").asText();
            }
            // 未知卡片：降级为紧凑文本，避免前端出现原始 JSON
            return data.toString();
        } catch (Exception e) {
            log.warn("卡片转 Markdown 失败: {}", value, e);
            return data.toString();
        }
    }

    private String careerPathwayToMarkdown(JsonNode p) {
        StringBuilder sb = new StringBuilder();
        String title = p.path("title").asText("");
        if (!title.isEmpty()) {
            sb.append("### ").append(title).append("\n\n");
        }
        for (JsonNode phase : p.path("phases")) {
            sb.append("#### 阶段：").append(phase.path("phase").asText("")).append("\n");
            String goal = phase.path("goal").asText("");
            if (!goal.isEmpty()) {
                sb.append("**核心目标：** ").append(goal).append("\n");
            }
            String timeline = phase.path("timeline").asText("");
            if (!timeline.isEmpty()) {
                sb.append("**时间线：** ").append(timeline).append("\n");
            }
            JsonNode actions = phase.path("key_actions");
            if (actions.isArray() && !actions.isEmpty()) {
                sb.append("\n**关键行动：**\n");
                for (JsonNode a : actions) {
                    sb.append("- ").append(a.asText()).append("\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String scenarioScoreToMarkdown(JsonNode s) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 场景模拟评分\n\n");
        String scenario = s.path("scenario").asText("");
        if (!scenario.isEmpty()) {
            sb.append("**场景：** ").append(scenarioLabel(scenario)).append("\n\n");
        }
        JsonNode dims = s.path("dimensions");
        if (dims.isObject()) {
            sb.append("**各维度得分：**\n");
            dims.fields().forEachRemaining(e ->
                    sb.append("- ").append(dimensionLabel(e.getKey())).append("：").append(e.getValue().asText()).append("\n"));
            sb.append("\n");
        }
        if (s.has("total")) {
            sb.append("**综合得分：** ").append(s.path("total").asText()).append("\n\n");
        }
        String comment = s.path("comment").asText("");
        if (!comment.isEmpty()) {
            sb.append("**总体评语：** ").append(comment).append("\n\n");
        }
        JsonNode sugg = s.path("suggestions");
        if (sugg.isArray() && !sugg.isEmpty()) {
            sb.append("**改进建议：**\n");
            for (JsonNode g : sugg) {
                sb.append("- ").append(g.asText()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String scenarioLabel(String code) {
        return switch (code) {
            case "mock_interview" -> "模拟面试";
            case "cross_role_communication" -> "跨岗位沟通";
            case "ai_assisted_office" -> "AI 辅助办公";
            default -> code;
        };
    }

    private String dimensionLabel(String key) {
        return switch (key) {
            case "professional" -> "专业技能";
            case "communication" -> "沟通能力";
            case "teamwork" -> "团队协作";
            case "problem_solving" -> "问题解决";
            case "learning" -> "学习能力";
            case "innovation" -> "创新能力";
            case "pressure" -> "抗压能力";
            default -> key;
        };
    }

    // ====================== 工具 ======================

    /** 包装为既有转发契约的 chunk：{"data":"..."} */
    private String chunk(String text) {
        return toJson(Map.of("data", text == null ? "" : text));
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 序列化失败", e);
        }
    }

    private String firstNonBlank(JsonNode n, String... keys) {
        for (String k : keys) {
            JsonNode v = n.path(k);
            if (v.isTextual() && !v.asText().isBlank()) {
                return v.asText();
            }
        }
        return null;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank() && !"null".equals(s);
    }

    /** 平台错误码 → 可读提示 */
    private String translatePlatformMessage(String msg) {
        String m = msg == null ? "" : msg;
        if (m.contains("Not Open")) {
            return "AI 模型网关尚未开通（平台返回 " + m + "）。请在百宝箱侧开通模型后重试。";
        }
        if (m.isBlank()) {
            return "AI 服务返回未知错误，请稍后重试（详见后端日志）。";
        }
        return "AI 服务返回错误：" + m + "（详见后端日志）。";
    }

    /** 异常 → 可读提示 */
    private String translateError(Throwable e) {
        if (e instanceof TimeoutException) {
            return "AI 响应超时（" + props.getTimeoutSeconds() + " 秒），请稍后重试。";
        }
        String s = e.toString();
        if (s.contains("Not Open")) {
            return "AI 模型网关尚未开通（平台返回 Not Open）。请在百宝箱侧开通模型后重试。";
        }
        if (s.contains("Connection refused") || s.contains("UnknownHost")
                || s.contains("Failed to connect") || s.contains("timeout")) {
            return "无法连接百宝箱平台，请检查网络与 TBOX_API_URL 配置（详见后端日志）。";
        }
        return "AI 服务调用失败：" + e.getMessage();
    }
}
