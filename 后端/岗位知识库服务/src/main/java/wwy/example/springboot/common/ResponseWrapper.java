package wwy.example.springboot.common;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "wwy.example.springboot.controller")
public class ResponseWrapper implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 如果返回值已经是 Result 类型，不再包装（避免异常处理器返回的 Result 被二次包装）
        return !returnType.getParameterType().isAssignableFrom(Result.class);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        // String 类型需要特殊处理（因为 StringHttpMessageConverter 无法转换 Result 对象）
        if (body instanceof String) {
            // 手动将 Result 转为 JSON 字符串
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.writeValueAsString(Result.success(body));
            } catch (JsonProcessingException e) {
                // 转换失败时返回错误信息
                return "{\"code\":500,\"message\":\"响应封装失败\"}";
            }
        }
        // 其他类型直接包装
        return Result.success(body);
    }
}