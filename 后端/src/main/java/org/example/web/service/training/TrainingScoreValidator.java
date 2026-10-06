package org.example.web.service.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TrainingScoreValidator {
    private final TrainingTemplate template;
    private final ObjectMapper mapper;

    public TrainingScoreValidator(TrainingTemplate template, ObjectMapper mapper) {
        this.template = template;
        this.mapper = mapper;
    }

    public ObjectNode validate(JsonNode raw, Map<String, String> userAnswers) {
        return validate(raw, template.get(template.id()), userAnswers.entrySet().stream().map(e -> new TrainingEvidenceCatalog.Source("turn", e.getKey(), "", e.getValue())).toList(), false);
    }

    public ObjectNode validate(JsonNode raw, TrainingTemplate.Definition template, List<TrainingEvidenceCatalog.Source> sources, boolean requireArtifact) {
        require(raw != null && raw.isObject() && !(raw.has("training_evaluation") && raw.has("scenario_score")), "评分根结构无效或含有多个结果");
        JsonNode score = raw.has("training_evaluation") ? raw.path("training_evaluation") : raw.path("scenario_score");
        require(template.scenario().equals(score.path("scenario").asText()), "评分场景不匹配");
        require(template.id().equals(score.path("templateVersion").asText()), "评分缺少正确的模板版本");
        require(template.rubricVersion().equals(score.path("rubricVersion").asText()), "评分缺少正确的规则版本");
        JsonNode dimensions = score.path("dimensions");
        Set<String> expected = new HashSet<>();
        template.weights().fieldNames().forEachRemaining(expected::add);
        Set<String> actual = new HashSet<>();
        dimensions.fieldNames().forEachRemaining(actual::add);
        require(dimensions.isObject() && expected.equals(actual), "评分维度不完整或含有不允许的维度");
        double total = 0;
        for (String dimension : expected) {
            JsonNode value = dimensions.get(dimension);
            require(value.isIntegralNumber() && value.canConvertToInt() && value.intValue() >= 0 && value.intValue() <= 100, "评分必须是 0–100 的整数");
            total += value.intValue() * template.weights().path(dimension).asInt() / 100.0;
        }
        Set<String> covered = new HashSet<>();
        var catalog = TrainingEvidenceCatalog.fromSources(sources);
        boolean artifactCovered = false;
        var validatedEvidence = mapper.createArrayNode();
        JsonNode evidence = score.path("evidence");
        require(evidence.isArray() && evidence.size() <= 24, "评分缺少回答证据");
        for (JsonNode item : evidence) {
            String dimension = item.path("dimension").asText();
            require(expected.contains(dimension), "评分证据维度无效");
            String sourceId, quote, sourceType = "turn", field = "";
            if (item.has("evidenceId")) {
                require(item.size() == 2 && item.path("evidenceId").isTextual(), "证据编号格式无效");
                var excerpt = catalog.get(item.path("evidenceId").asText());
                require(excerpt != null, "评分引用了不存在的回答片段");
                sourceId = excerpt.sourceId(); quote = excerpt.quote(); sourceType = excerpt.sourceType(); field = excerpt.field();
            } else {
                require("turn".equals(item.path("sourceType").asText()) && item.path("sourceId").isTextual() && item.path("quote").isTextual(), "评分证据类型或消息ID格式无效");
                sourceId = item.path("sourceId").asText(); quote = item.path("quote").asText().strip();
            }
            String answer = null;
            for (var source : sources) if (source.sourceId().equals(sourceId) && source.sourceType().equals(sourceType) && source.field().equals(field)) answer = source.content();
            require(answer != null && !quote.isBlank() && quote.length() <= 500 && answer.contains(quote), "评分证据未对应本人的原始回答");
            validatedEvidence.addObject().put("dimension", dimension).put("sourceType", sourceType).put("sourceId", sourceId).put("field", field).put("quote", quote);
            artifactCovered |= "artifact".equals(sourceType);
            covered.add(dimension);
        }
        require(covered.equals(expected), "部分维度缺少可核对的回答证据");
        require(!requireArtifact || artifactCovered, "评分缺少最终作品的证据");
        require(score.path("comment").isTextual() && !score.path("comment").asText().isBlank() && score.path("comment").asText().length() <= 2000, "评分缺少有效评语");
        JsonNode suggestions = score.path("suggestions");
        require(suggestions.isArray() && !suggestions.isEmpty() && suggestions.size() <= 5, "评分缺少改进建议");
        for (JsonNode suggestion : suggestions) require(suggestion.isTextual() && !suggestion.asText().isBlank() && suggestion.asText().length() <= 1000, "改进建议格式无效");
        // Allowlist output fields; don't forward arbitrary model-generated HTML or instructions.
        ObjectNode validated = mapper.createObjectNode();
        for (String field : new String[]{"scenario", "templateVersion", "rubricVersion", "dimensions", "comment", "suggestions"}) validated.set(field, score.get(field));
        validated.set("evidence", validatedEvidence);
        validated.put("total", Math.round(total));
        return validated;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new TrainingException(422, "SCORE_REVIEW_REQUIRED", message);
    }
}
