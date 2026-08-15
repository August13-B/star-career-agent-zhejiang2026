package wwy.example.springboot.service;

import wwy.example.springboot.common.Result;
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

}
