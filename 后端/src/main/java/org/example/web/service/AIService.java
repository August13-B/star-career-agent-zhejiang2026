package org.example.web.service;

import org.example.web.entity.Result;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AIService {

        /**
         * 发送POST请求
         */
        Result<?> sendPostRequest(Object requestData, String uri);

        /**
         * 发送GET请求
         */
        Result<?> sendGetRequest(String uri);

        /**
         * 异步发送POST请求
         */
        Mono<Result> sendPostRequestAsync(Object requestData, String uri);

        /**
         * 异步发送GET请求
         */
        Mono<Result> sendGetRequestAsync(String uri);

        /**
         * 普通对话（新AI服务器接口）
         * @param message 消息内容
         * @param temperature 温度参数
         * @return 结果
         */
        Result<?> chat(String message, Double temperature);

        /**
         * 流式输出对话（A02：转发百宝箱 WebSocket 事件流）
         * @param message 消息内容
         * @param temperature 温度参数（百宝箱侧由应用配置决定，保留兼容）
         * @param userId 本地用户ID（映射平台 userId，用于会话映射）
         * @param conversationId 本地对话ID（映射平台 sessionId/conversationId，落库支持续会话）
         * @return 流式响应（元素为 {"data":"..."} 的 JSON）
         */
        Flux<String> chatStream(String message, Double temperature, Long userId, Long conversationId);

        /**
         * 带图片的对话（新AI服务器接口）
         * @param message 消息内容
         * @param temperature 温度参数
         * @param imageUrl 图片URL
         * @return 结果
         */
        Result<?> chatWithImage(String message, Double temperature, String imageUrl);

        /**
         * 带图片的流式输出对话（新AI服务器接口）
         * @param message 消息内容
         * @param temperature 温度参数
         * @param imageUrl 图片URL
         * @return 流式响应（SSE事件流）
         */
        Flux<String> chatWithImageStream(String message, Double temperature, String imageUrl);
}
