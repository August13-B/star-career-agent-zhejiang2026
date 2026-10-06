package org.example.web.service.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 客观题题库兜底（`data/ability-questions.json`）。
 *
 * <p>题库与百宝箱接口**语义同构、字段不同名**，这里做适配：
 * {@code dim→dimension}、{@code text→question}、{@code options[].t/s→options[].text/score}。
 * 题库的 {@code reverse} 已按题库说明预处理过，无需二次反转；选项分值**不下发前端**（由 Store 剥离）。
 */
@Component
public class AssessmentBank {
    private final ObjectMapper json;
    private final JsonNode root;

    public AssessmentBank(ObjectMapper json) throws IOException {
        this.json = json;
        try (var in = new ClassPathResource("data/ability-questions.json").getInputStream()) {
            this.root = json.readTree(in);
        }
    }

    /** 某个维度随机抽一题，返回与接口同构的 JSON；该维度无题则返回 null。 */
    public JsonNode draw(String dimension) {
        List<JsonNode> pool = new ArrayList<>();
        for (JsonNode question : root.path("questions")) {
            if (dimension.equals(question.path("dim").asText(""))) {
                pool.add(question);
            }
        }
        if (pool.isEmpty()) {
            return null;
        }
        JsonNode picked = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        ObjectNode node = json.createObjectNode();
        node.put("dimension", dimension);
        node.put("question", picked.path("text").asText(""));
        ArrayNode options = node.putArray("options");
        for (JsonNode option : picked.path("options")) {
            ObjectNode item = options.addObject();
            item.put("text", option.path("t").asText(""));
            item.put("score", option.path("s").asInt(1));
        }
        node.put("fromBank", true);
        return node;
    }

    public int questionCount() {
        return root.path("questions").size();
    }
}
