package org.example.web.tool;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VerifyCodeManager {
    private final Map<String, Map<String, Map<String, Object>>> storage = new ConcurrentHashMap<>();

    // 保存验证码
    public void saveCode(String sessionId, String codeType, String code) {
        Map<String, Object> codeInfo = Map.of(
                "验证码", code,
                "发送时间", System.currentTimeMillis()
        );

        storage.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                .put(codeType, codeInfo);
    }

    // 获取验证码
    public String getCode(String sessionId, String codeType) {
        Map<String, Object> codeInfo = storage.getOrDefault(sessionId, Map.of())
                .get(codeType);
        return codeInfo != null ? (String) codeInfo.get("验证码") : null;
    }
}