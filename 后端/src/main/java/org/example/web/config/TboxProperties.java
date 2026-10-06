package org.example.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 百宝箱（Tbox）应用配置
 * <p>取值来自 后端/.env 的 TBOX_API_URL / TBOX_API_KEY / TBOX_AGENT_ID
 */
@Data
@Component("tboxProperties")
@ConfigurationProperties(prefix = "tbox")
public class TboxProperties {

    /** 应用基址（平台注入的 APP_API_URL，预览态域名会变，勿硬编码） */
    private String apiUrl;

    /** 报名所得密钥（inc-ak...） */
    private String apiKey;

    /** 报告类接口专用令牌：平台配置 REPORT_API_TOKEN 后需带 X-Report-Token（未配置则为空，不发送） */
    private String reportToken;

    /** 应用 ID（/api/tbox/session 返回的 appId） */
    private String agentId;

    /** 单次对话超时（秒），模型首响较慢，默认 30 */
    private int timeoutSeconds = 30;

    /** 职业报告（6 智能体串行）超时（秒），默认 180 */
    private int reportTimeoutSeconds = 180;

    /** 职业报告异步轮询间隔（秒） */
    private int reportPollSeconds = 4;

    /** HELLO 与 SEND_MESSAGE 之间的间隔（毫秒），实测需要短暂间隔 */
    private long helloDelayMillis = 500;

    /**
     * 对话通道：
     * <ul>
     *   <li>{@code http}（默认）—— 平台专用 SSE {@code POST /api/chat/stream}，纯文本对话</li>
     *   <li>{@code ws} —— 旧 WebSocket {@code /ws}（平台新接口未就绪时的兜底；图片对话始终走 WS）</li>
     * </ul>
     */
    private String chatChannel = "ws";

    /** 对话（HTTP SSE）超时（秒），含 RAG 检索空窗 */
    private int chatTimeoutSeconds = 180;

    // ── 职场训练：本应用内新增的三个训练接口（推荐；未配置则回退旧 WS 会话链路）──

    /** 模拟面试训练接口路径，如 /api/training/interview */
    private String trainingPathMockInterview;

    /** 跨岗位沟通训练接口路径，如 /api/training/communication */
    private String trainingPathCrossRole;

    /** AI 辅助办公训练接口路径，如 /api/training/office */
    private String trainingPathAiOffice;

    /** 训练接口令牌：平台配置 TRAINING_API_TOKEN 后需带 X-Training-Token（未配置则不发送） */
    private String trainingToken;

    /** 训练单次调用超时（秒），模型生成长文本较慢 */
    private int trainingTimeoutSeconds = 180;

    /**
     * 按场景取训练接口路径。
     *
     * @return 未配置或未知场景时返回 {@code null}（调用方回退到旧 WS 会话链路）
     */
    public String trainingPathFor(String scenario) {
        if (scenario == null) {
            return null;
        }
        return switch (scenario) {
            case "mock_interview" -> blankToNull(trainingPathMockInterview);
            case "cross_role_communication" -> blankToNull(trainingPathCrossRole);
            case "ai_assisted_office" -> blankToNull(trainingPathAiOffice);
            default -> null;
        };
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public boolean isConfigured() {
        return apiUrl != null && !apiUrl.isBlank();
    }

    /** https://xxx → wss://xxx/ws */
    public String webSocketUrl() {
        if (!isConfigured()) {
            return null;
        }
        String base = apiUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base.replaceFirst("^https://", "wss://")
                   .replaceFirst("^http://", "ws://") + "/ws";
    }
}
