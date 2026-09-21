package org.example.web.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.web.config.TboxProperties;
import org.example.web.mapper.AiConversationMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TboxErrorFrameTest {
    @Test
    void missingConfigurationAndUnsupportedImageUseErrorFrames() throws Exception {
        var tbox = new TboxAgentServiceImpl(new TboxProperties(), mock(AiConversationMapper.class));
        var mapper = new ObjectMapper();
        var missingConfig = mapper.readTree(tbox.chatStream(1L, 2L, "hello").blockFirst());
        assertTrue(missingConfig.has("error"));
        assertFalse(missingConfig.has("data"));
        var imageError = mapper.readTree(new AIServiceImpl(tbox)
                .chatWithImageStream("hello", 0.7, "https://example.test/image.png").blockFirst());
        assertTrue(imageError.has("error"));
        assertFalse(imageError.has("data"));
    }
}
