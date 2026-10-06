package org.example.web.service.training;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.Consumer;

/**
 * 职场训练的场景智能体网关。
 *
 * <p>当前实现有两条链路，由配置决定走哪条（见 {@code TboxProperties.trainingPathFor}）：
 * <ul>
 *   <li><b>场景训练接口</b>（推荐）：本应用内新增的 {@code POST /api/training/{interview|communication|office}}，
 *       一次性 JSON，{@code mode=ask → {"reply":…}}、{@code mode=evaluate → {"training_evaluation":…}}</li>
 *   <li><b>旧会话链路</b>（兜底）：{@code /api/tbox/session} + {@code /api/conversation/create} + WS {@code /ws} 流式事件</li>
 * </ul>
 */
public interface ScenarioAgentGateway {

    /**
     * 一次训练交互的上下文。
     *
     * @param scenario  场景：{@code mock_interview} / {@code cross_role_communication} / {@code ai_assisted_office}（决定调用哪个训练接口）
     * @param operation 操作：{@code evaluate}=评分，其余（如 {@code turn}）=提问/追问
     * @param namespace 隔离标识（旧会话链路用作用户维度，保证每次执行互不共享上下文）
     * @param prompt    后端冻结的完整任务指令（含阶段/身份/题目/材料/历史；评分时含冻结证据目录）
     */
    record Request(String scenario, String operation, String namespace, String prompt) {
    }

    /** @param text 提问/追问的纯文本，或评分时的原始 JSON 文本；@param score 平台直接给出的结构化评分（可空） */
    record Output(String text, JsonNode score) {
    }

    Output execute(Request request, Consumer<String> onText);
}
