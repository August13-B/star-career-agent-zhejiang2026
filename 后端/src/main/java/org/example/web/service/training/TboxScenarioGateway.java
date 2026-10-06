package org.example.web.service.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;
import org.example.web.config.TboxProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 百宝箱训练网关。
 *
 * <p>优先调用本应用内新增的三个**场景训练接口**（一次性 JSON）；未配置接口路径时，
 * 回退到旧的「会话 + WS 事件流」链路（保证向后兼容，不影响既有环境）。
 */
@Service
public class TboxScenarioGateway implements ScenarioAgentGateway {
    private final TboxProperties properties;
    private final ObjectMapper mapper;

    public TboxScenarioGateway(TboxProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
    }

    @Override
    public Output execute(Request request, Consumer<String> onText) {
        if (!properties.isConfigured()) {
            throw new TrainingException(503, "PLATFORM_NOT_CONFIGURED", "尚未配置百宝箱应用地址");
        }
        String path = properties.trainingPathFor(request.scenario());
        if (path != null) {
            return executeInterface(request, path, onText);
        }
        return executeConversation(request.namespace(), request.prompt(), onText);
    }

    // ====================== 链路一：场景训练接口（推荐） ======================

    /**
     * {@code POST <path>} 请求体 {@code {mode, prompt, scenario}}：
     * <ul>
     *   <li>{@code mode=ask} → {@code {"reply":"…"}}（也兼容裸文本）</li>
     *   <li>{@code mode=evaluate} → {@code {"training_evaluation":{…}}}</li>
     * </ul>
     */
    private Output executeInterface(Request request, String path, Consumer<String> onText) {
        boolean evaluate = "evaluate".equals(request.operation());
        String raw;
        try {
            raw = client().post().uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("mode", evaluate ? "evaluate" : "ask",
                            "prompt", request.prompt(),
                            "scenario", request.scenario()))
                    .retrieve()
                    .onStatus(status -> status.value() == 401, response -> Mono.error(new TrainingException(502, "PLATFORM_UNAUTHORIZED",
                            "训练接口鉴权失败：请核对 TRAINING_API_TOKEN 与 X-Training-Token 是否一致")))
                    .onStatus(status -> status.value() == 400, response -> Mono.error(new TrainingException(502, "PLATFORM_BAD_REQUEST",
                            "训练接口拒绝了本次请求：请核对接口路径与场景是否匹配")))
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(Math.max(30, properties.getTrainingTimeoutSeconds())));
        } catch (TrainingException e) {
            throw e;
        } catch (Exception e) {
            // Never expose endpoint credentials, response bodies or user input in diagnostics.
            throw new TrainingException(502, "PLATFORM_UNAVAILABLE", "训练平台连接失败或超时；已保存的回答可以重试");
        }
        if (raw == null || raw.isBlank()) {
            throw new TrainingException(502, "PLATFORM_EMPTY", "训练接口没有返回内容，请重试");
        }
        String payload = unwrap(raw);
        JsonNode body = parse(payload);
        if (evaluate) {
            if (body == null || !body.has("training_evaluation")) {
                throw new TrainingException(502, "PLATFORM_EVENT_INVALID", "训练接口没有返回评分结构，请重试");
            }
            // 交给 TrainingScoreValidator 做权威校验（维度、证据编号、版本号）
            return new Output(body.toString(), body);
        }
        String reply = body != null && body.path("reply").isTextual() ? body.path("reply").asText().trim() : payload;
        if (reply.isBlank()) {
            throw new TrainingException(502, "PLATFORM_EMPTY", "训练接口没有返回可用的回复，请重试");
        }
        onText.accept(reply);
        return new Output(reply, null);
    }

    // ====================== 链路二：旧会话 + WS（兜底） ======================

    private Output executeConversation(String namespace, String prompt, Consumer<String> onText) {
        HttpHeaders headers = new HttpHeaders();
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            headers.setBearerAuth(properties.getApiKey());
        }
        var http = client();
        try {
            // Each execution has an isolated platform conversation. Server-frozen history is explicit.
            JsonNode session = http.get().uri(u -> u.path("/api/tbox/session").queryParam("userId", namespace).build())
                    .retrieve().bodyToMono(JsonNode.class).block(Duration.ofSeconds(20));
            JsonNode conversation = http.post().uri("/api/conversation/create").bodyValue(Map.of("userId", namespace))
                    .retrieve().bodyToMono(JsonNode.class).block(Duration.ofSeconds(20));
            String sessionId = requiredId(session, "sessionId");
            String conversationId = requiredId(conversation, "conversationId");
            String hello = mapper.writeValueAsString(Map.of("type", "HELLO", "sessionId", sessionId));
            String send = mapper.writeValueAsString(Map.of("type", "SEND_MESSAGE", "sessionId", sessionId, "conversationId", conversationId, "content", prompt));
            var events = new TrainingPlatformEvents(mapper, onText);
            new ReactorNettyWebSocketClient().execute(URI.create(properties.webSocketUrl()), headers, ws ->
                    ws.send(Flux.concat(Mono.just(ws.textMessage(hello)),
                                    Mono.just(ws.textMessage(send)).delayElement(Duration.ofMillis(Math.max(0, properties.getHelloDelayMillis())))))
                            .thenMany(ws.receive().map(WebSocketMessage::getPayloadAsText).publishOn(Schedulers.boundedElastic())
                                    .doOnNext(events::accept).takeUntil(raw -> events.finished())).then())
                    .block(Duration.ofSeconds(Math.max(30, properties.getChatTimeoutSeconds())));
            return events.result();
        } catch (TrainingException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainingException(502, "PLATFORM_UNAVAILABLE", "训练平台连接失败或超时；已保存的回答可以重试");
        }
    }

    // ====================== 工具 ======================

    private WebClient client() {
        HttpHeaders headers = new HttpHeaders();
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            headers.setBearerAuth(properties.getApiKey());
        }
        if (properties.getTrainingToken() != null && !properties.getTrainingToken().isBlank()) {
            headers.set("X-Training-Token", properties.getTrainingToken());
        }
        return WebClient.builder().baseUrl(properties.getApiUrl().replaceAll("/+$", ""))
                .defaultHeaders(h -> h.addAll(headers)).build();
    }

    /** 解包偶发的 {@code ```json} 围栏与 {@code {"response":"<JSON字符串>"}} 包裹。 */
    private String unwrap(String raw) {
        String text = raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("```\\s*$", "").trim();
        }
        JsonNode node = parse(text);
        if (node != null && node.isObject() && node.path("response").isTextual()) {
            return node.path("response").asText().trim();
        }
        return text;
    }

    private JsonNode parse(String text) {
        try {
            return mapper.readTree(text);
        } catch (Exception e) {
            return null;
        }
    }

    private String requiredId(JsonNode node, String field) {
        String value = node == null ? "" : node.path(field).asText("");
        if (value.isBlank() || value.equals("null")) throw new TrainingException(502, "PLATFORM_SESSION_INVALID", "平台未创建有效训练会话");
        return value;
    }
}
