package org.example.web.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * JSON 工具类（自带脏字符自动清理）
 * 自动清理：换行 \n、回车 \r、制表符 \t、各类控制字符
 */
@Component
public class JsonUtils {

    private static ObjectMapper mapper;

    public JsonUtils(ObjectMapper objectMapper) {
        mapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    // ====================== 核心：自动清理脏字符串 ======================
    private static String cleanJson(String jsonStr) {
        if (jsonStr == null) return null;
        // 1. 替换换行、回车、制表符为空格
        // 2. 移除 ASCII 0~31 控制字符（JSON 不允许出现）
        return jsonStr.replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ")
                .replaceAll("[\\x00-\\x1F\\x7F]", "");
    }

    // ====================== 工具方法（自动清理） ======================

    /**
     * JSON字符串 → JsonNode（自动清理脏字符）
     */
    public static JsonNode change(String jsonStr) {
        try {
            return mapper.readTree(cleanJson(jsonStr));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON转JsonNode失败：" + e.getMessage(), e);
        }
    }

    /**
     * JSON字符串 → Java对象（自动清理脏字符）
     */
    public static <T> T strToObj(String jsonStr, Class<T> clazz) {
        try {
            return mapper.readValue(cleanJson(jsonStr), clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON转对象失败：" + e.getMessage(), e);
        }
    }

    /**
     * JSON字符串 → 泛型（List/Map）（自动清理脏字符）
     */
    public static <T> T strToGeneric(String jsonStr, TypeReference<T> typeReference) {
        try {
            return mapper.readValue(cleanJson(jsonStr), typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON转泛型失败：" + e.getMessage(), e);
        }
    }

    /**
     * 对象 → JSON字符串
     */
    public static String objToStr(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("对象转JSON失败：" + e.getMessage(), e);
        }
    }
}