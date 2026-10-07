package org.example.web.service.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.web.service.training.TrainingContentCipher;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 轮次视图与选题：选项在库里是 AES-GCM 密文。
 *
 * <p>回归背景：曾因「只 JSON 解析、未解密」导致前端**客观题没有选项**、提交校验失败、
 * 客观题维度分复算与证据目录拿不到选项。此测试锁死：加密存储 → 视图必须还原出选项，且**分值不下发前端**。
 */
class AssessmentStoreTest {
    private final ObjectMapper json = new ObjectMapper();
    private final TrainingContentCipher cipher = new TrainingContentCipher("assessment-key-1");
    private final AssessmentStore store = new AssessmentStore(null, json, cipher);

    private Map<String, Object> objectiveRow() {
        ArrayNode options = json.createArrayNode();
        options.addObject().put("text", "先把不一致的字段逐条列成清单，主动对齐口径").put("score", 4);
        options.addObject().put("text", "直接按文档调整前端适配，先跑通").put("score", 3);
        options.addObject().put("text", "自己加一层临时转换挡掉，不告诉别人").put("score", 2);
        options.addObject().put("text", "先不处理，等联调时看后端是否说明").put("score", 1);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 1L);
        row.put("ordinal", 1);
        row.put("question_no", 1);
        row.put("kind", "objective");
        row.put("dimension", "communication");
        row.put("follow_up", 0);
        row.put("question", cipher.encrypt("场景题：联调时发现字段口径不一致，你会怎么处理？"));
        row.put("options", cipher.encrypt(options.toString()));
        row.put("status", "asking");
        row.put("limit_seconds", 30);
        row.put("started_at", LocalDateTime.now());
        row.put("answer", null);
        row.put("chosen", null);
        return row;
    }

    @Test void turnViewExposesDecryptedOptionsWithoutScores() {
        Map<String, Object> view = store.turnView(objectiveRow(), false);
        Object options = view.get("options");
        assertNotNull(options, "客观题必须带选项（曾因漏解密导致前端没有选项）");
        assertEquals(4, ((List<?>) options).size());
        Map<?, ?> first = (Map<?, ?>) ((List<?>) options).get(0);
        assertEquals("先把不一致的字段逐条列成清单，主动对齐口径", first.get("text"));
        assertEquals(0, first.get("index"));
        assertFalse(first.containsKey("score"), "选项分值不能下发前端");
        assertEquals("场景题：联调时发现字段口径不一致，你会怎么处理？", view.get("question"), "题干同样需要解密");
    }

    @Test void optionsHandlesEncryptedPlainAndMissing() {
        JsonNode encrypted = store.options(objectiveRow());
        assertNotNull(encrypted, "加密选项应能解密并解析");
        assertEquals(4, encrypted.size());
        assertEquals(4, encrypted.get(0).path("score").asInt(), "分值仍保留在后端，供复算使用");

        JsonNode plain = store.options(Map.of("options", "[{\"text\":\"a\",\"score\":3}]"));
        assertNotNull(plain, "兼容未加密的历史数据");
        assertEquals(1, plain.size());

        assertNull(store.options(Map.of()));
        assertNull(store.options(null));
    }
}
