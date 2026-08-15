package org.example.web.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.web.entity.Result;
import org.example.web.entity.StudentAbility;
import org.example.web.entity.StudentAbilityScore;
import org.example.web.entity.StudentProfile;
import org.example.web.service.AIAnalysisService;
import org.example.web.service.AIService;
import org.example.web.service.StudentAbilityScoreService;
import org.example.web.service.StudentAbilityService;
import org.example.web.service.StudentProfileService;
import org.example.web.tool.InPutGiveAI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AIAnalysisServiceImpl implements AIAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(AIAnalysisServiceImpl.class);

    // 注入项目现有服务
    private final StudentProfileService studentProfileService;
    private final StudentAbilityService studentAbilityService;
    private final AIService aiService;
    private final StudentAbilityScoreService studentAbilityScoreService;

    // AI服务请求路径
    private static final String AI_REQUEST_URI = "/only_chat";

    // 固定核心Prompt，用户的message会补充到这里
    private static final String AI_ANALYSIS_CORE_PROMPT = "请你根据提供的学生画像和能力维度信息，对学生的各项能力进行专业评分，严格按照以下要求输出：\n" +
            "1. 评分维度包含：educationScore（学历背景评分，0-100整数）、internshipScore（实习经历评分，0-100整数）、professionalScore（专业技能评分，0-100整数）、certificateScore（证书资质评分，0-100整数）、innovationScore（创新能力评分，0-100整数）、learningScore（学习能力评分，0-100整数）、pressureScore（抗压能力评分，0-100整数）、communicationScore（沟通能力评分，0-100整数）、problemSolvingScore（问题解决能力评分，0-100整数）、teamworkScore（团队协作能力评分，0-100整数）、totalScore（综合总分，0-100整数）\n" +
            "2. 输出内容必须是纯JSON格式，禁止包含任何markdown、解释性文字、多余符号，JSON结构严格如下：\n" +
            "{\n" +
            "  \"educationScore\": 数字,\n" +
            "  \"internshipScore\": 数字,\n" +
            "  \"professionalScore\": 数字,\n" +
            "  \"certificateScore\": 数字,\n" +
            "  \"innovationScore\": 数字,\n" +
            "  \"learningScore\": 数字,\n" +
            "  \"pressureScore\": 数字,\n" +
            "  \"communicationScore\": 数字,\n" +
            "  \"problemSolvingScore\": 数字,\n" +
            "  \"teamworkScore\": 数字,\n" +
            "  \"totalScore\": 数字,\n" +
            "  \"scoreComment\": \"综合评价评语，200字以内\"\n" +
            "}\n" +
            "3. 评分必须客观贴合学生实际情况，评语专业有针对性，仅输出JSON，禁止其他内容。\n" +
            "4. 用户补充需求（如有）请优先参考：";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<List<StudentAbilityScore>> analyzeAndSaveAbilityScore(Long userId, String userMessage, Float temperature) {
        try {
            // 1. 入参合法性校验
            if (userId == null || userId <= 0) {
                logger.warn("【AI能力分析】用户ID参数非法，userId：{}", userId);
                return Result.error("用户ID不能为空且必须大于0", null);
            }
            logger.info("【AI能力分析】开始执行，userId：{}，补充需求：{}，temperature：{}", userId, userMessage, temperature);

            // 2. 查询用户基本信息（学生画像）
            logger.info("【AI能力分析】开始查询用户基本信息，userId：{} (类型: {}, 值: {})", userId, userId.getClass().getName(), userId);
            StudentProfile studentProfile = null;
            try {
                List<StudentProfile> profileList = studentProfileService.selectByUserId(userId);
                logger.info("【AI能力分析】查询用户基本信息结果，profileList size：{}", profileList == null ? "null" : profileList.size());
                if (profileList == null || profileList.isEmpty()) {
                    // 尝试直接查询数据库验证用户是否存在
                    logger.error("【AI能力分析】未查询到用户基本信息，userId：{}，请检查：", userId);
                    logger.error("  - 数据库连接是否正确: jdbc:mysql://localhost:3306/youthpath");
                    logger.error("  - 表 student_profile 是否存在");
                    logger.error("  - SQL: SELECT * FROM student_profile WHERE user_id = {} AND is_deleted = 0", userId);
                    logger.error("  - 用户ID在数据库中的实际值: 232819034332467200");
                    logger.error("  - 用户ID类型匹配: Java Long vs MySQL BIGINT");
                    return Result.error("未查询到用户的基本信息", null);
                }
                studentProfile = profileList.get(0);
                logger.info("【AI能力分析】获取到用户基本信息，profileId：{}，userName：{}", studentProfile.getId(), studentProfile.getUserName());
            } catch (Exception e) {
                logger.error("【AI能力分析】查询用户基本信息异常，userId：{}", userId, e);
                return Result.error("查询用户基本信息时发生异常: " + e.getMessage(), null);
            }

            // 3. 查询用户能力维度信息
            logger.info("【AI能力分析】开始查询用户能力维度信息，userId：{}", userId);
            StudentAbility studentAbility = null;
            try {
                List<StudentAbility> abilityList = studentAbilityService.selectByUserId(userId);
                logger.info("【AI能力分析】查询用户能力维度信息结果，abilityList size：{}", abilityList == null ? "null" : abilityList.size());
                if (abilityList == null || abilityList.isEmpty()) {
                    logger.error("【AI能力分析】未查询到用户能力维度信息，userId：{}，请检查数据库中是否存在该用户的能力维度数据", userId);
                    return Result.error("未查询到用户的能力维度信息", null);
                }
                studentAbility = abilityList.get(0);
                logger.info("【AI能力分析】获取到用户能力维度信息，abilityId：{}", studentAbility.getId());
            } catch (Exception e) {
                logger.error("【AI能力分析】查询用户能力维度信息异常，userId：{}", userId, e);
                return Result.error("查询用户能力维度信息时发生异常: " + e.getMessage(), null);
            }

            // 4. 拼接完整用户需求（核心Prompt + 用户补充message）
            String finalUserPrompt = AI_ANALYSIS_CORE_PROMPT + (userMessage == null ? "无" : userMessage);

            // 5. 严格使用InPutGiveAI工具类构建AI请求
            Map<String, Object> userBackground = JSONUtil.parse(studentProfile).toBean(Map.class);
            String userData = JSONUtil.toJsonStr(studentAbility);
            Map<String, Object> aiRequest = InPutGiveAI.ai_input_with_background(
                    String.valueOf(userId),
                    finalUserPrompt,
                    userBackground,
                    new HashMap<>(),
                    userData,
                    temperature
            );
            logger.info("【AI能力分析】构建的AI请求完整内容: {}", JSONUtil.toJsonStr(aiRequest));
            // 6. 调用AI服务
            Result<?> aiResult = aiService.sendPostRequest(aiRequest, AI_REQUEST_URI);
            if (aiResult == null || aiResult.getCode() != 10001) {
                logger.error("【AI能力分析】AI服务请求失败，响应：{}", aiResult);
                return Result.error("AI服务请求失败：" + (aiResult == null ? "无响应" : aiResult.getMessage()), null);
            }

            // 7. 解析AI返回结果 - 直接使用返回的data作为评分数据
            Object aiData = aiResult.getData();
            if (aiData == null) {
                logger.error("【AI能力分析】AI返回数据为空");
                return Result.error("AI返回数据为空", null);
            }

            // 解析为评分实体
            StudentAbilityScore abilityScore;
            try {
                if (aiData instanceof Map) {
                    // 直接从Map转换
                    abilityScore = JSONUtil.toBean(JSONUtil.toJsonStr(aiData), StudentAbilityScore.class);
                } else if (aiData instanceof String) {
                    // 从字符串解析
                    abilityScore = JSONUtil.toBean((String) aiData, StudentAbilityScore.class);
                } else {
                    logger.error("【AI能力分析】AI返回数据类型异常，类型：{}", aiData.getClass().getName());
                    return Result.error("AI返回数据类型异常", null);
                }
            } catch (Exception e) {
                logger.error("【AI能力分析】AI评分解析失败，数据：{}", aiData, e);
                return Result.error("AI返回结果解析失败", null);
            }

            // 打印解析后的评分数据（明文）
            logger.info("【AI能力分析】AI解析的评分数据 - educationScore: {}, internshipScore: {}, professionalScore: {}, certificateScore: {}, innovationScore: {}, learningScore: {}, pressureScore: {}, communicationScore: {}, problemSolvingScore: {}, teamworkScore: {}, totalScore: {}, scoreComment: {}",
                    abilityScore.getEducationScore(),
                    abilityScore.getInternshipScore(),
                    abilityScore.getProfessionalScore(),
                    abilityScore.getCertificateScore(),
                    abilityScore.getInnovationScore(),
                    abilityScore.getLearningScore(),
                    abilityScore.getPressureScore(),
                    abilityScore.getCommunicationScore(),
                    abilityScore.getProblemSolvingScore(),
                    abilityScore.getTeamworkScore(),
                    abilityScore.getTotalScore(),
                    abilityScore.getScoreComment());

            // 8. 填充基础字段并入库
            abilityScore.setUserId(userId);
            abilityScore.setAbilityId(studentAbility.getId());
            abilityScore.setScoreType(1); // 1=AI自动分析
            logger.info("【AI能力分析】AI评分解析完成，总分：{}", abilityScore.getTotalScore());

            // 9. 删除该用户现有的AI评分记录（scoreType = 1），避免重复
            logger.info("【AI能力分析】开始删除用户现有的AI评分记录，userId：{}", userId);
            List<StudentAbilityScore> existingScores = studentAbilityScoreService.selectByUserId(userId);
            if (existingScores != null && !existingScores.isEmpty()) {
                int deletedCount = 0;
                for (StudentAbilityScore existingScore : existingScores) {
                    if (existingScore.getScoreType() != null && existingScore.getScoreType() == 1) {
                        studentAbilityScoreService.deleteById(existingScore.getId());
                        deletedCount++;
                        logger.info("【AI能力分析】删除现有AI评分记录，id：{}", existingScore.getId());
                    }
                }
                logger.info("【AI能力分析】共删除{}条现有的AI评分记录", deletedCount);
            }

            // 10. 插入新的评分记录
            int insertResult = studentAbilityScoreService.insert(abilityScore);
            if (insertResult <= 0) {
                logger.error("【AI能力分析】评分保存失败，userId：{}", userId);
                throw new RuntimeException("评分保存失败");
            }
            
            // 查询刚刚插入的记录
            List<StudentAbilityScore> savedScoreList = studentAbilityScoreService.selectByUserId(userId);
            logger.info("【AI能力分析】评分保存成功，userId：{}", userId);
            // 打印返回给前端的评分数据（解密后的明文）
            if (savedScoreList != null && !savedScoreList.isEmpty()) {
                for (StudentAbilityScore score : savedScoreList) {
                    logger.info("【AI能力分析】返回前端的评分数据 - ID: {}, educationScore: {}, internshipScore: {}, professionalScore: {}, certificateScore: {}, innovationScore: {}, learningScore: {}, pressureScore: {}, communicationScore: {}, problemSolvingScore: {}, teamworkScore: {}, totalScore: {}, scoreComment: {}",
                            score.getId(),
                            score.getEducationScore(),
                            score.getInternshipScore(),
                            score.getProfessionalScore(),
                            score.getCertificateScore(),
                            score.getInnovationScore(),
                            score.getLearningScore(),
                            score.getPressureScore(),
                            score.getCommunicationScore(),
                            score.getProblemSolvingScore(),
                            score.getTeamworkScore(),
                            score.getTotalScore(),
                            score.getScoreComment());
                }
            }

            return Result.success("AI能力分析评分完成", savedScoreList);

        } catch (Exception e) {
            logger.error("【AI能力分析】全流程失败，userId：{}", userId, e);
            return Result.error("AI能力分析评分失败：" + e.getMessage(), null);
        }
    }
}
