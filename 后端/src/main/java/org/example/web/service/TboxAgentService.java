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
     * 同步对话（内部调用）：收集平台 WS 文本输出，返回完整纯文本。
     *
     * <p>用于「非流式」后端内部场景（如岗位图谱对比分析、兼容旧的同步接口）。
     * 平台不可用时返回空字符串。
     *
     * @param userId              本地用户ID（可为 null）
     * @param localConversationId 本地对话ID（可为 null，不落库会话映射）
     * @param message             用户消息
     */
    String chatSync(Long userId, Long localConversationId, String message);

    /**
     * 职业报告多智能体流式（平台专用 HTTP SSE 接口 {@code POST /api/report/stream}）
     *
     * <p>平台侧已按固定顺序串行编排 6 个智能体并逐段打 {@code agent} 标签，
     * 因此返回元素即平台原始帧（JSON 字符串）：
     * <ul>
     *   <li>{@code {"agent":"<key>","data":"<增量文本>"}}（按 key 分组拼接 = 完整章节）</li>
     *   <li>{@code {"done":true,"agents":[...],"reportName":"...","reportId":"..."}}</li>
     *   <li>{@code {"error":"文案"}}</li>
     * </ul>
     *
     * @param userId  本地用户ID（字符串形式传给平台，用于平台侧落库与"上一份报告"注入）
     * @param message 已拼好的提示词（账号画像上下文 + 用户本次诉求）
     */
    Flux<String> reportStream(Long userId, String message);

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
