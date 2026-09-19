package org.example.web.service;

import reactor.core.publisher.Flux;

/**
 * 百宝箱（Tbox）应用对接服务
 *
 * <p>对话走平台 WebSocket（AG-UI 事件流），本服务负责：
 * <ul>
 *   <li>会话映射：本地 ai_conversation ↔ 平台 sessionId / conversationId（落库，可续会话）</li>
 *   <li>流式对话：WS 事件流 → Flux&lt;String&gt;（元素为 {"data":"..."} 的 JSON，兼容既有转发契约）</li>
 *   <li>记录平台 messageId / requestId（历史回捞与三方对账的前置）</li>
 * </ul>
 */
public interface TboxAgentService {

    /**
     * 流式对话（对外主入口）
     *
     * @param userId              本地用户ID（映射为平台 userId）
     * @param localConversationId 本地对话ID（映射为平台 sessionId + conversationId，落库）
     * @param message             用户消息
     * @return Flux，元素为 {"data":"增量文本"} 形式的 JSON 字符串
     */
    Flux<String> chatStream(Long userId, Long localConversationId, String message);

    /**
     * 职业报告多智能体流式（按段标记切分）
     *
     * <p>返回元素为结构化 JSON：{"agent":"<智能体key>","data":"<增量文本>"}；
     * 结束时额外返回一条 {"done":true,"agents":[...],"hasMarkers":true|false}
     */
    Flux<String> reportStream(Long userId, Long localConversationId, String message, org.example.web.service.impl.AgentMarkerParser parser);

    /** 记录本次运行的平台 ID（供保存消息时回填） */
    void rememberRunIds(Long localConversationId, String tboxMessageId, String tboxRequestId);

    /** 取出并清除本次运行的平台 ID */
    RunIds consumeRunIds(Long localConversationId);

    /**
     * 预留：拉取平台历史（GET /api/conversation/messages?format=raw）
     *
     * @return 平台返回的原始 JSON 字符串；失败返回错误说明
     */
    String fetchRawHistory(Long localConversationId, int pageSize);

    /** 平台运行标识（三方对账用） */
    record RunIds(String tboxMessageId, String tboxRequestId) {
    }
}
