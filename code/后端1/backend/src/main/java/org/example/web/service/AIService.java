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
         * 流式输出对话（新AI服务器接口）
         * @param message 消息内容
         * @param temperature 温度参数
         * @return 流式响应（SSE事件流）
         */
        Flux<String> chatStream(String message, Double temperature);

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
