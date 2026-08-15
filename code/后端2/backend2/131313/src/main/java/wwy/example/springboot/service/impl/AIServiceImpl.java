package wwy.example.springboot.service.impl;

import wwy.example.springboot.common.Result;
import wwy.example.springboot.service.AIService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class AIServiceImpl implements AIService {
    private final WebClient aiWebClient;

    public AIServiceImpl(@Qualifier("aiWebClient") WebClient aiWebClient) {
        this.aiWebClient = aiWebClient;
    }

    /**
     * 发送 POST 请求到 AI 服务（同步）
     * @param requestData 请求体，应为 Map 或可序列化对象，建议格式 {"message": "...", "temperature": 0.1}
     * @param uri 请求路径，例如 "/api/chat/chat"
     * @return Result 对象，成功时 data 为 AI 返回的文本内容
     */
    @Override
    public Result<?> sendPostRequest(Object requestData, String uri) {
        try {
            ResponseEntity<Map> responseEntity = aiWebClient.post()
                    .uri(uri)
                    .bodyValue(requestData)
                    .retrieve()
                    .toEntity(Map.class)
                    .block();

            if (responseEntity == null || responseEntity.getBody() == null) {
                return Result.error("AI service returned no response");
            }

            Map<?, ?> responseBody = responseEntity.getBody();
            // 提取 AI 返回的文本内容（兼容多种格式）
            String aiText = extractAiResponseText(responseBody);
            if (aiText == null || aiText.trim().isEmpty()) {
                return Result.error("AI 返回内容为空");
            }
            // 成功返回文本内容
            return Result.success(aiText);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("AI service request failed: " + e.getMessage());
        }
    }

    /**
     * 从 AI 响应中提取实际返回的文本内容
     * 支持以下格式：
     * - 直接返回字符串（响应体为纯文本）
     * - {"response": "文本内容"}
     * - {"code":200, "data": "文本内容"}
     * - {"code":200, "data": {"response": "文本内容"}}
     * - {"result": "文本内容"}
     * - {"message": "操作成功", "data": "文本内容"}（忽略 message）
     * @param responseBody AI 服务返回的 Map
     * @return 提取的文本，若无法提取则返回 null
     */
    private String extractAiResponseText(Map<?, ?> responseBody) {
        // 1. 如果有 data 字段
        if (responseBody.containsKey("data")) {
            Object data = responseBody.get("data");
            if (data instanceof String) {
                return (String) data;
            } else if (data instanceof Map) {
                Map<?, ?> dataMap = (Map<?, ?>) data;
                // 尝试获取 response 字段
                if (dataMap.containsKey("response")) {
                    return String.valueOf(dataMap.get("response"));
                }
                // 尝试获取第一个字符串值
                for (Object val : dataMap.values()) {
                    if (val instanceof String && !((String) val).isEmpty()) {
                        return (String) val;
                    }
                }
            }
        }
        // 2. 直接取 response 字段
        if (responseBody.containsKey("response")) {
            return String.valueOf(responseBody.get("response"));
        }
        // 3. 取 result 字段
        if (responseBody.containsKey("result")) {
            return String.valueOf(responseBody.get("result"));
        }
        // 4. 取 message 字段，但过滤掉“操作成功”等短提示（长度 > 20 且不包含“成功”）
        if (responseBody.containsKey("message")) {
            String msg = String.valueOf(responseBody.get("message"));
            if (msg.length() > 20 && !msg.contains("成功")) {
                return msg;
            }
        }
        // 5. 如果 Map 中只有一个字符串值且长度 > 50，返回它（可能是纯文本）
        for (Object val : responseBody.values()) {
            if (val instanceof String && ((String) val).length() > 50) {
                return (String) val;
            }
        }
        // 6. 如果所有字段都不匹配，尝试将整个 Map 转为 JSON 字符串（可能包含 AI 输出）
        try {
            String jsonStr = com.fasterxml.jackson.databind.ObjectMapper.class.newInstance().writeValueAsString(responseBody);
            if (jsonStr.length() > 50) {
                return jsonStr;
            }
        } catch (Exception ignored) {}
        return null;
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
}