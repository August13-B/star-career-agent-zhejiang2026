package org.example.web.dto.response;


import lombok.Data;

/**
 * AI服务器返回的数据结构
 * 根据实际AI响应格式调整
 */
@Data
public class AIResponseData {
    /**
     * AI生成的文本
     */
    private String text;

    /**
     * 处理耗时（毫秒）
     */
    private Long processingTime;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 其他扩展字段
     */
    private Object extra;
}