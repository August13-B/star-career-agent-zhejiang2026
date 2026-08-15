package org.example.web.config;


import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebClientConfig.class);

    /**
     * 创建AI服务WebClient - 无需认证
     */
    @Bean
    public WebClient aiWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl("http://127.0.0.1:8081/api/chat") //AI服务器地址
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(300000))
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 50000000)
                ))
                .filter(loggingFilter()) // 只保留日志过滤器
                .build();
    }

    /**
     * 日志过滤器
     */
    private ExchangeFilterFunction loggingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            logger.info("🌐 发送请求到AI服务器: {} {}",
                    clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }
}
