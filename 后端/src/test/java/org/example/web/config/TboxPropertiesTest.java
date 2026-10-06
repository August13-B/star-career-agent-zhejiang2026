package org.example.web.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 训练接口路由：按场景取路径；未配置/未知场景返回 null（调用方回退旧 WS 链路）。 */
class TboxPropertiesTest {

    private TboxProperties configured() {
        TboxProperties properties = new TboxProperties();
        properties.setTrainingPathMockInterview("/api/training/interview");
        properties.setTrainingPathCrossRole("  /api/training/communication  ");
        properties.setTrainingPathAiOffice("/api/training/office");
        return properties;
    }

    @Test void mapsEachScenarioToItsOwnInterface() {
        TboxProperties properties = configured();
        assertEquals("/api/training/interview", properties.trainingPathFor("mock_interview"));
        assertEquals("/api/training/communication", properties.trainingPathFor("cross_role_communication"), "两侧空白应被裁掉");
        assertEquals("/api/training/office", properties.trainingPathFor("ai_assisted_office"));
    }

    @Test void fallsBackToConversationChannelWhenNotConfigured() {
        TboxProperties properties = new TboxProperties();
        assertNull(properties.trainingPathFor("mock_interview"));
        assertNull(properties.trainingPathFor("cross_role_communication"));
        assertNull(properties.trainingPathFor("ai_assisted_office"));
    }

    @Test void blankAndUnknownScenariosNeverRouteToAnInterface() {
        TboxProperties properties = configured();
        properties.setTrainingPathAiOffice("   ");
        assertNull(properties.trainingPathFor("ai_assisted_office"), "仅空白视为未配置");
        assertNull(properties.trainingPathFor("unknown_scenario"));
        assertNull(properties.trainingPathFor(null));
    }
}
