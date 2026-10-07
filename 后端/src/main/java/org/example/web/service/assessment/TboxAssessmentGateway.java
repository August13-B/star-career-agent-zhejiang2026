package org.example.web.service.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import org.example.web.config.TboxProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * 百宝箱「能力补充测评」接口客户端。
 *
 * <p>契约（与平台交付一致）：{@code POST ${TBOX_API_URL}${ASSESSMENT_API_PATH}}，体 {@code {mode, prompt}}，
 * 返回顶层恰好一个键的 JSON；``` 围栏 / {@code {"response":…}} 信封 / 前后说明文字都会被自动解包
 * （平台侧已归一，这里再做一层兜底，避免通道差异导致误判）。
 */
@Service
public class TboxAssessmentGateway implements AssessmentGateway {
    private final TboxProperties properties;
    private final ObjectMapper mapper;

    public TboxAssessmentGateway(TboxProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
    }

    @Override
    public JsonNode execute(String mode, String prompt) {
        String path = properties.getAssessmentPath();
        if (!properties.isConfigured() || path == null || path.isBlank()) {
            throw new AssessmentException(503, "ASSESSMENT_NOT_CONFIGURED", "尚未配置能力补充测评接口路径");
        }
        HttpHeaders headers = new HttpHeaders();
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            headers.setBearerAuth(properties.getApiKey());
        }
        if (properties.getAssessmentToken() != null && !properties.getAssessmentToken().isBlank()) {
            headers.set("X-Assessment-Token", properties.getAssessmentToken());
        }
        var http = WebClient.builder().baseUrl(properties.getApiUrl().replaceAll("/+$", ""))
                .defaultHeaders(h -> h.addAll(headers)).build();
        String raw;
        try {
            raw = http.post().uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("mode", mode, "prompt", prompt == null ? "" : prompt))
                    .retrieve()
                    .onStatus(status -> status.value() == 401, response -> Mono.error(new AssessmentException(502, "ASSESSMENT_UNAUTHORIZED",
                            "测评接口鉴权失败：请核对 ASSESSMENT_API_TOKEN 与 X-Assessment-Token 是否一致")))
                    .onStatus(status -> status.value() == 400, response -> Mono.error(new AssessmentException(502, "ASSESSMENT_BAD_REQUEST",
                            "测评接口拒绝了本次请求：请核对接口路径与 mode")))
                    .onStatus(status -> status.isError(), response -> response.bodyToMono(String.class).defaultIfEmpty("")
                            .flatMap(body -> {
                                String preview = body.length() > 300 ? body.substring(0, 300) : body;
                                System.err.println("测评接口返回错误: mode=" + mode + " HTTP " + response.statusCode().value()
                                        + " | body 前 300 字: " + preview);
                                return Mono.error(new AssessmentException(502, "ASSESSMENT_UNAVAILABLE",
                                        "测评接口返回错误（HTTP " + response.statusCode().value() + "），请稍后重试"));
                            }))
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(Math.max(30, properties.getAssessmentTimeoutSeconds())));
        } catch (AssessmentException e) {
            throw e;
        } catch (Exception e) {
            throw new AssessmentException(502, "ASSESSMENT_UNAVAILABLE", "测评接口连接失败或超时，请重试");
        }
        String payload = unwrap(raw);
        JsonNode node = parse(payload);
        if (node == null || !node.isObject()) {
            String preview = payload == null ? "null" : payload.substring(0, Math.min(300, payload.length()));
            System.err.println("测评接口返回无法解析（前 300 字）: " + preview);
            throw new AssessmentException(502, "ASSESSMENT_INVALID", "测评接口没有返回合法 JSON，请重试（原文已打印到后端日志）");
        }
        return node;
    }

    /** 解包 ``` 围栏与 {@code {"response":"<JSON字符串>"}} 信封（平台已归一，这里是双保险）。 */
    private String unwrap(String raw) {
        String text = raw == null ? "" : raw.strip();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("```\\s*$", "").strip();
        }
        JsonNode node = parse(text);
        if (node != null && node.isObject() && node.path("response").isTextual()) {
            return node.path("response").asText().strip();
        }
        return text;
    }

    private JsonNode parse(String text) {
        try {
            return mapper.readTree(text);
        } catch (Exception strict) {
            // 兜底：修复字符串值里的裸引号（模型在评语里加英文双引号时会出现）
            try {
                return mapper.readTree(org.example.web.tool.JsonRepair.repairUnescapedQuotes(text));
            } catch (Exception still) {
                System.err.println("测评 JSON 解析失败（裸引号修复后仍失败）: " + strict.getMessage());
                return null;
            }
        }
    }
}
