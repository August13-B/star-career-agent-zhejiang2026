package org.example.web.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.example.web.entity.Result;
import org.example.web.service.AIService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AIServiceImpl implements AIService {
    private final WebClient aiWebClient;

    public AIServiceImpl(@Qualifier("aiWebClient") WebClient aiWebClient) {
        this.aiWebClient = aiWebClient;
    }

    @Override
    public Result<?> sendPostRequest(Object requestData, String uri) {
        return aiWebClient.post()
                .uri(uri)
                .bodyValue(requestData)
                .retrieve()
                .bodyToMono(Result.class)
                .block();
    }

    @Override
    public Result<?> sendGetRequest(String uri) {
        return aiWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(Result.class)
                .block();
    }

    @Override
    public Mono<Result> sendPostRequestAsync(Object requestData, String uri) {
        return aiWebClient.post()
                .uri(uri)
                .bodyValue(requestData)
                .retrieve()
                .bodyToMono(Result.class);
    }

    @Override
    public Mono<Result> sendGetRequestAsync(String uri) {
        return aiWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(Result.class);
    }

    @Override
    public Result<?> chat(String message, Double temperature) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("temperature", temperature);
        return sendPostRequest(body, "/chat");
    }

    @Override
    public Flux<String> chatStream(String message, Double temperature) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("temperature", temperature);
        // 使用SSE流式响应，AI服务器返回text/event-stream
        // 修复406错误：移除accept头，让WebClient自动处理
        return aiWebClient.post()
                .uri("/chat_with_flux")
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class);
    }

    @Override
    public Result<?> chatWithImage(String message, Double temperature, String imageUrl) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("temperature", temperature);
        body.put("image_url", imageUrl);
        return sendPostRequest(body, "/chat_with_picture_with_RAG");
    }

    @Override
    public Flux<String> chatWithImageStream(String message, Double temperature, String imageUrl) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("temperature", temperature);
        body.put("image_url", imageUrl);
        // 修复406错误：移除accept头，让WebClient自动处理
        return aiWebClient.post()
                .uri("/chat_with_picture_with_RAG_stream")
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class);
    }
}
