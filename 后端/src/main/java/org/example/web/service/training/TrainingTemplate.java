package org.example.web.service.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class TrainingTemplate {
    private final Map<String, Definition> templates = new LinkedHashMap<>();

    public TrainingTemplate(ObjectMapper mapper) throws IOException {
        for (String name : List.of("interview_backend_intern.v1", "communication_release.v1", "office_review.v1")) {
            try (var in = new ClassPathResource("training/" + name + ".json").getInputStream()) {
                Definition definition = new Definition(mapper.readTree(in));
                templates.put(definition.id(), definition);
            }
        }
    }

    public record Definition(JsonNode raw) {
        public String id() { return raw.path("id").asText(); }
        public String scenario() { return raw.path("scenario").asText(); }
        public String title() { return raw.path("title").asText(); }
        public String rubricVersion() { return raw.path("rubricVersion").asText(); }
        public int rounds() { return raw.path("questions").size(); }
        public JsonNode weights() { return raw.path("weights"); }
        public JsonNode rubric() { return raw.path("rubric"); }
        public String question(int index) { return raw.path("questions").get(index).asText(); }

        /**
         * 用岗位变量渲染模板文本（{@code {{job}}} / {@code {{skills}}} / {@code {{industry}}} 等）。
         *
         * <p>题目文本仍以后端模板为准，只是被岗位参数化；未提供的变量名保持原样。
         */
        public Definition withVariables(Map<String, String> variables) {
            if (variables == null || variables.isEmpty()) return this;
            return new Definition(render(raw, variables));
        }

        private static JsonNode render(JsonNode node, Map<String, String> variables) {
            if (node.isTextual()) {
                String text = node.asText();
                for (Map.Entry<String, String> entry : variables.entrySet()) {
                    if (entry.getValue() != null) text = text.replace("{{" + entry.getKey() + "}}", entry.getValue());
                }
                return TextNode.valueOf(text);
            }
            if (node.isObject()) {
                ObjectNode copy = JsonNodeFactory.instance.objectNode();
                node.fields().forEachRemaining(field -> copy.set(field.getKey(), render(field.getValue(), variables)));
                return copy;
            }
            if (node.isArray()) {
                ArrayNode copy = JsonNodeFactory.instance.arrayNode();
                for (JsonNode item : node) copy.add(render(item, variables));
                return copy;
            }
            return node.deepCopy();
        }
        /** 单题限时（秒）；0 或缺失 = 不限时 */
        public int timeLimitSeconds() { return Math.max(0, raw.path("timeLimitSeconds").asInt(0)); }
        public String speaker(int index) { return index >= rounds() ? "训练主持" : raw.path("speakers").path(index).asText("面试官"); }
        public JsonNode fields() { return raw.path("artifactFields"); }
        public boolean needsArtifact() { return fields().isArray() && !fields().isEmpty(); }
        public Map<String, Object> view() {
            Map<String, Object> result = new LinkedHashMap<>();
            for (String field : List.of("id", "scenario", "title", "description", "duration", "rubricVersion")) result.put(field, raw.path(field).asText());
            result.put("timeLimitSeconds", timeLimitSeconds());
            result.put("rounds", rounds()); result.put("dimensions", raw.path("dimensionLabels")); result.put("weights", weights());
            for (String field : List.of("materials", "practiceDraft", "artifactTitle", "artifactFields", "stageLabels")) if (raw.has(field)) result.put(field, raw.get(field));
            result.put("enabled", true); result.put("profileUpdateEnabled", true);
            return result;
        }
    }
    public Definition get(String id) {
        Definition value = templates.get(id);
        if (value == null) throw new TrainingException(422, "TEMPLATE_INVALID", "训练模板不存在");
        return value;
    }
    public List<Map<String, Object>> views() { return templates.values().stream().map(Definition::view).toList(); }
    public String id() { return "interview_backend_intern.v1"; }
    public String rubricVersion() { return get(id()).rubricVersion(); }
    public JsonNode weights() { return get(id()).weights(); }
    public JsonNode rubric() { return get(id()).rubric(); }
    public int rounds() { return get(id()).rounds(); }
    public String question(int index) { return get(id()).question(index); }
    public Map<String, Object> view() { return get(id()).view(); }
}
