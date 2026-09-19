package org.example.web.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.example.web.entity.Result;
import org.example.web.service.AIService;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AI 服务实现（A02 改造后）
 *
 * <p>国赛时期的自研 AI 服务（LangChain4j + PGVector，默认 127.0.0.1:8081）已退役，
 * 对话/多智能体能力全部由蚂蚁百宝箱（WebSocket / AG-UI）承担，统一入口为
 * {@link org.example.web.service.TboxAgentService}。
 *
 * <p>保留本类是为了兼容既有接口契约：流式返回元素仍为 {"data":"..."} 的 JSON。
 */
@Service
public class AIServiceImpl implements AIService {

    private static final String RETIRED_MSG =
            "该能力已随自研 AI 服务退役；文本对话请使用百宝箱通道（/ai-conversation/send-stream）。";

    private final org.example.web.service.TboxAgentService tboxAgentService;

    public AIServiceImpl(org.example.web.service.TboxAgentService tboxAgentService) {
        this.tboxAgentService = tboxAgentService;
    }

    @Override
    public Result<?> sendPostRequest(Object requestData, String uri) {
        return Result.error(RETIRED_MSG);
    }

    @Override
    public Result<?> sendGetRequest(String uri) {
        return Result.error(RETIRED_MSG);
    }

    @Override
    public Mono<Result> sendPostRequestAsync(Object requestData, String uri) {
        return Mono.just(Result.error(RETIRED_MSG));
    }

    @Override
    public Mono<Result> sendGetRequestAsync(String uri) {
        return Mono.just(Result.error(RETIRED_MSG));
    }

    @Override
    public Result<?> chat(String message, Double temperature) {
        // A02：文本对话转发百宝箱（同步收集 WS 输出），不再调用已退役的自研 AI 服务
        String text = tboxAgentService.chatSync(null, null, message);
        Map<String, Object> data = new HashMap<>();
        if (text == null || text.isBlank()) {
            data.put("response", "AI 服务暂无响应，请稍后重试（详见后端日志）。");
        } else {
            data.put("response", text);
        }
        return Result.success(data);
    }

    @Override
    public Flux<String> chatStream(String message, Double temperature, Long userId, Long conversationId) {
        // A02 改造：对话能力由蚂蚁百宝箱（WebSocket / AG-UI）承担
        // 返回元素仍为 {"data":"..."}，与既有转发契约、前端渲染保持一致（前端零改动）
        return tboxAgentService.chatStream(userId, conversationId, message);
    }

    @Override
    public Result<?> chatWithImage(String message, Double temperature, String imageUrl) {
        return Result.error("图片对话暂不可用：百宝箱文本通道不支持图片输入，请改用文字描述。");
    }

    @Override
    public Flux<String> chatWithImageStream(String message, Double temperature, String imageUrl) {
        Map<String, Object> chunk = new HashMap<>();
        chunk.put("data", "图片对话暂不可用：百宝箱文本通道不支持图片输入，请改用文字描述。");
        return Flux.just(toJson(chunk));
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(e.getKey()).append("\":\"")
                    .append(String.valueOf(e.getValue()).replace("\\", "\\\\").replace("\"", "\\\""))
                    .append('"');
        }
        return sb.append('}').toString();
    }
}
