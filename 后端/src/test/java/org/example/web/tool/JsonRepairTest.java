package org.example.web.tool;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 裸引号修复：真实踩坑样本（模型把评语里的强调词加了英文双引号）。 */
class JsonRepairTest {

    /** 取自真实训练评分返回：comment 里出现 ...风险。"缺证据"集中在：... 把 JSON 截断 */
    @Test void repairsBareQuotesInsideComment() {
        String broken = "{\"training_evaluation\":{\"comment\":\"能识别风险、安排优先级。\"缺证据\"集中在：无真实项目。\",\"total\":80}}";
        String fixed = JsonRepair.repairUnescapedQuotes(broken);
        assertTrue(fixed.contains("\\\"缺证据\\\""), "裸引号应被转义");
        assertFalse(fixed.contains("。\"缺证据\"集中"), "原样裸引号不应保留");
        // 修复后能被严格解析
        assertDoesNotThrow(() -> new com.fasterxml.jackson.databind.ObjectMapper().readTree(fixed));
    }

    @Test void keepsWellFormedJsonUntouched() {
        String ok = "{\"a\":\"b\",\"nested\":{\"c\":[\"d\",\"e\"]}}";
        assertEquals(ok, JsonRepair.repairUnescapedQuotes(ok));
        assertDoesNotThrow(() -> new com.fasterxml.jackson.databind.ObjectMapper().readTree(JsonRepair.repairUnescapedQuotes(ok)));
    }

    @Test void keepsEscapedQuotesIntact() {
        String escaped = "{\"a\":\"他说\\\"好\\\"\",\"b\":1}";
        assertEquals(escaped, JsonRepair.repairUnescapedQuotes(escaped));
        assertDoesNotThrow(() -> new com.fasterxml.jackson.databind.ObjectMapper().readTree(JsonRepair.repairUnescapedQuotes(escaped)));
    }

    @Test void handlesMultipleBareQuotesAndArrays() {
        String broken = "{\"suggestions\":[\"把\"会说\"变成\"做过\"\",\"补工程化\"]}";
        String fixed = JsonRepair.repairUnescapedQuotes(broken);
        assertDoesNotThrow(() -> new com.fasterxml.jackson.databind.ObjectMapper().readTree(fixed));
    }

    @Test void nullAndEmptyAreSafe() {
        assertNull(JsonRepair.repairUnescapedQuotes(null));
        assertEquals("", JsonRepair.repairUnescapedQuotes(""));
    }
}
