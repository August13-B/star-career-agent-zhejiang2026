package org.example.web.service.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
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
        // 容错：部分模型把 suggestions / comment / perQuestion 放到与 training_evaluation 平级的顶层 → 归位到结果对象内
        JsonNode scoreNode = raw.has("training_evaluation") ? raw.path("training_evaluation") : raw.path("scenario_score");
        require(scoreNode.isObject(), "评分结构无效");
        ObjectNode score = scoreNode.deepCopy();
        for (String sibling : new String[]{"suggestions", "comment", "perQuestion", "highlights"}) {
            if (!score.has(sibling) && raw.has(sibling)) score.set(sibling, raw.get(sibling));
        }
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
            require(value != null && value.isIntegralNumber() && value.canConvertToInt()
                    && value.intValue() >= 0 && value.intValue() <= 100, "评分必须是 0–100 的整数");
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
        // 评语：允许缺失 → 按维度自动兜底（不因少一句话丢掉整份分数）
        String comment = score.path("comment").asText("").strip();
        if (comment.isBlank()) {
            comment = fallbackComment(template, dimensions);
        }
        require(comment.length() <= 2000, "评语过长");
        // 改进建议：允许缺失 → 按最弱维度自动兜底
        ObjectNode validated = mapper.createObjectNode();
        for (String fieldName : new String[]{"scenario", "templateVersion", "rubricVersion", "dimensions"}) validated.set(fieldName, score.get(fieldName));
        validated.set("evidence", validatedEvidence);
        validated.put("total", Math.round(total));
        validated.put("comment", comment);
        var tips = validated.putArray("suggestions");
        JsonNode suggestions = score.path("suggestions");
        if (suggestions.isArray() && !suggestions.isEmpty()) {
            int count = 0;
            for (JsonNode suggestion : suggestions) {
                String tip = suggestion.asText("").strip();
                if (tip.isBlank() || tip.length() > 1000) continue;
                tips.add(tip);
                if (++count >= 5) break;
            }
        }
        if (tips.isEmpty()) {
            for (String tip : fallbackSuggestions(template, dimensions)) tips.add(tip);
        }
        // 逐题反馈（可选）：每题一条 {questionNo, dimension, verdict, comment, suggestion}
        JsonNode perQuestion = score.path("perQuestion");
        if (perQuestion.isArray() && !perQuestion.isEmpty()) {
            var items = validated.putArray("perQuestion");
            for (JsonNode item : perQuestion) {
                if (!item.isObject()) continue;
                ObjectNode entry = items.addObject();
                entry.put("questionNo", item.path("questionNo").asInt(items.size()));
                String dimension = item.path("dimension").asText("");
                entry.put("dimension", expected.contains(dimension) ? dimension : "");
                String verdict = item.path("verdict").asText("");
                entry.put("verdict", List.of("correct", "partial", "wrong").contains(verdict) ? verdict : "partial");
                entry.put("comment", clip(item.path("comment").asText(""), 500));
                entry.put("suggestion", clip(item.path("suggestion").asText(""), 500));
                if (items.size() >= 24) break;
            }
        }
        return validated;
    }

    /** 评语兜底：按维度高低自动生成一句（避免因模型漏写评语而丢掉整份分数）。 */
    private String fallbackComment(TrainingTemplate.Definition template, JsonNode dimensions) {
        List<String> ranked = new ArrayList<>();
        dimensions.fieldNames().forEachRemaining(ranked::add);
        ranked.sort((a, b) -> Integer.compare(dimensions.path(b).asInt(), dimensions.path(a).asInt()));
        String top = ranked.isEmpty() ? "" : label(template, ranked.get(0)) + "（" + dimensions.path(ranked.get(0)).asInt() + "）";
        String low = ranked.isEmpty() ? "" : label(template, ranked.get(ranked.size() - 1)) + "（" + dimensions.path(ranked.get(ranked.size() - 1)).asInt() + "）";
        return "整体表现：相对优势 " + top + "；待提升 " + low + "。本评语为系统按各维度得分自动生成（平台未返回评语）。";
    }

    /** 建议兜底：针对最弱维度给一条可执行建议。 */
    private List<String> fallbackSuggestions(TrainingTemplate.Definition template, JsonNode dimensions) {
        List<String> ranked = new ArrayList<>();
        dimensions.fieldNames().forEachRemaining(ranked::add);
        ranked.sort((a, b) -> Integer.compare(dimensions.path(a).asInt(), dimensions.path(b).asInt()));
        List<String> tips = new ArrayList<>();
        for (String dimension : ranked) {
            tips.add("针对「" + label(template, dimension) + "」：把一次真实（或假设）经历按「背景→我的做法→依据→结果」整理成可复述案例，并准备一个可验证的数字或证据。");
            if (tips.size() >= 2) break;
        }
        return tips;
    }

    private static String label(TrainingTemplate.Definition template, String dimension) {
        return template.dimensions().path(dimension).asText(dimension);
    }

    private static String clip(String value, int max) {
        String text = value == null ? "" : value.strip();
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new TrainingException(422, "SCORE_REVIEW_REQUIRED", message);
    }
}
