package org.example.web.service.grow;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 成长计划难度口径：太简单=5 / 中等=3 / 困难=1（沿用 effect_score，越高越轻松）。 */
class GrowCycleTest {

    @Test void difficultyScores() {
        assertEquals(5, GrowCycleService.DIFFICULTY_SCORE.get("太简单"));
        assertEquals(3, GrowCycleService.DIFFICULTY_SCORE.get("中等"));
        assertEquals(1, GrowCycleService.DIFFICULTY_SCORE.get("困难"));
        assertNull(GrowCycleService.DIFFICULTY_SCORE.get("很简单"));
        assertEquals(3, GrowCycleService.DIFFICULTY_SCORE.size());
    }

    @Test void difficultyLabel() {
        assertEquals("太简单", GrowCycleService.difficultyLabel(5));
        assertEquals("中等", GrowCycleService.difficultyLabel(3));
        assertEquals("困难", GrowCycleService.difficultyLabel(1));
        assertEquals("未选择", GrowCycleService.difficultyLabel(4));
        assertEquals("未选择", GrowCycleService.difficultyLabel(0));
    }
}
