package wwy.example.springboot.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.web.service.TboxAgentService;
import org.springframework.stereotype.Service;
import wwy.example.springboot.dto.JobCompareResult;
import wwy.example.springboot.entity.JobInfo;
import wwy.example.springboot.entity.JobPromotionGraph;
import wwy.example.springboot.entity.JobRequirementProfile;
import wwy.example.springboot.entity.JobTransferGraph;
import wwy.example.springboot.service.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobCompareServiceImpl implements JobCompareService {

    private final JobInfoService jobInfoService;
    private final JobRequirementProfileService profileService;
    private final JobPromotionGraphService jobPromotionGraphService;
    private final JobTransferGraphService jobTransferGraphService;
    /** A02：AI 能力统一走蚂蚁百宝箱（自研 AI 服务已退役） */
    private final TboxAgentService tboxAgentService;

    @Override
    public JobCompareResult compareWithGraph(Long newJobId) {
        // 1. 获取新岗位信息
        JobInfo newJob = jobInfoService.findById(newJobId);
        if (newJob == null) {
            throw new RuntimeException("新岗位不存在，id=" + newJobId);
        }
        Long profileId = newJob.getJobId();
        JobRequirementProfile newProfile = null;
        if (profileId != null) {
            newProfile = profileService.findById(profileId);
        }

        // 2. 获取所有已有岗位画像
        List<JobRequirementProfile> allProfiles = profileService.findAll();

        // 3. 获取晋升图谱和换岗图谱
        List<JobPromotionGraph> promotionGraphs = jobPromotionGraphService.findAll();
        List<JobTransferGraph> transferGraphs = jobTransferGraphService.findAll();

        // 4. 构建 AI 请求内容
        String prompt = buildComparePrompt(newJob, newProfile, allProfiles, promotionGraphs, transferGraphs);

        // 5. 调用百宝箱（同步收集 WS 输出；温度等由平台应用配置决定）

        // 6. 调用百宝箱（同步收集 WS 输出）
        String aiResponse = tboxAgentService.chatSync(null, null, prompt);

        log.info("=== AI 原始响应 ===");
        log.info(aiResponse);

        // 7. 清理响应文本
        String cleanedResponse = cleanResponse(aiResponse);
        if (cleanedResponse == null || cleanedResponse.trim().isEmpty()) {
            throw new RuntimeException("AI 返回内容为空（请确认百宝箱已配置且模型网关已开通）");
        }
        log.info("=== 清理后的响应 ===");
        log.info(cleanedResponse);

        // 8. 从文本中提取 JSON 对象（平台可能返回 Markdown 代码块/前后说明）
        String jsonText = extractJsonObject(cleanedResponse);
        if (jsonText == null) {
            throw new RuntimeException("AI 返回中未找到 JSON 对象：" + cleanedResponse);
        }

        // 9. 解析（兼容直接返回 {analysis, matchedJobs} 与旧的 {code, data} 两种结构）
        JSONObject outerJson;
        try {
            outerJson = JSONUtil.parseObj(jsonText);
        } catch (Exception e) {
            log.error("JSON 解析失败，原始响应: {}", cleanedResponse, e);
            throw new RuntimeException("AI 返回的 JSON 格式不正确: " + e.getMessage());
        }

        JSONObject dataJson = outerJson;
        Object dataObj = outerJson.get("data");
        if (dataObj != null) {
            if (dataObj instanceof JSONObject) {
                dataJson = (JSONObject) dataObj;
            } else if (dataObj instanceof String) {
                try {
                    dataJson = JSONUtil.parseObj((String) dataObj);
                } catch (Exception e) {
                    throw new RuntimeException("data 字段是字符串但无法解析为 JSON: " + dataObj);
                }
            } else {
                throw new RuntimeException("data 字段类型不支持: " + dataObj.getClass());
            }
        }
        log.info("=== 提取的 data JSON ===");
        log.info(dataJson.toStringPretty());

        // 11. 手动构造 JobCompareResult
        JobCompareResult result = new JobCompareResult();
        result.setNewJobId(newJobId);
        result.setAnalysis(dataJson.getStr("analysis"));

        // 12. 处理 matchedJobs 数组
        JSONArray matchedJobsArray = dataJson.getJSONArray("matchedJobs");
        List<JobCompareResult.MatchedJob> matchedJobs = new ArrayList<>();
        if (matchedJobsArray != null && !matchedJobsArray.isEmpty()) {
            for (Object itemObj : matchedJobsArray) {
                if (!(itemObj instanceof JSONObject)) {
                    log.warn("matchedJobs 中的元素不是 JSONObject，跳过: {}", itemObj);
                    continue;
                }
                JSONObject item = (JSONObject) itemObj;
                JobCompareResult.MatchedJob matched = new JobCompareResult.MatchedJob();
                // 处理 profileId
                Object profileIdVal = item.get("profileId");
                if (profileIdVal instanceof Number) {
                    matched.setProfileId(((Number) profileIdVal).longValue());
                } else if (profileIdVal instanceof String) {
                    try {
                        matched.setProfileId(Long.parseLong((String) profileIdVal));
                    } catch (NumberFormatException e) {
                        log.warn("profileId 字符串无法转换为 Long: {}", profileIdVal);
                        matched.setProfileId(null);
                    }
                } else {
                    matched.setProfileId(null);
                }
                matched.setPositionName(item.getStr("positionName"));
                // 处理 similarity
                Object simVal = item.get("similarity");
                if (simVal instanceof Number) {
                    matched.setSimilarity(((Number) simVal).doubleValue());
                } else if (simVal instanceof String) {
                    try {
                        matched.setSimilarity(Double.parseDouble((String) simVal));
                    } catch (NumberFormatException e) {
                        log.warn("similarity 字符串无法转换为 Double: {}", simVal);
                        matched.setSimilarity(null);
                    }
                } else {
                    matched.setSimilarity(null);
                }
                matchedJobs.add(matched);
            }
        }
        result.setMatchedJobs(matchedJobs);

        log.info("=== 解析成功，analysis 长度: {}, matchedJobs 数量: {}",
                result.getAnalysis() == null ? 0 : result.getAnalysis().length(),
                result.getMatchedJobs() == null ? 0 : result.getMatchedJobs().size());
        return result;
    }

    private String cleanResponse(String raw) {
        if (raw == null) return null;
        String cleaned = raw.trim();
        // 移除 BOM
        if (cleaned.startsWith("\uFEFF")) {
            cleaned = cleaned.substring(1);
        }
        // 移除 Markdown 代码块标记
        cleaned = cleaned.replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();
        // 移除零宽空格等不可见字符
        cleaned = cleaned.replaceAll("[\\u200B\\u200C\\u200D\\uFEFF]", "");
        return cleaned;
    }

    /**
     * 从自由文本中提取第一个完整的 JSON 对象。
     * <p>平台可能返回 Markdown 代码块或前后说明，需要截取 {...} 再解析。
     */
    private String extractJsonObject(String text) {
        if (text == null) {
            return null;
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return text.substring(start, end + 1);
    }

    private String buildComparePrompt(JobInfo newJob, JobRequirementProfile newProfile,
                                      List<JobRequirementProfile> allProfiles,
                                      List<JobPromotionGraph> promotionGraphs,
                                      List<JobTransferGraph> transferGraphs) {
        StringBuilder sb = new StringBuilder();
        sb.append("请分析新增岗位与现有岗位图谱的对比。\n\n");
        sb.append("【新增岗位信息】\n");
        sb.append("岗位名称：").append(newJob.getJobName()).append("\n");
        sb.append("公司：").append(newJob.getCompanyName()).append("\n");
        sb.append("行业：").append(newJob.getIndustry()).append("\n");
        sb.append("薪资范围：").append(newJob.getSalaryRange()).append("\n");
        sb.append("岗位描述：").append(newJob.getJobDetail()).append("\n");
        if (newProfile != null) {
            sb.append("职级：").append(newProfile.getLevel()).append("\n");
            sb.append("岗位分类：").append(newProfile.getCategory()).append("\n");
        }

        sb.append("\n【现有岗位画像列表】\n");
        for (JobRequirementProfile p : allProfiles) {
            sb.append("- ID:").append(p.getId())
                    .append(", 名称:").append(p.getPositionName())
                    .append(", 分类:").append(p.getCategory()).append("\n");
        }

        sb.append("\n【晋升图谱示例】\n");
        for (JobPromotionGraph g : promotionGraphs.stream().limit(5).toList()) {
            sb.append("主岗位:").append(g.getMainJobId())
                    .append(" -> 晋升岗位:").append(g.getPromotionJob1Id()).append("\n");
        }

        sb.append("\n【换岗图谱示例】\n");
        for (JobTransferGraph g : transferGraphs.stream().limit(5).toList()) {
            sb.append("主岗位:").append(g.getMainJobId())
                    .append(" -> 换岗岗位:").append(g.getTransferJob1Id()).append("\n");
        }

        sb.append("请按以下 JSON 格式返回分析结果。注意：在 analysis 文本中，**不要输出任何岗位 ID（如 ID:12345）**，只输出岗位名称。\n");
        sb.append("{\n");
        sb.append("  \"analysis\": \"文本分析内容\",\n");
        sb.append("  \"matchedJobs\": [\n");
        sb.append("    { \"profileId\": 123, \"positionName\": \"Java开发工程师\", \"similarity\": 0.85 }\n");
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }
}