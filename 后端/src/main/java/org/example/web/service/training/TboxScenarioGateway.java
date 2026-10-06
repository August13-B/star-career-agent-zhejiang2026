package org.example.web.service.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;
import org.example.web.config.TboxProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class TboxScenarioGateway implements ScenarioAgentGateway {
    private final TboxProperties properties;
    private final ObjectMapper mapper;

    public TboxScenarioGateway(TboxProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
    }

    @Override
    public Output execute(String namespace, String prompt, Consumer<String> onText) {
        if (!properties.isConfigured()) throw new TrainingException(503, "PLATFORM_NOT_CONFIGURED", "尚未配置百宝箱应用地址");
        HttpHeaders headers = new HttpHeaders();
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) headers.setBearerAuth(properties.getApiKey());
        var http = WebClient.builder().baseUrl(properties.getApiUrl().replaceAll("/+$", ""))
                .defaultHeaders(h -> h.addAll(headers)).build();
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
            // Never expose endpoint credentials, response bodies or user input in diagnostics.
            throw new TrainingException(502, "PLATFORM_UNAVAILABLE", "训练平台连接失败或超时；已保存的回答可以重试");
        }
    }

    private String requiredId(JsonNode node, String field) {
        String value = node == null ? "" : node.path(field).asText("");
        if (value.isBlank() || value.equals("null")) throw new TrainingException(502, "PLATFORM_SESSION_INVALID", "平台未创建有效训练会话");
        return value;
    }
}
