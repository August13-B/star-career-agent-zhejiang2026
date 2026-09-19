package org.example.web.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 序列化配置：把 64 位整数（Long/long）统一序列化为「字符串」。
 *
 * <p>背景：项目使用雪花算法生成 64 位 ID（约 2.3e17），超过 JavaScript 的
 * 安全整数范围（Number.MAX_SAFE_INTEGER = 2^53 ≈ 9.0e15）。
 * 后端若把 ID 作为 JSON 数字下发，浏览器 `JSON.parse` / `Number()` 会**静默丢精度**，
 * 前端再把这个被污染的 ID 回传保存时，就会命中
 * `Cannot add or update a child row: ... fk_stu_profile_user`。
 *
 * <p>改为字符串后，前端只要原样透传（不要 `Number(id)`）即可保证 ID 精确。
 * 反序列化方向无需特殊处理：Jackson 支持把数字字符串强制转换为 Long。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
