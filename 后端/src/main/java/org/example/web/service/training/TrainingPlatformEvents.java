package org.example.web.service.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Consumer;

/** Keeps raw score cards separate from display text, and never treats an error as a reply. */
public final class TrainingPlatformEvents {
    private final ObjectMapper mapper;
    private final Consumer<String> onText;
    private final StringBuilder text = new StringBuilder();
    private JsonNode score;
    private boolean finished;

    public TrainingPlatformEvents(ObjectMapper mapper, Consumer<String> onText) {
        this.mapper = mapper;
        this.onText = onText;
    }

    public void accept(String raw) {
        if (finished) return;
        try {
            JsonNode event = mapper.readTree(raw);
            switch (event.path("type").asText()) {
                case "TEXT_MESSAGE_CONTENT" -> append(event.path("delta").asText(event.path("content").asText("")));
                case "CUSTOM" -> {
                    JsonNode value = event.has("value") ? event.get("value") : event;
                    JsonNode data = value.path("data");
                    if (data.isTextual()) data = mapper.readTree(data.asText());
                    if (data.has("scenario_score")) score = data.deepCopy();
                    else if (data.has("scenario") && data.has("dimensions")) {
                        score = mapper.createObjectNode().set("scenario_score", data);
                    } else if (text.isEmpty() && data.path("response").isTextual()) append(data.path("response").asText());
                }
                case "RUN_ERROR" -> throw new TrainingException(502, "PLATFORM_RUN_ERROR", "平台未完成本次训练请求，请检查应用发布和鉴权配置后重试");
                case "RUN_FINISHED" -> finished = true;
                default -> { /* Handshake, tool and message lifecycle events contain no user reply. */ }
            }
        } catch (TrainingException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainingException(502, "PLATFORM_EVENT_INVALID", "平台返回的训练事件格式无效");
        }
    }

    private void append(String delta) {
        if (text.length() + delta.length() > 64000) throw new TrainingException(502, "PLATFORM_OUTPUT_LIMIT", "训练回复过长，请重试");
        if (!delta.isEmpty()) { text.append(delta); onText.accept(text.toString()); }
    }

    public boolean finished() { return finished; }
    public ScenarioAgentGateway.Output result() {
        if (!finished) throw new TrainingException(502, "PLATFORM_INTERRUPTED", "平台连接提前结束，已保存的回答可以重试");
        if (text.isEmpty() && score == null) throw new TrainingException(502, "PLATFORM_EMPTY", "平台未返回训练内容，请重试");
        return new ScenarioAgentGateway.Output(text.toString(), score);
    }
}
