package org.example.web.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.web.config.TboxProperties;
import org.example.web.entity.AiConversation;
import org.example.web.entity.User;
import org.example.web.mapper.AiConversationMapper;
import org.example.web.mapper.UserMapper;
import org.example.web.service.AIService;
import org.example.web.service.TboxAgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConversationStreamingTest {
    private AIConversationServiceImpl service;
    private AiConversationMapper conversations;
    private AIService ai;
    private Sinks.Many<String> upstream;

    @BeforeEach
    void setUp() {
        service = new AIConversationServiceImpl();
        conversations = mock(AiConversationMapper.class);
        var users = mock(UserMapper.class);
        ai = mock(AIService.class);
        var properties = new TboxProperties();
        properties.setChatChannel("ws");
        var profile = mock(StudentProfileContextService.class);
        when(profile.build(1L, null, null)).thenReturn("测试画像");
        ReflectionTestUtils.setField(service, "aiConversationMapper", conversations);
        ReflectionTestUtils.setField(service, "userMapper", users);
        ReflectionTestUtils.setField(service, "aiService", ai);
        ReflectionTestUtils.setField(service, "tboxProperties", properties);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(service, "studentProfileContextService", profile);
        ReflectionTestUtils.setField(service, "tboxAgentService", mock(TboxAgentService.class));
        var conversation = new AiConversation();
        conversation.setId(2L);
        conversation.setUserId(1L);
        when(users.findById(1L)).thenReturn(new User());
        when(conversations.selectConversationById(2L)).thenReturn(conversation);
        when(conversations.selectMessagesByConversationId(2L)).thenReturn(List.of());
        upstream = Sinks.many().unicast().onBackpressureBuffer();
        when(ai.chatStream(anyString(), anyDouble(), eq(1L), eq(2L))).thenReturn(upstream.asFlux());
        when(ai.chatWithImageStream(anyString(), anyDouble(), anyString())).thenReturn(upstream.asFlux());
    }

    private Flux<String> response(boolean withImage) {
        return withImage
                ? service.sendMessageWithImageStream(1L, "hello", 1, 2L, 0.7, "https://example.test/image.png")
                : service.sendMessageStream(1L, "hello", 1, 2L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void pushesChunksBeforeCompletionAndThenPersistsReply(boolean withImage) {
        var received = new ArrayList<String>();
        var errors = new ArrayList<Throwable>();
        response(withImage).subscribe(received::add, errors::add);
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(Sinks.EmitResult.OK, upstream.tryEmitNext("{\"data\":\"你好\"}"));
        assertEquals(List.of("{\"data\":\"你好\"}"), received);
        verify(conversations, never()).insertMessage(argThat(m -> m.getMessageType() == 2));
        upstream.tryEmitNext("{\"data\":\"世界🙂\"}");
        upstream.tryEmitComplete();
        assertEquals(2, received.size());
        assertTrue(errors.isEmpty(), errors.toString());
        verify(conversations).insertMessage(argThat(m -> m.getMessageType() == 2
                && m.getContent().equals("{\"response\":\"你好世界🙂\"}")));
        verify(conversations).updateConversationStatus(2L, 2);
        if (withImage) verify(ai).chatWithImageStream(contains("测试画像"), eq(0.7), anyString());
        else verify(ai).chatStream(contains("测试画像"), eq(1.0), eq(1L), eq(2L));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void failedStreamDoesNotPersistPartialReplyAsComplete(boolean withImage) {
        var received = new ArrayList<String>();
        var errors = new ArrayList<Throwable>();
        response(withImage).subscribe(received::add, errors::add);
        upstream.tryEmitNext("partial");
        upstream.tryEmitError(new IllegalStateException("连接中断"));
        assertEquals(List.of("partial"), received);
        assertEquals(1, errors.size());
        verify(conversations, never()).insertMessage(argThat(m -> m.getMessageType() == 2));
        verify(conversations, never()).updateConversationStatus(2L, 2);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cancelledStreamDoesNotPersistPartialReplyAsComplete(boolean withImage) {
        var subscription = response(withImage).subscribe();
        upstream.tryEmitNext("partial");
        subscription.dispose();
        assertEquals(Sinks.EmitResult.FAIL_CANCELLED, upstream.tryEmitNext("tail"));
        verify(conversations, never()).insertMessage(argThat(m -> m.getMessageType() == 2));
        verify(conversations, never()).updateConversationStatus(2L, 2);
    }
}
