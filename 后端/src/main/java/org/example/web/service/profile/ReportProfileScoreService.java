package org.example.web.service.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.example.web.entity.CareerReport;
import org.example.web.mapper.CareerReportMapper;
import org.example.web.service.TboxAgentService;
import org.springframework.stereotype.Service;

/**
 * 多智能体联合测评报告 → 能力画像（Stage 3）。
 *
 * <p>幂等：同一份报告只计分一次（在报告 JSON 里写 {@code profileScoredAt} 标记）。
 * 报告未完成（六段不全）或不属于当前用户时明确拒绝。
 * AI 调用在事务之外，落库交给 {@link ReportProfileOutcomeService}（本地短事务）。
 *
 * <p>职业星图**不参与计分**：它是本报告的可视化衍生视图，避免同一份测评被重复计分。
 */
@Service
@RequiredArgsConstructor
public class ReportProfileScoreService {

    private static final Set<String> KEYS = Set.of(
            "profile_analysis", "career_exploration", "goal_setting",
            "path_planning", "action_planning", "report_composition");
    private static final List<String> DIMENSIONS = List.of(
            "education", "internship", "professional", "certificate",
            "innovation", "learning", "pressure", "communication", "problem_solving", "teamwork");
    /** 依据最短长度（引用报告原文，避免"无依据打分"）。 */
    private static final int MIN_EVIDENCE = 6;

    private final CareerReportMapper reports;
    private final ReportProfileOutcomeService outcome;
    private final TboxAgentService ai;
    private final ObjectMapper json;

    public Map<String, Object> score(Long userId, Long reportId) {
        CareerReport report = reports.selectById(reportId);
        if (report == null || !userId.equals(report.getUserId())) {
            throw new ResumeParseException("测评报告不存在或无权访问");
        }
        ObjectNode document = content(report);
        if (!complete(report, document)) {
            throw new ResumeParseException("请先完成六个智能体的联合测评");
        }
        if (document.path("profileScoredAt").asText("").length() > 0) {
            Map<String, Object> already = new LinkedHashMap<>();
            already.put("status", "already_scored");
            already.put("reportId", String.valueOf(reportId));
            already.put("source", ProfileScorePolicy.SOURCE_REPORT);
            already.put("sourceLabel", ProfileChangeService.label(ProfileScorePolicy.SOURCE_REPORT));
            already.put("message", "这份测评已经同步过能力画像");
            return already;
        }

        String answer = ai.chatSync(userId, null, prompt(document.path("agents").toString()));
        Map<String, Integer> scores = new LinkedHashMap<>();
        Map<String, String> reasons = new LinkedHashMap<>();
        parse(answer, scores, reasons);
        if (scores.isEmpty()) {
            throw new ResumeParseException("这次没能从报告里读出可用于画像的结论，稍后可重试");
        }
        Map<String, Object> result = outcome.apply(userId, reportId, scores, reasons);
        markScored(report, document);
        return result;
    }

    /** 在报告 JSON 里打标（乐观并发：内容未被改动才写，避免覆盖正在生成的内容）。 */
    private void markScored(CareerReport report, ObjectNode document) {
        try {
            ObjectNode next = document.deepCopy();
            next.put("profileScoredAt", Instant.now().toString());
            reports.saveGraphIfUnchanged(report.getId(), report.getUserId(), report.getReportContent(),
                    json.writeValueAsString(next));
        } catch (Exception ignored) {
            // 打标失败不影响本次结果：下次调用会重新评分，但策略会限制变化幅度
        }
    }

    private void parse(String answer, Map<String, Integer> scores, Map<String, String> reasons) {
        if (answer == null || answer.indexOf('{') < 0 || answer.lastIndexOf('}') <= answer.indexOf('{')) {
            return;
        }
        JsonNode node;
        try {
            node = json.readTree(answer.substring(answer.indexOf('{'), answer.lastIndexOf('}') + 1));
        } catch (Exception e) {
            return;
        }
        JsonNode scoreNode = node.path("scores");
        JsonNode reasonNode = node.path("reasons");
        for (String dimension : DIMENSIONS) {
            String evidence = reasonNode.path(dimension).asText("").strip();
            if (!scoreNode.path(dimension).isNumber() || evidence.length() < MIN_EVIDENCE) {
                continue;   // 没有依据的维度不打分
            }
            scores.put(dimension, ProfileScorePolicy.bound(scoreNode.path(dimension).asInt()));
            reasons.put(dimension, evidence);
        }
    }

    private ObjectNode content(CareerReport report) {
        try {
            return (ObjectNode) json.readTree(report.getReportContent());
        } catch (Exception e) {
            throw new ResumeParseException("测评报告内容格式异常，请重新完成联合测评");
        }
    }

    private boolean complete(CareerReport report, ObjectNode document) {
        if (report.getStatus() == null || report.getStatus() < 2 || Integer.valueOf(1).equals(report.getIsDeleted())) {
            return false;
        }
        Set<String> found = new HashSet<>();
        for (JsonNode agent : document.path("agents")) {
            if (!agent.path("content").asText("").isBlank()) {
                found.add(agent.path("key").asText());
            }
        }
        return found.containsAll(KEYS);
    }

    private String prompt(String agents) {
        return """
                你是能力画像评估器。下面是一名学生的【六智能体联合测评报告】原文。
                只依据报告内容评估该学生在下面 10 个维度上的能力分值（0-100 整数），不得凭空拔高、不得编造。
                维度：education 学历背景、internship 实习经历、professional 专业技能、certificate 证书资质、
                innovation 创新能力、learning 学习能力、pressure 抗压能力、communication 沟通能力、
                problem_solving 问题解决、teamwork 团队协作。
                评分口径：50 = 报告未提供相关信息；60 = 与应届生平均水平相当；
                只有报告里有明确优势证据时才高于 60，最高 95。
                每个维度都必须给出依据：引用报告原文片段（至少 6 个字），报告里没有依据的维度就**不要输出该维度**。
                输出纯 JSON（不要解释、不要 markdown 代码块）：
                {"scores":{"education":0},"reasons":{"education":"报告原文中的依据"}}
                【报告原文】
                """ + agents;
    }
}
