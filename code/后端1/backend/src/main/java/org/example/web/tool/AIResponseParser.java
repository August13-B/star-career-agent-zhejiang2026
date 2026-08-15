package org.example.web.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.web.entity.Result;

import java.util.HashMap;
import java.util.Map;

/**
 * AI响应解析工具类（简化版）
 */
public class AIResponseParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析Result中的response为Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> parse(Result result) {
        if (result == null || result.getCode() != 200 || result.getData() == null) {
            return new HashMap<>();
        }

        try {
            // 获取data对象并转为Map
            Map<String, Object> dataMap = objectMapper.convertValue(result.getData(), new TypeReference<Map<String, Object>>() {});

            // 获取response字段
            Object response = dataMap.get("response");
            if (response == null) {
                return new HashMap<>();
            }

            // 如果是String，解析为Map
            if (response instanceof String) {
                return objectMapper.readValue((String) response, new TypeReference<Map<String, String>>() {});
            }

            // 如果已经是Map，直接转换
            if (response instanceof Map) {
                Map<String, String> resultMap = new HashMap<>();
                ((Map<?, ?>) response).forEach((k, v) ->
                        resultMap.put(String.valueOf(k), String.valueOf(v)));
                return resultMap;
            }

        } catch (Exception e) {
            System.err.println("解析response失败: " + e.getMessage());
        }

        return new HashMap<>();
    }
}