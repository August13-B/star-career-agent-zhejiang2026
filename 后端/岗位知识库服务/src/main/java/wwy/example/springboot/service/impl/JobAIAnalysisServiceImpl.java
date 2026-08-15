package wwy.example.springboot.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wwy.example.springboot.common.IdCategoryConstants;
import wwy.example.springboot.dto.AiJobAnalysisResult;
import wwy.example.springboot.entity.*;
import wwy.example.springboot.service.*;
import wwy.example.springboot.tool.SnowIdCreater;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobAIAnalysisServiceImpl implements JobAIAnalysisService {

    private final JobInfoService jobInfoService;
    private final JobRequirementProfileService requirementProfileService;
    private final JobHardRequirementService hardRequirementService;
    private final JobSkillRequirementService skillRequirementService;
    private final JobSoftRequirementService softRequirementService;
    private final JobPromotionGraphService promotionGraphService;
    private final JobTransferGraphService transferGraphService;

    // 使用新的 AI 接口地址
    private static final String AI_API_URL = "http://57c42474b0ea.ofalias.net:50311/api/chat/chat";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void analyzeAndSave(Long jobInfoId) {
        // 1. 获取岗位基本信息
        JobInfo jobInfo = jobInfoService.findById(jobInfoId);
        if (jobInfo == null) {
            throw new RuntimeException("岗位不存在，id=" + jobInfoId);
        }

        // 2. 获取或创建对应的岗位画像
        Long profileId = getOrCreateRequirementProfile(jobInfo);
        if (profileId == null) {
            throw new RuntimeException("无法创建岗位画像");
        }

        // 3. 检查是否已分析过
        if (hardRequirementService.findByJobId(profileId) != null) {
            log.info("岗位 {} 已分析过，跳过", jobInfoId);
            return;
        }

        // 4. 构建 AI 请求 prompt
        String prompt = buildPrompt(jobInfo);

        // 5. 调用 AI 服务器（新接口）
        String aiResponseText = callAiApi(prompt);
        log.debug("AI 返回的原始文本: {}", aiResponseText);

        // 6. 解析 AI 返回的 JSON（可能包含 analysis 和 promotions 等）
        AiJobAnalysisResult result = parseAiResponse(aiResponseText);

        // 7. 保存各子表
        saveHardRequirement(profileId, result);
        saveSkillRequirement(profileId, result);
        saveSoftRequirement(profileId, result);
        savePromotionGraph(profileId, result);
        saveTransferGraph(profileId, result);

        log.info("岗位 {} 分析完成并保存", jobInfoId);
    }

    private Long getOrCreateRequirementProfile(JobInfo jobInfo) {
        if (jobInfo.getJobId() != null) {
            JobRequirementProfile existing = requirementProfileService.findById(jobInfo.getJobId());
            if (existing != null) {
                return jobInfo.getJobId();
            }
        }
        JobRequirementProfile profile = new JobRequirementProfile();
        profile.setId(SnowIdCreater.generateId(IdCategoryConstants.JOB_REQUIREMENT_PROFILE));
        profile.setPositionName(jobInfo.getJobName());
        profile.setCategory("技术");
        profile.setLevel(1);
        requirementProfileService.add(profile);
        jobInfo.setJobId(profile.getId());
        jobInfoService.update(jobInfo);
        return profile.getId();
    }

    private String buildPrompt(JobInfo jobInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个专业的岗位分析师。请根据以下岗位信息，提取出该岗位的硬实力要求、专业技能、软实力要求、可能的晋升路径以及换岗路径。\n\n");
        sb.append("【岗位信息】\n");
        sb.append("岗位名称：").append(jobInfo.getJobName()).append("\n");
        sb.append("公司：").append(jobInfo.getCompanyName()).append("\n");
        sb.append("行业：").append(jobInfo.getIndustry()).append("\n");
        sb.append("薪资范围：").append(jobInfo.getSalaryRange()).append("\n");
        sb.append("岗位描述：").append(jobInfo.getJobDetail()).append("\n");
        sb.append("公司详情：").append(jobInfo.getCompanyDetail()).append("\n");
        sb.append("\n请严格按照以下 JSON 格式返回结果，只返回 JSON，不要包含任何其他说明文字、注释或代码块标记。\n");
        sb.append("{\n");
        sb.append("  \"educationRequirement\": \"学历要求（例如：本科及以上）\",\n");
        sb.append("  \"internshipRequirement\": \"实习经历要求（例如：有相关实习经验优先）\",\n");
        sb.append("  \"professionalSkills\": [\"专业技能1\", \"专业技能2\"],\n");
        sb.append("  \"certificateRequirement\": [\"证书1\", \"证书2\"],\n");
        sb.append("  \"innovationAbility\": \"创新能力要求描述\",\n");
        sb.append("  \"learningAbility\": \"学习能力要求描述\",\n");
        sb.append("  \"pressureResistance\": \"抗压能力要求描述\",\n");
        sb.append("  \"communicationAbility\": \"沟通能力要求描述\",\n");
        sb.append("  \"problemSolving\": \"问题解决能力要求描述\",\n");
        sb.append("  \"teamworkAbility\": \"团队协作能力要求描述\",\n");
        sb.append("  \"promotions\": [\n");
        sb.append("    { \"desc\": \"晋升岗位1描述\", \"skillDiff\": \"技能差异\", \"experience\": \"经验要求\", \"learningCycle\": 6 },\n");
        sb.append("    { \"desc\": \"晋升岗位2描述\", \"skillDiff\": \"技能差异\", \"experience\": \"经验要求\", \"learningCycle\": 12 }\n");
        sb.append("  ],\n");
        sb.append("  \"transfers\": [\n");
        sb.append("    { \"desc\": \"换岗岗位1描述\", \"skillDiff\": \"技能差异\", \"education\": \"学历要求\", \"experience\": \"经验要求\", \"learningCycle\": 4, \"difficulty\": 2 },\n");
        sb.append("    { \"desc\": \"换岗岗位2描述\", \"skillDiff\": \"技能差异\", \"education\": \"学历要求\", \"experience\": \"经验要求\", \"learningCycle\": 6, \"difficulty\": 3 }\n");
        sb.append("  ]\n");
        sb.append("}\n");
        sb.append("注意：promotions 数组最多5个元素，transfers 数组最多8个元素。如果没有则返回空数组。");
        return sb.toString();
    }

    /**
     * 调用 AI 接口（适配 /api/chat/chat）
     * 请求体：{"message": prompt, "temperature": 0.1}
     * 响应格式：{"code":10001, "message":"操作成功", "data": "AI生成的JSON字符串" 或 {...}}
     */
    private String callAiApi(String prompt) {
        JSONObject body = new JSONObject();
        body.set("message", prompt);
        body.set("temperature", 0.1);
        String jsonBody = body.toString();
        log.debug("发送给 AI 的请求: {}", jsonBody);

        try (HttpResponse response = HttpRequest.post(AI_API_URL)
                .header("Content-Type", "application/json")
                .body(jsonBody)
                .execute()) {
            if (response.isOk()) {
                String responseBody = response.body();
                log.debug("AI 响应原始内容: {}", responseBody);
                // 解析外层 JSON
                JSONObject outerJson = JSONUtil.parseObj(responseBody);
                int code = outerJson.getInt("code");
                if (code != 200 && code != 10001) {
                    throw new RuntimeException("AI 返回错误：" + outerJson.getStr("message"));
                }
                Object dataObj = outerJson.get("data");
                if (dataObj == null) {
                    throw new RuntimeException("AI 返回的 data 字段为空");
                }
                // data 可能是字符串（包含 JSON），也可能是直接的对象
                String dataStr;
                if (dataObj instanceof String) {
                    dataStr = (String) dataObj;
                } else {
                    dataStr = JSONUtil.toJsonStr(dataObj);
                }
                log.debug("提取的 data 内容: {}", dataStr);
                return dataStr;
            } else {
                throw new RuntimeException("AI 服务调用失败，状态码：" + response.getStatus());
            }
        }
    }

    private AiJobAnalysisResult parseAiResponse(String aiResponse) {
        // 清理可能的 Markdown 标记和前后空白
        String jsonStr = aiResponse.replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
        // 如果仍然不是以 { 开头，尝试找到第一个 {
        int firstBrace = jsonStr.indexOf('{');
        if (firstBrace > 0) {
            jsonStr = jsonStr.substring(firstBrace);
        }
        int lastBrace = jsonStr.lastIndexOf('}');
        if (lastBrace < jsonStr.length() - 1) {
            jsonStr = jsonStr.substring(0, lastBrace + 1);
        }
        try {
            return JSONUtil.toBean(jsonStr, AiJobAnalysisResult.class);
        } catch (Exception e) {
            log.error("解析 AI 响应失败，清理后的 JSON 字符串: {}", jsonStr, e);
            // 降级：返回空对象，避免整体失败
            return new AiJobAnalysisResult();
        }
    }

    // 以下 save 方法保持不变，省略...
    private void saveHardRequirement(Long profileId, AiJobAnalysisResult result) {
        JobHardRequirement hard = new JobHardRequirement();
        hard.setJobId(profileId);
        hard.setEducationRequirement(result.getEducationRequirement());
        hard.setInternshipRequirement(result.getInternshipRequirement());
        hardRequirementService.add(hard);
    }

    private void saveSkillRequirement(Long profileId, AiJobAnalysisResult result) {
        JobSkillRequirement skill = new JobSkillRequirement();
        skill.setJobId(profileId);
        skill.setProfessionalSkill(JSONUtil.toJsonStr(result.getProfessionalSkills()));
        skill.setCertificateRequirement(JSONUtil.toJsonStr(result.getCertificateRequirement()));
        skillRequirementService.add(skill);
    }

    private void saveSoftRequirement(Long profileId, AiJobAnalysisResult result) {
        JobSoftRequirement soft = new JobSoftRequirement();
        soft.setJobId(profileId);
        soft.setInnovationAbility(result.getInnovationAbility());
        soft.setLearningAbility(result.getLearningAbility());
        soft.setPressureResistance(result.getPressureResistance());
        soft.setCommunicationAbility(result.getCommunicationAbility());
        soft.setProblemSolving(result.getProblemSolving());
        soft.setTeamworkAbility(result.getTeamworkAbility());
        softRequirementService.add(soft);
    }

    private void savePromotionGraph(Long profileId, AiJobAnalysisResult result) {
        JobPromotionGraph graph = new JobPromotionGraph();
        graph.setMainJobId(profileId);

        List<AiJobAnalysisResult.PromotionJob> promotions = result.getPromotions();
        if (promotions != null && !promotions.isEmpty()) {
            for (int i = 0; i < Math.min(promotions.size(), 5); i++) {
                AiJobAnalysisResult.PromotionJob p = promotions.get(i);
                Long targetProfileId = findOrCreateProfile(p.getDesc(), null, 1);
                if (targetProfileId == null) continue;

                switch (i) {
                    case 0:
                        graph.setPromotionJob1Id(targetProfileId);
                        graph.setPromotionJob1Desc(p.getDesc());
                        graph.setPromotionJob1SkillDiff(p.getSkillDiff());
                        graph.setPromotionJob1Experience(p.getExperience());
                        graph.setPromotionJob1LearningCycle(p.getLearningCycle());
                        break;
                    case 1:
                        graph.setPromotionJob2Id(targetProfileId);
                        graph.setPromotionJob2Desc(p.getDesc());
                        graph.setPromotionJob2SkillDiff(p.getSkillDiff());
                        graph.setPromotionJob2Experience(p.getExperience());
                        graph.setPromotionJob2LearningCycle(p.getLearningCycle());
                        break;
                    case 2:
                        graph.setPromotionJob3Id(targetProfileId);
                        graph.setPromotionJob3Desc(p.getDesc());
                        graph.setPromotionJob3SkillDiff(p.getSkillDiff());
                        graph.setPromotionJob3Experience(p.getExperience());
                        graph.setPromotionJob3LearningCycle(p.getLearningCycle());
                        break;
                    case 3:
                        graph.setPromotionJob4Id(targetProfileId);
                        graph.setPromotionJob4Desc(p.getDesc());
                        graph.setPromotionJob4SkillDiff(p.getSkillDiff());
                        graph.setPromotionJob4Experience(p.getExperience());
                        graph.setPromotionJob4LearningCycle(p.getLearningCycle());
                        break;
                    case 4:
                        graph.setPromotionJob5Id(targetProfileId);
                        graph.setPromotionJob5Desc(p.getDesc());
                        graph.setPromotionJob5SkillDiff(p.getSkillDiff());
                        graph.setPromotionJob5Experience(p.getExperience());
                        graph.setPromotionJob5LearningCycle(p.getLearningCycle());
                        break;
                    default:
                        break;
                }
            }
        }
        promotionGraphService.add(graph);
    }

    private void saveTransferGraph(Long profileId, AiJobAnalysisResult result) {
        JobTransferGraph graph = new JobTransferGraph();
        graph.setMainJobId(profileId);

        List<AiJobAnalysisResult.TransferJob> transfers = result.getTransfers();
        if (transfers != null && !transfers.isEmpty()) {
            for (int i = 0; i < Math.min(transfers.size(), 8); i++) {
                AiJobAnalysisResult.TransferJob t = transfers.get(i);
                Long targetProfileId = findOrCreateProfile(t.getDesc(), null, 1);
                if (targetProfileId == null) continue;

                switch (i) {
                    case 0:
                        graph.setTransferJob1Id(targetProfileId);
                        graph.setTransferJob1Desc(t.getDesc());
                        graph.setTransferJob1SkillDiff(t.getSkillDiff());
                        graph.setTransferJob1Education(t.getEducation());
                        graph.setTransferJob1Experience(t.getExperience());
                        graph.setTransferJob1LearningCycle(t.getLearningCycle());
                        graph.setTransferJob1Difficulty(t.getDifficulty());
                        break;
                    case 1:
                        graph.setTransferJob2Id(targetProfileId);
                        graph.setTransferJob2Desc(t.getDesc());
                        graph.setTransferJob2SkillDiff(t.getSkillDiff());
                        graph.setTransferJob2Education(t.getEducation());
                        graph.setTransferJob2Experience(t.getExperience());
                        graph.setTransferJob2LearningCycle(t.getLearningCycle());
                        graph.setTransferJob2Difficulty(t.getDifficulty());
                        break;
                    case 2:
                        graph.setTransferJob3Id(targetProfileId);
                        graph.setTransferJob3Desc(t.getDesc());
                        graph.setTransferJob3SkillDiff(t.getSkillDiff());
                        graph.setTransferJob3Education(t.getEducation());
                        graph.setTransferJob3Experience(t.getExperience());
                        graph.setTransferJob3LearningCycle(t.getLearningCycle());
                        graph.setTransferJob3Difficulty(t.getDifficulty());
                        break;
                    case 3:
                        graph.setTransferJob4Id(targetProfileId);
                        graph.setTransferJob4Desc(t.getDesc());
                        graph.setTransferJob4SkillDiff(t.getSkillDiff());
                        graph.setTransferJob4Education(t.getEducation());
                        graph.setTransferJob4Experience(t.getExperience());
                        graph.setTransferJob4LearningCycle(t.getLearningCycle());
                        graph.setTransferJob4Difficulty(t.getDifficulty());
                        break;
                    case 4:
                        graph.setTransferJob5Id(targetProfileId);
                        graph.setTransferJob5Desc(t.getDesc());
                        graph.setTransferJob5SkillDiff(t.getSkillDiff());
                        graph.setTransferJob5Education(t.getEducation());
                        graph.setTransferJob5Experience(t.getExperience());
                        graph.setTransferJob5LearningCycle(t.getLearningCycle());
                        graph.setTransferJob5Difficulty(t.getDifficulty());
                        break;
                    case 5:
                        graph.setTransferJob6Id(targetProfileId);
                        graph.setTransferJob6Desc(t.getDesc());
                        graph.setTransferJob6SkillDiff(t.getSkillDiff());
                        graph.setTransferJob6Education(t.getEducation());
                        graph.setTransferJob6Experience(t.getExperience());
                        graph.setTransferJob6LearningCycle(t.getLearningCycle());
                        graph.setTransferJob6Difficulty(t.getDifficulty());
                        break;
                    case 6:
                        graph.setTransferJob7Id(targetProfileId);
                        graph.setTransferJob7Desc(t.getDesc());
                        graph.setTransferJob7SkillDiff(t.getSkillDiff());
                        graph.setTransferJob7Education(t.getEducation());
                        graph.setTransferJob7Experience(t.getExperience());
                        graph.setTransferJob7LearningCycle(t.getLearningCycle());
                        graph.setTransferJob7Difficulty(t.getDifficulty());
                        break;
                    case 7:
                        graph.setTransferJob8Id(targetProfileId);
                        graph.setTransferJob8Desc(t.getDesc());
                        graph.setTransferJob8SkillDiff(t.getSkillDiff());
                        graph.setTransferJob8Education(t.getEducation());
                        graph.setTransferJob8Experience(t.getExperience());
                        graph.setTransferJob8LearningCycle(t.getLearningCycle());
                        graph.setTransferJob8Difficulty(t.getDifficulty());
                        break;
                    default:
                        break;
                }
            }
        }
        transferGraphService.add(graph);
    }

    private Long findOrCreateProfile(String positionName, String category, Integer level) {
        if (positionName == null || positionName.trim().isEmpty()) {
            log.warn("岗位名称为空，无法创建或查询画像");
            return null;
        }
        JobRequirementProfile existing = requirementProfileService.findByPositionNameExact(positionName);
        if (existing != null) {
            return existing.getId();
        }
        JobRequirementProfile newProfile = new JobRequirementProfile();
        newProfile.setId(SnowIdCreater.generateId(IdCategoryConstants.JOB_REQUIREMENT_PROFILE));
        newProfile.setPositionName(positionName);
        newProfile.setCategory(category != null ? category : "通用");
        newProfile.setLevel(level != null ? level : 1);
        newProfile.setHardWeight(BigDecimal.valueOf(30.00));
        newProfile.setSkillWeight(BigDecimal.valueOf(40.00));
        newProfile.setSoftWeight(BigDecimal.valueOf(30.00));
        requirementProfileService.add(newProfile);
        log.info("创建新岗位画像: {} , ID={}", positionName, newProfile.getId());
        return newProfile.getId();
    }

    /**
     * 批量生成多个岗位的换岗路径，确保每个岗位至少有2条换岗路径
     * @param jobInfoIds 岗位信息ID列表（至少5个）
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchGenerateTransferPaths(List<Long> jobInfoIds) {
        if (jobInfoIds == null || jobInfoIds.size() < 5) {
            throw new RuntimeException("至少需要提供5个岗位ID");
        }
        for (Long jobInfoId : jobInfoIds) {
            // 尝试分析，如果换岗路径少于2条，则重试一次
            boolean success = false;
            for (int retry = 0; retry < 2; retry++) {
                try {
                    analyzeAndSave(jobInfoId);
                    // 检查生成的换岗路径数量
                    Long profileId = jobInfoService.findById(jobInfoId).getJobId();
                    List<JobTransferGraph> graphs = transferGraphService.findByMainJobId(profileId);
                    if (graphs != null && !graphs.isEmpty()) {
                        // 统计该岗位的总换岗方向数（所有 transfer_jobX_id 非空的数量）
                        int transferCount = countTransferDirections(graphs);
                        if (transferCount >= 2) {
                            success = true;
                            break;
                        } else {
                            log.warn("岗位 {} 的换岗路径不足2条（实际{}条），重试第{}次", jobInfoId, transferCount, retry+1);
                            // 删除本次生成的图谱，重新分析
                            for (JobTransferGraph g : graphs) {
                                transferGraphService.deleteById(g.getId());
                            }
                        }
                    } else {
                        log.warn("岗位 {} 未生成任何换岗图谱，重试第{}次", jobInfoId, retry+1);
                    }
                } catch (Exception e) {
                    log.error("岗位 {} 分析失败，重试第{}次", jobInfoId, retry+1, e);
                }
            }
            if (!success) {
                // 如果重试后仍不满足，则手动补充默认换岗路径（可选）
                supplementDefaultTransferPaths(jobInfoId);
            }
        }
    }

    /**
     * 统计一个岗位的换岗路径数量（从图谱表中读取）
     */
    private int countTransferDirections(List<JobTransferGraph> graphs) {
        if (graphs == null || graphs.isEmpty()) return 0;
        int count = 0;
        for (JobTransferGraph g : graphs) {
            if (g.getTransferJob1Id() != null) count++;
            if (g.getTransferJob2Id() != null) count++;
            if (g.getTransferJob3Id() != null) count++;
            if (g.getTransferJob4Id() != null) count++;
            if (g.getTransferJob5Id() != null) count++;
            if (g.getTransferJob6Id() != null) count++;
            if (g.getTransferJob7Id() != null) count++;
            if (g.getTransferJob8Id() != null) count++;
        }
        return count;
    }

    /**
     * 补充默认换岗路径（当 AI 生成不足时使用）
     */
    private void supplementDefaultTransferPaths(Long jobInfoId) {
        JobInfo jobInfo = jobInfoService.findById(jobInfoId);
        if (jobInfo == null) return;
        Long profileId = jobInfo.getJobId();
        if (profileId == null) return;

        // 检查已有路径数量
        List<JobTransferGraph> existing = transferGraphService.findByMainJobId(profileId);
        int currentCount = countTransferDirections(existing);
        if (currentCount >= 2) return;

        // 创建补充的换岗路径（例如转向“通用管理岗”或“技术专家岗”）
        JobTransferGraph supplement = new JobTransferGraph();
        supplement.setMainJobId(profileId);
        // 根据缺失数量填充
        if (currentCount == 0) {
            // 添加两条默认路径
            supplement.setTransferJob1Id(findOrCreateProfile("通用管理岗", "管理", 2));
            supplement.setTransferJob1Desc("转向管理岗位，负责团队协调与项目推进");
            supplement.setTransferJob1SkillDiff("需要提升沟通、计划、决策能力");
            supplement.setTransferJob1Education("本科及以上");
            supplement.setTransferJob1Experience("3年以上相关经验");
            supplement.setTransferJob1LearningCycle(6);
            supplement.setTransferJob1Difficulty(3);

            supplement.setTransferJob2Id(findOrCreateProfile("技术专家岗", "技术", 3));
            supplement.setTransferJob2Desc("深耕技术领域，成为资深技术专家");
            supplement.setTransferJob2SkillDiff("需要深入学习核心技术及架构设计");
            supplement.setTransferJob2Education("本科及以上");
            supplement.setTransferJob2Experience("5年以上开发经验");
            supplement.setTransferJob2LearningCycle(12);
            supplement.setTransferJob2Difficulty(4);
        } else if (currentCount == 1) {
            // 仅补充一条
            supplement.setTransferJob1Id(findOrCreateProfile("横向拓展岗", "通用", 2));
            supplement.setTransferJob1Desc("跨职能拓展，学习产品/运营知识");
            supplement.setTransferJob1SkillDiff("需要补充业务知识");
            supplement.setTransferJob1Education("本科");
            supplement.setTransferJob1Experience("2年以上");
            supplement.setTransferJob1LearningCycle(4);
            supplement.setTransferJob1Difficulty(2);
        }
        transferGraphService.add(supplement);
        log.info("为岗位 {} 补充了默认换岗路径", jobInfoId);
    }
}