package org.example.web.service.training;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 单题限时口径：从下发开始计时、含宽限、未配置即不限时。 */
class TrainingTimeLimitTest {
    private final LocalDateTime delivered = LocalDateTime.of(2026, 10, 5, 10, 0, 0);

    @Test void templateWithoutLimitNeverTimesOut() {
        assertNull(TrainingTimeLimit.remainingSeconds(delivered, delivered.plusHours(3), 0));
        assertFalse(TrainingTimeLimit.timedOut(delivered, delivered.plusHours(3), 0));
        assertFalse(TrainingTimeLimit.timedOut(delivered, delivered.plusHours(3), -1));
    }

    @Test void countsDownFromDeliveryMoment() {
        assertEquals(300L, TrainingTimeLimit.remainingSeconds(delivered, delivered, 300));
        assertEquals(1L, TrainingTimeLimit.remainingSeconds(delivered, delivered.plusSeconds(299), 300));
        assertEquals(-45L, TrainingTimeLimit.remainingSeconds(delivered, delivered.plusSeconds(345), 300));
    }

    @Test void graceAbsorbsAutoSubmitLatency() {
        assertFalse(TrainingTimeLimit.timedOut(delivered, delivered.plusSeconds(300), 300), "准点提交不算超时");
        assertFalse(TrainingTimeLimit.timedOut(delivered, delivered.plusSeconds(304), 300), "宽限内不算超时");
        assertTrue(TrainingTimeLimit.timedOut(delivered, delivered.plusSeconds(306), 300), "超出宽限即判超时");
    }

    @Test void undeliveredQuestionHasNoDeadline() {
        assertNull(TrainingTimeLimit.remainingSeconds(null, delivered, 300));
        assertFalse(TrainingTimeLimit.timedOut(null, delivered, 300));
    }
}
