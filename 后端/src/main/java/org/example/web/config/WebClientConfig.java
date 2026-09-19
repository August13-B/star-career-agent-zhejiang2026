package org.example.web.config;


import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * 通用 HTTP 客户端配置（预留）
 *
 * <p>注意：国赛时期自研 AI 服务（LangChain4j + PGVector，默认 127.0.0.1:8081）已退役，
 * 文本对话/多智能体能力统一走蚂蚁百宝箱（WebSocket / AG-UI，见
 * {@link org.example.web.service.TboxAgentService}）。本 Bean 不再指向任何已退役地址，
 * 如后续需要调用其它 HTTP 服务，通过 {@code AI_SERVER_URL} 配置基址即可。
 */
@Configuration
public class WebClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebClientConfig.class);

    /** 预留的外部 HTTP 服务基址（默认空：不指向任何地址，避免误连已退役服务） */
    @Value("${ai.server.url:}")
    private String aiServerUrl;

    @Bean
    public WebClient aiWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl(aiServerUrl == null ? "" : aiServerUrl)
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(60))
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                ))
                .filter(loggingFilter())
                .build();
    }

    /**
     * 日志过滤器
     */
    private ExchangeFilterFunction loggingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            logger.info("发送请求到外部服务: {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }
}
