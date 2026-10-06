package org.example.web.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.web.config.TboxProperties;
import org.example.web.mapper.AiConversationMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TboxErrorFrameTest {
    @Test
    void textDeltasPreserveWhitespaceAndMarkdownBoundaries() throws Exception {
        var tbox = new TboxAgentServiceImpl(new TboxProperties(), mock(AiConversationMapper.class));
        var mapper = new ObjectMapper();
        var sink = reactor.core.publisher.Sinks.many().unicast().<String>onBackpressureBuffer();
        var frames = new java.util.ArrayList<String>();
        sink.asFlux().subscribe(frames::add);
        var chunks = java.util.List.of("###", " ", "职业建议", "\n\n", "-", " ", "学习 Vue", "\n");
        for (String delta : chunks) {
            org.springframework.test.util.ReflectionTestUtils.invokeMethod(tbox, "handleEvent",
                    mapper.writeValueAsString(java.util.Map.of("type", "TEXT_MESSAGE_CONTENT", "delta", delta)),
                    sink, new java.util.concurrent.atomic.AtomicBoolean(), 2L);
        }
        StringBuilder text = new StringBuilder();
        for (String frame : frames) text.append(mapper.readTree(frame).path("data").asText());
        assertEquals(String.join("", chunks), text.toString());
    }

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
