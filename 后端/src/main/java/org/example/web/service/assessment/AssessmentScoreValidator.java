package org.example.web.service.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.example.web.service.training.TrainingEvidenceCatalog;
import org.springframework.stereotype.Component;

/**
 * 能力补充测评的「形态 + 深度」校验（平台只做形态归一，深度校验在此）。
 *
 * <p>硬约束（与百宝箱提示词一致）：
 * <ul>
 *   <li>客观题：维度必须与要求一致、**恰好 4 个选项**、分值 1–4 且**互异**</li>
 *   <li>主观题：维度一致；不允许追问时强制 {@code followUp=false}</li>
 *   <li>评分：**恰好软实力六维**、0–100 整数、证据编号必须命中冻结目录、评语与建议有效</li>
 * </ul>
 */
@Component
public class AssessmentScoreValidator {
    private final ObjectMapper json;

    public AssessmentScoreValidator(ObjectMapper json) {
        this.json = json;
    }

    public ObjectNode objective(JsonNode node, String dimension) {
        JsonNode question = node.path("objective_question");
        require(question.isObject(), "客观题结构无效");
        require(dimension.equals(question.path("dimension").asText("")), "客观题维度与本次要求不一致");
        String text = question.path("question").asText("").strip();
        require(!text.isBlank() && text.length() <= 600, "客观题题干无效");
        JsonNode options = question.path("options");
        require(options.isArray() && options.size() == 4, "客观题必须恰好 4 个选项");
        Set<Integer> scores = new HashSet<>();
        ObjectNode normalized = json.createObjectNode();
        normalized.put("dimension", dimension);
        normalized.put("question", text);
        ArrayNode items = normalized.putArray("options");
        for (JsonNode option : options) {
            String optionText = option.path("text").asText("").strip();
            int score = option.path("score").asInt(-1);
            require(!optionText.isBlank() && optionText.length() <= 300 && score >= 1 && score <= 4, "选项内容或其分值无效");
            scores.add(score);
            ObjectNode item = items.addObject();
            item.put("text", optionText);
            item.put("score", score);
        }
        require(scores.size() == 4, "选项分值必须互异（1、2、3、4 各一次）");
        return normalized;
    }

    public ObjectNode subjective(JsonNode node, String dimension, boolean followUpAllowed) {
        JsonNode question = node.path("subjective_question");
        require(question.isObject(), "主观题结构无效");
        require(dimension.equals(question.path("dimension").asText("")), "主观题维度与本次要求不一致");
        String text = question.path("question").asText("").strip();
        require(!text.isBlank() && text.length() <= 800, "主观题题干无效");
        ObjectNode normalized = json.createObjectNode();
        normalized.put("dimension", dimension);
        normalized.put("question", text);
        normalized.put("followUp", followUpAllowed && question.path("followUp").asBoolean(false));
        return normalized;
    }

    /**
     * @param sources       冻结证据目录来源（用户回答 + 客观题的选择）
     * @param mustCover     必须给出证据的维度（= 本会话里有「可引用作答」的维度；超时未答的维度不强制）
     */
    public ObjectNode evaluation(JsonNode node, List<TrainingEvidenceCatalog.Source> sources, Set<String> mustCover) {
        JsonNode score = node.path("ability_evaluation");
        require(score.isObject(), "评分结构无效");
        JsonNode dimensions = score.path("dimensions");
        require(dimensions.isObject(), "评分缺少 dimensions");
        Set<String> actual = new HashSet<>();
        dimensions.fieldNames().forEachRemaining(actual::add);
        require(new HashSet<>(AssessmentStore.SOFT_DIMENSIONS).equals(actual), "评分维度必须恰好是软实力六维");
        ObjectNode dims = json.createObjectNode();
        for (String dimension : AssessmentStore.SOFT_DIMENSIONS) {
            JsonNode value = dimensions.path(dimension);
            require(value.isIntegralNumber() && value.intValue() >= 0 && value.intValue() <= 100, "评分必须是 0–100 的整数");
            dims.put(dimension, value.intValue());
        }
        var catalog = TrainingEvidenceCatalog.fromSources(sources);
        JsonNode evidence = score.path("evidence");
        require(evidence.isArray() && !evidence.isEmpty() && evidence.size() <= 24, "评分缺少回答证据");
        ArrayNode normalizedEvidence = json.createArrayNode();
        Set<String> covered = new HashSet<>();
        for (JsonNode item : evidence) {
            require(item.size() == 2 && item.path("evidenceId").isTextual(), "证据项只能包含 dimension 与 evidenceId");
            String dimension = item.path("dimension").asText("");
            require(AssessmentStore.SOFT_DIMENSIONS.contains(dimension), "评分证据维度无效");
            TrainingEvidenceCatalog.Excerpt excerpt = catalog.get(item.path("evidenceId").asText());
            require(excerpt != null, "评分引用了不存在的回答片段");
            normalizedEvidence.addObject()
                    .put("dimension", dimension)
                    .put("evidenceId", item.path("evidenceId").asText())
                    .put("quote", excerpt.quote());
            covered.add(dimension);
        }
        require(covered.containsAll(mustCover), "部分维度缺少可核对的回答证据");
        String comment = score.path("comment").asText("").strip();
        require(!comment.isBlank() && comment.length() <= 2000, "评分缺少有效评语");
        JsonNode suggestions = score.path("suggestions");
        require(suggestions.isArray() && !suggestions.isEmpty() && suggestions.size() <= 5, "评分缺少改进建议");
        ObjectNode out = json.createObjectNode();
        out.set("dimensions", dims);
        out.set("evidence", normalizedEvidence);
        out.put("comment", comment);
        ArrayNode tips = out.putArray("suggestions");
        for (JsonNode suggestion : suggestions) {
            String tip = suggestion.asText("").strip();
            require(!tip.isBlank() && tip.length() <= 1000, "改进建议格式无效");
            tips.add(tip);
        }
        return out;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssessmentException(422, "ASSESSMENT_OUTPUT_INVALID", message);
        }
    }
}
