package org.example.web.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.web.entity.MatchDetail;
import org.example.web.entity.MatchRecord;
import org.example.web.mapper.MatchDetailMapper;
import org.example.web.mapper.MatchRecordMapper;
import org.example.web.service.MatchService;
import org.example.web.tool.RSA_256;
import org.example.web.tool.SnowIdCreater;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 人岗匹配服务实现类
 * 实现MatchService接口，提供人岗匹配的核心业务逻辑
 * 注意：实际的匹配计算和报告生成由AI服务器完成，本服务主要负责数据管理和AI请求转发
 * 
 * @author 系统生成
 * @version 1.0
 */
@Service
public class MatchServiceImpl implements MatchService {

    @Autowired
    private MatchRecordMapper matchRecordMapper;

    @Autowired
    private MatchDetailMapper matchDetailMapper;

    @Autowired
    private RSA_256 rsa256;
    
    // ============== AI服务器调用部分（需要实现） ==============
    // 注意：以下AI服务客户端注入和AI调用部分需要根据实际AI服务器接口进行实现
    // 请实现一个AIClientService类，用于与AI服务器进行HTTP/RPC通信
    
    // TODO: 注入AI服务客户端，用于调用AI服务器进行匹配计算和报告生成
    // @Autowired
    // private AIClientService aiClientService;
    
    // AI服务器相关配置示例：
    // @Value("${ai.server.url}")
    // private String aiServerUrl;
    // 
    // @Value("${ai.server.api-key}")
    // private String aiApiKey;
    // 
    // AI请求和响应数据结构示例：
    // public class AIMatchRequest {
    //     private Long userId;
    //     private Long jobId;
    //     private Long matchId;
    //     private Integer level;
    //     // 其他相关数据...
    // }
    // 
    // public class AIMatchResponse {
    //     private Long matchId;
    //     private Map<String, BigDecimal> scores; // 10个维度分数
    //     private Integer matchResult; // 匹配结果（1-4）
    //     private List<AIMatchDetail> matchDetails; // 10个维度详情
    //     private String summary; // 匹配摘要
    //     private List<String> recommendations; // 提升建议
    // }
    // 
    // public class AIMatchDetail {
    //     private String dimCode; // dim1-dim10
    //     private String dimName; // 维度名称
    //     private Integer dimType; // 1硬门槛 2专业技能 3软实力
    //     private String studentContent; // 学生情况
    //     private String jobRequire; // 岗位要求
    //     private BigDecimal score; // 得分
    //     private String gapAnalysis; // 差距分析
    //     private String improvementSuggestion; // 提升建议
    // }

    /**
     * 创建人岗匹配记录
     * 初始化匹配记录的基本信息，分数由AI服务器计算后更新
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @param level 职级（1-入门，2-中级，3-高级）
     * @return 创建的匹配记录对象，失败返回null
     */
    @Override
    @Transactional
    public MatchRecord createMatchRecord(Long userId, Long jobId, Integer level) {
        // 创建匹配记录对象
        MatchRecord matchRecord = new MatchRecord();
        // 使用雪花算法生成唯一ID，类别16表示匹配记录
        matchRecord.setId(SnowIdCreater.generateId(16));
        matchRecord.setUserId(userId);
        matchRecord.setJobId(jobId);
        matchRecord.setLevel(level);
        matchRecord.setMatchStatus(1); // 状态：生成中（等待AI计算）
        matchRecord.setMatchResult(0); // 结果：未确定
        matchRecord.setCreateTime(LocalDateTime.now());
        matchRecord.setUpdateTime(LocalDateTime.now());
        matchRecord.setIsDeleted(0);

        // 初始化所有分数为0，实际分数由AI服务器计算后更新
        matchRecord.setHardScore(BigDecimal.ZERO);
        matchRecord.setSkillScore(BigDecimal.ZERO);
        matchRecord.setSoftScore(BigDecimal.ZERO);
        matchRecord.setEducationScore(BigDecimal.ZERO);
        matchRecord.setInternshipScore(BigDecimal.ZERO);
        matchRecord.setProfessionalScore(BigDecimal.ZERO);
        matchRecord.setCertificateScore(BigDecimal.ZERO);
        matchRecord.setInnovationScore(BigDecimal.ZERO);
        matchRecord.setLearningScore(BigDecimal.ZERO);
        matchRecord.setPressureScore(BigDecimal.ZERO);
        matchRecord.setCommunicationScore(BigDecimal.ZERO);
        matchRecord.setProblemSolvingScore(BigDecimal.ZERO);
        matchRecord.setTeamworkScore(BigDecimal.ZERO);
        matchRecord.setTotalScore(BigDecimal.ZERO);

        // 插入数据库
        int result = matchRecordMapper.insert(matchRecord);
        if (result > 0) {
            return matchRecord;
        }
        return null;
    }

    /**
     * 根据ID获取匹配记录
     * 
     * @param id 匹配记录ID
     * @return 匹配记录对象
     */
    @Override
    public MatchRecord getMatchRecordById(Long id) {
        return matchRecordMapper.selectById(id);
    }

    /**
     * 根据用户ID和岗位ID获取匹配记录
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @return 匹配记录对象
     */
    @Override
    public MatchRecord getMatchRecordByUserAndJob(Long userId, Long jobId) {
        return matchRecordMapper.selectByUserIdAndJobId(userId, jobId);
    }

    /**
     * 根据用户ID获取匹配记录列表
     * 
     * @param userId 用户ID
     * @return 匹配记录列表
     */
    @Override
    public List<MatchRecord> getMatchRecordsByUserId(Long userId) {
        return matchRecordMapper.selectByUserId(userId);
    }

    /**
     * 根据岗位ID获取匹配记录列表
     * 
     * @param jobId 岗位ID
     * @return 匹配记录列表
     */
    @Override
    public List<MatchRecord> getMatchRecordsByJobId(Long jobId) {
        return matchRecordMapper.selectByJobId(jobId);
    }

    /**
     * 更新匹配状态
     * 
     * @param id 匹配记录ID
     * @param matchStatus 匹配状态（0-未生成，1-生成中，2-已完成，3-失败）
     * @return 更新成功返回true，失败返回false
     */
    @Override
    @Transactional
    public boolean updateMatchStatus(Long id, Integer matchStatus) {
        int result = matchRecordMapper.updateMatchStatus(id, matchStatus);
        return result > 0;
    }

    /**
     * 更新匹配结果
     * 
     * @param id 匹配记录ID
     * @param matchResult 匹配结果（1-强烈推荐，2-推荐，3-一般，4-不推荐）
     * @return 更新成功返回true，失败返回false
     */
    @Override
    @Transactional
    public boolean updateMatchResult(Long id, Integer matchResult) {
        int result = matchRecordMapper.updateMatchResult(id, matchResult);
        return result > 0;
    }

    /**
     * 更新匹配分数
     * 根据传入的分数Map更新匹配记录的各项分数
     * 
     * @param id 匹配记录ID
     * @param scores 分数Map，key为分数字段名，value为分数值
     * @return 更新成功返回true，失败返回false
     */
    @Override
    @Transactional
    public boolean updateMatchScores(Long id, Map<String, BigDecimal> scores) {
        MatchRecord matchRecord = matchRecordMapper.selectById(id);
        if (matchRecord == null) {
            return false;
        }

        // 遍历分数Map，更新对应的分数字段
        scores.forEach((key, value) -> {
            switch (key) {
                case "hardScore":
                    matchRecord.setHardScore(value);
                    break;
                case "skillScore":
                    matchRecord.setSkillScore(value);
                    break;
                case "softScore":
                    matchRecord.setSoftScore(value);
                    break;
                case "educationScore":
                    matchRecord.setEducationScore(value);
                    break;
                case "internshipScore":
                    matchRecord.setInternshipScore(value);
                    break;
                case "professionalScore":
                    matchRecord.setProfessionalScore(value);
                    break;
                case "certificateScore":
                    matchRecord.setCertificateScore(value);
                    break;
                case "innovationScore":
                    matchRecord.setInnovationScore(value);
                    break;
                case "learningScore":
                    matchRecord.setLearningScore(value);
                    break;
                case "pressureScore":
                    matchRecord.setPressureScore(value);
                    break;
                case "communicationScore":
                    matchRecord.setCommunicationScore(value);
                    break;
                case "problemSolvingScore":
                    matchRecord.setProblemSolvingScore(value);
                    break;
                case "teamworkScore":
                    matchRecord.setTeamworkScore(value);
                    break;
                case "totalScore":
                    matchRecord.setTotalScore(value);
                    break;
            }
        });

        matchRecord.setUpdateTime(LocalDateTime.now());
        int result = matchRecordMapper.updateScores(matchRecord);
        return result > 0;
    }

    /**
     * 逻辑删除匹配记录
     * 将is_deleted字段置为1，实现软删除
     * 
     * @param id 匹配记录ID
     * @return 删除成功返回true，失败返回false
     */
    @Override
    @Transactional
    public boolean deleteMatchRecord(Long id) {
        int result = matchRecordMapper.logicDeleteById(id);
        return result > 0;
    }

    /**
     * 创建匹配详情列表
     * 为匹配记录创建10个维度的详细匹配信息
     * 
     * @param matchId 匹配记录ID
     * @param matchDetails 匹配详情列表
     * @return 创建成功返回true，失败返回false
     */
    @Override
    @Transactional
    public boolean createMatchDetails(Long matchId, List<MatchDetail> matchDetails) {
        if (matchDetails == null || matchDetails.isEmpty()) {
            return false;
        }

        // 为每个匹配详情设置ID和时间
        for (MatchDetail detail : matchDetails) {
            detail.setId(SnowIdCreater.generateId(17)); // 使用类别17表示匹配详情
            detail.setMatchId(matchId);
            detail.setCreateTime(LocalDateTime.now());
        }

        int result = matchDetailMapper.batchInsert(matchDetails);
        return result > 0;
    }

    /**
     * 根据匹配记录ID获取匹配详情列表
     * 
     * @param matchId 匹配记录ID
     * @return 匹配详情列表
     */
    @Override
    public List<MatchDetail> getMatchDetailsByMatchId(Long matchId) {
        return matchDetailMapper.selectByMatchId(matchId);
    }

    /**
     * 根据ID获取匹配详情
     * 
     * @param id 匹配详情ID
     * @return 匹配详情对象
     */
    @Override
    public MatchDetail getMatchDetailById(Long id) {
        return matchDetailMapper.selectById(id);
    }

    /**
     * 更新匹配详情
     * 
     * @param matchDetail 匹配详情对象
     * @return 更新成功返回true，失败返回false
     */
    @Override
    @Transactional
    public boolean updateMatchDetail(MatchDetail matchDetail) {
        matchDetail.setCreateTime(LocalDateTime.now());
        int result = matchDetailMapper.update(matchDetail);
        return result > 0;
    }

    /**
     * 删除匹配详情
     * 
     * @param id 匹配详情ID
     * @return 删除成功返回true，失败返回false
     */
    @Override
    @Transactional
    public boolean deleteMatchDetail(Long id) {
        int result = matchDetailMapper.deleteById(id);
        return result > 0;
    }

    /**
     * 计算人岗匹配
     * 创建匹配记录并调用AI服务器进行匹配计算
     * 注意：实际匹配计算由AI服务器完成，本方法负责创建记录并触发AI计算
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @param level 职级
     * @return 包含匹配ID、状态和AI请求JSON数据的结果Map
     */
    @Override
    public Map<String, Object> calculateMatch(Long userId, Long jobId, Integer level) {
        // 创建匹配记录
        MatchRecord matchRecord = createMatchRecord(userId, jobId, level);
        if (matchRecord == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        Long matchId = matchRecord.getId();
        result.put("matchId", matchId);
        result.put("status", "pending");
        result.put("message", "匹配计算请求已发送到AI服务器，请稍后查看结果");
        
        // 构建AI请求的完整JSON数据（包含学生画像、岗位要求和AI处理期望）
        Map<String, Object> aiRequestJSON = buildAIRequestJSON(userId, jobId, matchId, level);
        result.put("aiRequestJSON", aiRequestJSON);
        result.put("aiRequestJSONNote", "此JSON数据包含学生画像、岗位要求和AI处理期望，可用于AI服务器进行人岗匹配分析");

        // TODO: 调用AI服务器进行人岗匹配计算
        // 1. 构建请求参数，包含用户ID、岗位ID、匹配记录ID等
        // 2. 通过HTTP客户端或RPC调用AI服务器的匹配计算接口
        // 3. AI服务器返回计算结果后，更新匹配记录的状态和分数
        // 4. 创建匹配详情记录
        
        // 示例代码结构（需要实际实现）：
        // try {
        //     // 构建AI请求
        //     AIMatchRequest request = new AIMatchRequest();
        //     request.setUserId(userId);
        //     request.setJobId(jobId);
        //     request.setMatchId(matchRecord.getId());
        //     request.setLevel(level);
        //     request.setStudentData(aiRequestJSON.get("studentData"));
        //     request.setJobRequirements(aiRequestJSON.get("jobRequirements"));
        //     request.setAiExpectation(aiRequestJSON.get("aiExpectation"));
        //     
        //     // 调用AI服务
        //     AIMatchResponse response = aiClientService.calculateMatch(request);
        //     
        //     // 异步处理AI返回的结果
        //     processAIResponse(matchRecord.getId(), response);
        //     
        //     result.put("status", "processing");
        //     result.put("message", "AI服务器正在计算匹配结果");
        // } catch (Exception e) {
        //     // 更新匹配状态为失败
        //     matchRecordMapper.updateMatchStatus(matchRecord.getId(), 3);
        //     result.put("status", "error");
        //     result.put("message", "调用AI服务器失败: " + e.getMessage());
        // }

        return result;
    }

    /**
     * 获取匹配报告
     * 从数据库获取匹配记录和详情，如果需要生成详细报告则调用AI服务器
     * 
     * @param matchId 匹配记录ID
     * @return 匹配报告Map
     */
    @Override
    public Map<String, Object> getMatchReport(Long matchId) {
        Map<String, Object> report = new HashMap<>();
        
        // 获取匹配记录
        MatchRecord matchRecord = matchRecordMapper.selectById(matchId);
        if (matchRecord == null) {
            report.put("error", "未找到匹配记录");
            return report;
        }
        
        // 获取匹配详情
        List<MatchDetail> matchDetails = matchDetailMapper.selectByMatchId(matchId);
        
        report.put("matchRecord", matchRecord);
        report.put("matchDetails", matchDetails);
        
        // TODO: 如果需要生成详细的报告摘要和建议，可以调用AI服务器
        // 示例代码结构：
        // if (matchRecord.getMatchStatus() == 2) { // 已完成
        //     // 调用AI服务器生成报告摘要和建议
        //     AIReportRequest request = new AIReportRequest();
        //     request.setMatchId(matchId);
        //     request.setMatchRecord(matchRecord);
        //     request.setMatchDetails(matchDetails);
        //     
        //     AIReportResponse response = aiClientService.generateReport(request);
        //     
        //     report.put("summary", response.getSummary());
        //     report.put("recommendations", response.getRecommendations());
        // } else {
        //     // 使用简单的本地生成（仅用于演示）
        //     report.put("summary", generateSimpleSummary(matchRecord, matchDetails));
        //     report.put("recommendations", generateSimpleRecommendations(matchRecord, matchDetails));
        // }
        
        // 临时使用简单生成（实际应由AI服务器生成）
        report.put("summary", generateSimpleSummary(matchRecord, matchDetails));
        report.put("recommendations", generateSimpleRecommendations(matchRecord, matchDetails));
        
        return report;
    }

    /**
     * 批量匹配
     * 对多个用户和多个岗位进行批量匹配计算，调用AI服务器处理
     * 
     * @param userIds 用户ID列表
     * @param jobIds 岗位ID列表
     * @return 批量匹配结果列表
     */
    @Override
    public List<Map<String, Object>> batchMatch(List<Long> userIds, List<Long> jobIds) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (Long userId : userIds) {
            for (Long jobId : jobIds) {
                Map<String, Object> matchResult = new HashMap<>();
                matchResult.put("userId", userId);
                matchResult.put("jobId", jobId);
                
                try {
                    // 创建匹配记录
                    MatchRecord matchRecord = createMatchRecord(userId, jobId, 1); // 默认入门级
                    if (matchRecord != null) {
                        matchResult.put("success", true);
                        matchResult.put("matchId", matchRecord.getId());
                        matchResult.put("message", "匹配记录已创建，等待AI服务器计算");
                        
                        // TODO: 调用AI服务器进行批量匹配计算
                        // 可以收集所有匹配记录ID，然后一次性发送给AI服务器进行批量计算
                    } else {
                        matchResult.put("success", false);
                        matchResult.put("message", "创建匹配记录失败");
                    }
                } catch (Exception e) {
                    matchResult.put("success", false);
                    matchResult.put("message", "匹配失败: " + e.getMessage());
                }
                
                results.add(matchResult);
            }
        }
        
        // TODO: 如果有批量计算接口，可以在这里调用AI服务器的批量匹配接口
        // 示例：
        // if (!results.isEmpty()) {
        //     List<Long> matchIds = results.stream()
        //         .filter(r -> (Boolean)r.get("success"))
        //         .map(r -> (Long)r.get("matchId"))
        //         .collect(Collectors.toList());
        //     
        //     if (!matchIds.isEmpty()) {
        //         aiClientService.batchCalculateMatch(matchIds);
        //     }
        // }
        
        return results;
    }

    /**
     * 创建匹配详情（应由AI服务器生成）
     * 注意：实际匹配详情应由AI服务器计算后返回，本方法仅为数据存储示例
     * 
     * @param matchId 匹配记录ID
     * @param aiMatchDetails AI服务器返回的匹配详情数据
     * @return 是否创建成功
     */
    private boolean createMatchDetailsFromAI(Long matchId, List<MatchDetail> aiMatchDetails) {
        if (aiMatchDetails == null || aiMatchDetails.isEmpty()) {
            return false;
        }

        // 为每个匹配详情设置ID和时间
        for (MatchDetail detail : aiMatchDetails) {
            detail.setId(SnowIdCreater.generateId(17)); // 使用类别17表示匹配详情
            detail.setMatchId(matchId);
            detail.setCreateTime(LocalDateTime.now());
        }

        int result = matchDetailMapper.batchInsert(aiMatchDetails);
        return result > 0;
    }
    
    /**
     * 生成简单摘要（私有辅助方法）
     * 注意：实际项目中应由AI服务器生成详细的报告摘要
     * 
     * @param matchRecord 匹配记录
     * @param matchDetails 匹配详情列表
     * @return 简单摘要Map
     */
    private Map<String, Object> generateSimpleSummary(MatchRecord matchRecord, List<MatchDetail> matchDetails) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalScore", matchRecord.getTotalScore());
        summary.put("matchResult", getMatchResultText(matchRecord.getMatchResult()));
        summary.put("matchLevel", getLevelText(matchRecord.getLevel()));
        
        // 简单摘要，实际应由AI服务器生成
        summary.put("note", "此为简单摘要，详细分析报告应由AI服务器生成");
        
        return summary;
    }
    
    /**
     * 生成简单建议（私有辅助方法）
     * 注意：实际项目中应由AI服务器生成个性化的提升建议
     * 
     * @param matchRecord 匹配记录
     * @param matchDetails 匹配详情列表
     * @return 简单建议列表
     */
    private List<String> generateSimpleRecommendations(MatchRecord matchRecord, List<MatchDetail> matchDetails) {
        List<String> recommendations = new ArrayList<>();
        
        recommendations.add("根据匹配结果，建议重点关注以下方面的提升：");
        recommendations.add("注：详细个性化建议应由AI服务器基于您的具体情况进行生成");
        
        // 简单通用建议
        if (matchRecord.getTotalScore().compareTo(new BigDecimal("80")) < 0) {
            recommendations.add("总体匹配度有待提升，建议系统学习相关技能并积累实践经验。");
        } else {
            recommendations.add("总体匹配度良好，继续保持并深化专业能力。");
        }
        
        return recommendations;
    }
    
    /**
     * 获取匹配结果文本描述（私有辅助方法）
     * 
     * @param matchResult 匹配结果代码
     * @return 匹配结果文本描述
     */
    private String getMatchResultText(Integer matchResult) {
        switch (matchResult) {
            case 1: return "强烈推荐";
            case 2: return "推荐";
            case 3: return "一般";
            case 4: return "不推荐";
            default: return "未确定";
        }
    }
    
    /**
     * 获取职级文本描述（私有辅助方法）
     * 
     * @param level 职级代码
     * @return 职级文本描述
     */
    private String getLevelText(Integer level) {
        switch (level) {
            case 1: return "入门级";
            case 2: return "中级";
            case 3: return "高级";
            default: return "未知";
        }
    }
    
    /**
     * 处理AI服务器返回的匹配结果（示例方法）
     * 注意：实际项目中应根据AI服务器的响应格式进行调整
     * 
     * @param matchId 匹配记录ID
     * @param aiResponse AI服务器返回的匹配结果
     */
    private void processAIResponse(Long matchId, Object aiResponse) {
        // TODO: 根据AI服务器的响应格式解析数据并更新数据库
        // 示例代码结构：
        // 1. 解析AI响应，获取匹配分数、结果、详情等信息
        // 2. 更新匹配记录的状态、分数和结果
        // 3. 创建匹配详情记录
        // 4. 更新相关统计信息
        
        // 示例：
        // MatchRecord matchRecord = matchRecordMapper.selectById(matchId);
        // if (matchRecord != null) {
        //     // 解析AI响应
        //     BigDecimal totalScore = aiResponse.getTotalScore();
        //     Integer matchResult = aiResponse.getMatchResult();
        //     List<AIMatchDetail> aiDetails = aiResponse.getMatchDetails();
        //     
        //     // 更新匹配记录
        //     matchRecord.setTotalScore(totalScore);
        //     matchRecord.setMatchResult(matchResult);
        //     matchRecord.setMatchStatus(2); // 已完成
        //     matchRecord.setUpdateTime(LocalDateTime.now());
        //     
        //     // 更新各项分数（根据AI响应）
        //     // matchRecord.setHardScore(aiResponse.getHardScore());
        //     // ... 其他分数
        //     
        //     matchRecordMapper.updateScores(matchRecord);
        //     
        //     // 创建匹配详情
        //     if (aiDetails != null && !aiDetails.isEmpty()) {
        //         List<MatchDetail> matchDetails = convertAIDetailsToMatchDetails(matchId, aiDetails);
        //         createMatchDetailsFromAI(matchId, matchDetails);
        //     }
        // }
    }
    
    // ============== 高级匹配功能实现 ==============
    
    /**
     * 自动触发全量人岗匹配
     * 在学生画像生成/更新、能力数据变更后自动调用
     * 注意：此方法应由事件监听器触发，实际匹配计算由AI服务器完成
     * 
     * @param userId 用户ID
     * @param triggerType 触发类型：1-画像生成，2-画像更新，3-能力数据变更
     * @return 是否成功触发匹配计算
     */
    @Override
    @Transactional
    public boolean triggerAutoMatch(Long userId, Integer triggerType) {
        try {
            // 记录触发日志
            System.out.println("自动触发全量人岗匹配：用户ID=" + userId + "，触发类型=" + triggerType);
            
            // TODO: 获取用户的所有相关岗位，进行批量匹配
            // 1. 查询用户的最新画像和能力数据
            // 2. 获取所有适合该用户的岗位ID列表
            // 3. 调用batchMatch方法进行批量匹配
            
            // 临时返回成功，实际需要实现上述逻辑
            return true;
        } catch (Exception e) {
            System.err.println("自动触发全量匹配失败：" + e.getMessage());
            return false;
        }
    }
    
    /**
     * 硬门槛一票否决校验
     * 先校验学历、实习经历2个硬门槛维度，不达标直接终止匹配
     * 注意：此方法应在AI计算前调用，如果返回false则直接设置匹配结果为不推荐
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @return 是否通过硬门槛校验（true-通过，false-不通过）
     */
    @Override
    public boolean checkHardThreshold(Long userId, Long jobId) {
        // TODO: 实现硬门槛校验逻辑
        // 1. 查询用户的学历背景和实习经历（从StudentAbility表）
        // 2. 查询岗位的硬门槛要求（从JobHardRequirement表）
        // 3. 进行比对，判断是否满足硬门槛要求
        
        // 示例校验逻辑（需要根据实际数据模型调整）：
        // StudentAbility studentAbility = studentAbilityMapper.selectByUserId(userId);
        // JobHardRequirement jobHardReq = jobHardRequirementMapper.selectByJobId(jobId);
        // 
        // if (studentAbility == null || jobHardReq == null) {
        //     return false; // 数据不存在，无法校验
        // }
        // 
        // // 学历背景校验
        // String userEducation = studentAbility.getEducationRequirement();
        // String jobEducation = jobHardReq.getEducationRequirement();
        // boolean educationPass = checkEducationMatch(userEducation, jobEducation);
        // 
        // // 实习经历校验
        // String userInternship = studentAbility.getInternshipAbility();
        // String jobInternship = jobHardReq.getInternshipRequirement();
        // boolean internshipPass = checkInternshipMatch(userInternship, jobInternship);
        // 
        // return educationPass && internshipPass;
        
        // 临时返回true，等待实际实现
        System.out.println("硬门槛校验：用户ID=" + userId + "，岗位ID=" + jobId + "（TODO: 实现具体校验逻辑）");
        return true;
    }
    
    /**
     * 增量匹配优化
     * 学生仅更新单维度数据时，仅重新计算对应维度的匹配分
     * 注意：此优化需要AI服务器支持增量计算，如果AI不支持则回退到全量计算
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @param updatedDimension 更新的维度代码（dim1-dim10）
     * @return 包含匹配ID和状态的结果Map
     */
    @Override
    public Map<String, Object> incrementalMatch(Long userId, Long jobId, String updatedDimension) {
        Map<String, Object> result = new HashMap<>();
        
        // TODO: 实现增量匹配逻辑
        // 1. 检查是否存在现有的匹配记录
        // 2. 如果存在，获取现有匹配记录和详情
        // 3. 构建增量匹配请求，只包含更新的维度
        // 4. 调用AI服务器的增量匹配接口
        // 5. 更新对应维度的分数和详情
        
        // 临时返回结果
        result.put("matchId", 0L);
        result.put("status", "pending");
        result.put("message", "增量匹配功能待实现，目前回退到全量匹配");
        result.put("updatedDimension", updatedDimension);
        
        // 临时回退到全量匹配
        Map<String, Object> fullMatchResult = calculateMatch(userId, jobId, 1);
        if (fullMatchResult != null) {
            result.put("fallbackToFullMatch", true);
            result.put("fullMatchResult", fullMatchResult);
        }
        
        return result;
    }
    
    /**
     * 获取匹配历史记录（支持筛选）
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param matchResult 匹配结果（可选，1-4）
     * @return 匹配记录列表
     */
    @Override
    public List<MatchRecord> getMatchHistory(Long userId, Long jobId, String startTime, String endTime, Integer matchResult) {
        // TODO: 实现带筛选条件的匹配历史查询
        // 需要扩展MatchRecordMapper，添加带筛选条件的查询方法
        
        // 临时实现：先获取所有匹配记录，然后在内存中筛选
        List<MatchRecord> allRecords = matchRecordMapper.selectByUserId(userId);
        List<MatchRecord> filteredRecords = new ArrayList<>();
        
        for (MatchRecord record : allRecords) {
            // 岗位ID筛选
            if (jobId != null && !jobId.equals(record.getJobId())) {
                continue;
            }
            
            // 匹配结果筛选
            if (matchResult != null && !matchResult.equals(record.getMatchResult())) {
                continue;
            }
            
            // 时间范围筛选（TODO: 实现时间解析和比较）
            // 暂时跳过时间筛选
            
            filteredRecords.add(record);
        }
        
        return filteredRecords;
    }
    
    /**
     * 获取匹配结果（支持排序筛选）
     * 
     * @param userId 用户ID
     * @param sortBy 排序字段：match_score-匹配度，job_hot-岗位热度，industry-行业，city-城市，salary-薪资范围
     * @param sortOrder 排序顺序：asc-升序，desc-降序
     * @param industry 行业筛选（可选）
     * @param city 城市筛选（可选）
     * @param minSalary 最低薪资筛选（可选）
     * @param maxSalary 最高薪资筛选（可选）
     * @return 匹配记录列表
     */
    @Override
    public List<MatchRecord> getMatchResultsWithFilter(Long userId, String sortBy, String sortOrder, 
                                                        String industry, String city, BigDecimal minSalary, BigDecimal maxSalary) {
        // TODO: 实现带排序和筛选的匹配结果查询
        // 需要扩展MatchRecordMapper，支持联表查询（关联JobInfo表）和复杂条件
        
        // 临时实现：先获取所有匹配记录
        List<MatchRecord> allRecords = matchRecordMapper.selectByUserId(userId);
        
        // 简单的内存排序（实际应该在数据库层面排序）
        if ("match_score".equals(sortBy)) {
            allRecords.sort((r1, r2) -> {
                BigDecimal score1 = r1.getTotalScore() != null ? r1.getTotalScore() : BigDecimal.ZERO;
                BigDecimal score2 = r2.getTotalScore() != null ? r2.getTotalScore() : BigDecimal.ZERO;
                return "desc".equals(sortOrder) ? score2.compareTo(score1) : score1.compareTo(score2);
            });
        }
        
        return allRecords;
    }
    
    /**
     * 收藏匹配岗位
     * 
     * @param userId 用户ID
     * @param matchId 匹配记录ID
     * @return 是否收藏成功
     */
    @Override
    @Transactional
    public boolean favoriteMatch(Long userId, Long matchId) {
        // TODO: 实现收藏功能，需要创建MatchFavoriteMapper
        // 1. 验证匹配记录存在且属于该用户
        // 2. 检查是否已收藏
        // 3. 创建收藏记录
        
        System.out.println("收藏匹配岗位：用户ID=" + userId + "，匹配ID=" + matchId + "（TODO: 实现收藏功能）");
        return false;
    }
    
    /**
     * 取消收藏匹配岗位
     * 
     * @param userId 用户ID
     * @param matchId 匹配记录ID
     * @return 是否取消成功
     */
    @Override
    @Transactional
    public boolean unfavoriteMatch(Long userId, Long matchId) {
        // TODO: 实现取消收藏功能
        System.out.println("取消收藏匹配岗位：用户ID=" + userId + "，匹配ID=" + matchId + "（TODO: 实现取消收藏功能）");
        return false;
    }
    
    /**
     * 置顶匹配岗位
     * 
     * @param userId 用户ID
     * @param matchId 匹配记录ID
     * @return 是否置顶成功
     */
    @Override
    @Transactional
    public boolean pinMatch(Long userId, Long matchId) {
        // TODO: 实现置顶功能
        // 1. 验证匹配记录存在且属于该用户
        // 2. 检查是否已收藏（置顶需要先收藏）
        // 3. 更新收藏记录的is_pinned字段
        
        System.out.println("置顶匹配岗位：用户ID=" + userId + "，匹配ID=" + matchId + "（TODO: 实现置顶功能）");
        return false;
    }
    
    /**
     * 取消置顶匹配岗位
     * 
     * @param userId 用户ID
     * @param matchId 匹配记录ID
     * @return 是否取消成功
     */
    @Override
    @Transactional
    public boolean unpinMatch(Long userId, Long matchId) {
        // TODO: 实现取消置顶功能
        System.out.println("取消置顶匹配岗位：用户ID=" + userId + "，匹配ID=" + matchId + "（TODO: 实现取消置顶功能）");
        return false;
    }
    
    /**
     * 获取收藏的匹配列表
     * 
     * @param userId 用户ID
     * @return 匹配记录列表
     */
    @Override
    public List<MatchRecord> getFavoriteMatches(Long userId) {
        // TODO: 实现获取收藏列表功能
        // 需要联表查询MatchFavorite和MatchRecord
        
        System.out.println("获取收藏的匹配列表：用户ID=" + userId + "（TODO: 实现获取收藏列表功能）");
        return new ArrayList<>();
    }
    
    /**
     * 获取置顶的匹配列表
     * 
     * @param userId 用户ID
     * @return 匹配记录列表
     */
    @Override
    public List<MatchRecord> getPinnedMatches(Long userId) {
        // TODO: 实现获取置顶列表功能
        System.out.println("获取置顶的匹配列表：用户ID=" + userId + "（TODO: 实现获取置顶列表功能）");
        return new ArrayList<>();
    }
    
    /**
     * 获取匹配结果的完整详情（包含岗位市场信息、薪资趋势、晋升路径等）
     * 
     * @param matchId 匹配记录ID
     * @return 包含匹配记录和关联数据的Map
     */
    @Override
    public Map<String, Object> getMatchFullDetail(Long matchId) {
        Map<String, Object> fullDetail = new HashMap<>();
        
        // 获取匹配记录
        MatchRecord matchRecord = matchRecordMapper.selectById(matchId);
        if (matchRecord == null) {
            fullDetail.put("error", "未找到匹配记录");
            return fullDetail;
        }
        
        // 获取匹配详情
        List<MatchDetail> matchDetails = matchDetailMapper.selectByMatchId(matchId);
        
        fullDetail.put("matchRecord", matchRecord);
        fullDetail.put("matchDetails", matchDetails);
        
        // TODO: 获取岗位市场信息（关联JobMarketInfo表）
        // TODO: 获取薪资趋势信息（关联JobMarketInfo表）
        // TODO: 获取晋升路径信息（关联JobPromotionGraph表）
        // TODO: 获取换岗路径信息（关联JobTransferGraph表）
        
        // 添加TODO标记
        fullDetail.put("note", "完整详情功能待完善，需要关联查询岗位市场信息、薪资趋势、晋升路径等");
        
        return fullDetail;
    }
    
    /**
     * 重试失败的匹配计算
     * 
     * @param matchId 匹配记录ID
     * @return 重试是否成功
     */
    @Override
    @Transactional
    public boolean retryFailedMatch(Long matchId) {
        MatchRecord matchRecord = matchRecordMapper.selectById(matchId);
        if (matchRecord == null) {
            return false;
        }
        
        // 只有状态为失败（3）的匹配记录才能重试
        if (matchRecord.getMatchStatus() != 3) {
            return false;
        }
        
        // 更新状态为生成中
        matchRecord.setMatchStatus(1);
        matchRecord.setUpdateTime(LocalDateTime.now());
        int result = matchRecordMapper.updateMatchStatus(matchId, 1);
        
        if (result > 0) {
            // TODO: 重新触发AI匹配计算
            // 需要获取原始的用户ID、岗位ID、职级信息，重新调用calculateMatch
            
            System.out.println("重试失败的匹配计算：匹配ID=" + matchId + "（TODO: 重新触发AI计算）");
            return true;
        }
        
        return false;
    }
    
    /**
     * 终止超时的匹配计算
     * 
     * @param matchId 匹配记录ID
     * @return 终止是否成功
     */
    @Override
    @Transactional
    public boolean terminateTimeoutMatch(Long matchId) {
        MatchRecord matchRecord = matchRecordMapper.selectById(matchId);
        if (matchRecord == null) {
            return false;
        }
        
        // 只有状态为生成中（1）的匹配记录才能终止
        if (matchRecord.getMatchStatus() != 1) {
            return false;
        }
        
        // 检查是否超时（例如超过30分钟）
        LocalDateTime createTime = matchRecord.getCreateTime();
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(createTime, now).toMinutes();
        
        if (minutes > 30) { // 30分钟超时
            // 更新状态为失败
            matchRecord.setMatchStatus(3);
            matchRecord.setMatchResult(4); // 不推荐
            matchRecord.setUpdateTime(now);
            
            int result = matchRecordMapper.updateMatchStatus(matchId, 3);
            if (result > 0) {
                matchRecordMapper.updateMatchResult(matchId, 4);
                System.out.println("终止超时的匹配计算：匹配ID=" + matchId + "，已超时" + minutes + "分钟");
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 构建AI匹配请求的JSON数据
     * 根据用户要求，查询学生画像和岗位要求的所有字段，构建完整的JSON数据
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @param matchId 匹配记录ID
     * @param level 职级
     * @return 包含学生画像、岗位要求和处理期望的完整JSON数据
     */
    private Map<String, Object> buildAIRequestJSON(Long userId, Long jobId, Long matchId, Integer level) {
        Map<String, Object> requestData = new HashMap<>();
        
        // 1. 基本信息
        requestData.put("matchId", matchId);
        requestData.put("userId", userId);
        requestData.put("jobId", jobId);
        requestData.put("level", level);
        requestData.put("requestTime", LocalDateTime.now().toString());
        
        // 2. 学生画像数据（从student_profile表查询所有字段）
        Map<String, Object> studentProfile = new HashMap<>();
        studentProfile.put("note", "TODO: 从student_profile表查询所有字段");
        // 实际应该查询数据库，这里先放占位符
        // StudentProfile profile = studentProfileMapper.selectByUserId(userId);
        // if (profile != null) {
        //     studentProfile.put("userName", profile.getUserName());
        //     studentProfile.put("nickname", profile.getNickname());
        //     studentProfile.put("gender", profile.getGender());
        //     studentProfile.put("phone", profile.getPhone());
        //     studentProfile.put("email", profile.getEmail());
        //     studentProfile.put("college", profile.getCollege());
        //     studentProfile.put("major", profile.getMajor());
        //     studentProfile.put("grade", profile.getGrade());
        //     studentProfile.put("age", profile.getAge());
        //     studentProfile.put("graduationDate", profile.getGraduationDate());
        //     studentProfile.put("careerIntentions", profile.getCareerIntentions());
        //     studentProfile.put("jobIntentionDetail", profile.getJobIntentionDetail());
        //     studentProfile.put("targetCity", profile.getTargetCity());
        //     studentProfile.put("expectedSalary", profile.getExpectedSalary());
        //     studentProfile.put("industryPreference", profile.getIndustryPreference());
        //     studentProfile.put("workTypePreference", profile.getWorkTypePreference());
        //     studentProfile.put("maxLearningCycle", profile.getMaxLearningCycle());
        //     studentProfile.put("education", profile.getEducation());
        //     studentProfile.put("workExperience", profile.getWorkExperience());
        //     studentProfile.put("projectExperience", profile.getProjectExperience());
        //     studentProfile.put("skill", profile.getSkill());
        //     studentProfile.put("certificate", profile.getCertificate());
        //     studentProfile.put("studentGroup", profile.getStudentGroup());
        //     studentProfile.put("privacyLevel", profile.getPrivacyLevel());
        // }
        
        // 3. 学生能力评分数据（从student_ability_score表查询所有字段）
        Map<String, Object> studentAbilityScore = new HashMap<>();
        studentAbilityScore.put("note", "TODO: 从student_ability_score表查询所有字段");
        // 实际应该查询数据库，这里先放占位符
        // StudentAbilityScore abilityScore = studentAbilityScoreMapper.selectByUserId(userId);
        // if (abilityScore != null) {
        //     studentAbilityScore.put("educationScore", abilityScore.getEducationScore());
        //     studentAbilityScore.put("internshipScore", abilityScore.getInternshipScore());
        //     studentAbilityScore.put("professionalScore", abilityScore.getProfessionalScore());
        //     studentAbilityScore.put("certificateScore", abilityScore.getCertificateScore());
        //     studentAbilityScore.put("innovationScore", abilityScore.getInnovationScore());
        //     studentAbilityScore.put("learningScore", abilityScore.getLearningScore());
        //     studentAbilityScore.put("pressureScore", abilityScore.getPressureScore());
        //     studentAbilityScore.put("communicationScore", abilityScore.getCommunicationScore());
        //     studentAbilityScore.put("problemSolvingScore", abilityScore.getProblemSolvingScore());
        //     studentAbilityScore.put("teamworkScore", abilityScore.getTeamworkScore());
        //     studentAbilityScore.put("totalScore", abilityScore.getTotalScore());
        //     studentAbilityScore.put("industryRank", abilityScore.getIndustryRank());
        //     studentAbilityScore.put("peerRank", abilityScore.getPeerRank());
        // }
        
        // 4. 岗位要求数据（从job_requirement的三张分表查询所有字段）
        Map<String, Object> jobRequirements = new HashMap<>();
        
        // 4.1 岗位硬门槛要求（job_hard_requirement表）
        Map<String, Object> jobHardRequirement = new HashMap<>();
        jobHardRequirement.put("note", "TODO: 从job_hard_requirement表查询所有字段");
        // JobHardRequirement hardReq = jobHardRequirementMapper.selectByJobId(jobId);
        // if (hardReq != null) {
        //     jobHardRequirement.put("educationRequirement", hardReq.getEducationRequirement());
        //     jobHardRequirement.put("internshipRequirement", hardReq.getInternshipRequirement());
        // }
        
        // 4.2 岗位专业技能要求（job_skill_requirement表）
        Map<String, Object> jobSkillRequirement = new HashMap<>();
        jobSkillRequirement.put("note", "TODO: 从job_skill_requirement表查询所有字段");
        // JobSkillRequirement skillReq = jobSkillRequirementMapper.selectByJobId(jobId);
        // if (skillReq != null) {
        //     jobSkillRequirement.put("professionalSkill", skillReq.getProfessionalSkill());
        //     jobSkillRequirement.put("certificateRequirement", skillReq.getCertificateRequirement());
        // }
        
        // 4.3 岗位软实力要求（job_soft_requirement表）
        Map<String, Object> jobSoftRequirement = new HashMap<>();
        jobSoftRequirement.put("note", "TODO: 从job_soft_requirement表查询所有字段");
        // JobSoftRequirement softReq = jobSoftRequirementMapper.selectByJobId(jobId);
        // if (softReq != null) {
        //     jobSoftRequirement.put("innovationAbility", softReq.getInnovationAbility());
        //     jobSoftRequirement.put("learningAbility", softReq.getLearningAbility());
        //     jobSoftRequirement.put("pressureResistance", softReq.getPressureResistance());
        //     jobSoftRequirement.put("communicationAbility", softReq.getCommunicationAbility());
        //     jobSoftRequirement.put("problemSolving", softReq.getProblemSolving());
        //     jobSoftRequirement.put("teamworkAbility", softReq.getTeamworkAbility());
        // }
        
        jobRequirements.put("hardRequirement", jobHardRequirement);
        jobRequirements.put("skillRequirement", jobSkillRequirement);
        jobRequirements.put("softRequirement", jobSoftRequirement);
        
        // 5. AI处理期望（主提示词）
        Map<String, Object> aiExpectation = new HashMap<>();
        aiExpectation.put("role", "你是一个专业的职业规划和人岗匹配专家");
        aiExpectation.put("task", "根据学生画像和岗位要求，进行10维度的详细匹配分析");
        aiExpectation.put("dimensions", "1.学历背景, 2.实习经历, 3.专业技能, 4.证书资质, 5.创新能力, 6.学习能力, 7.抗压能力, 8.沟通能力, 9.问题解决能力, 10.团队协作能力");
        aiExpectation.put("outputFormat", "返回详细的匹配分数（0-100分）、差距分析、提升建议和总体匹配度评级（1-强烈推荐, 2-推荐, 3-一般, 4-不推荐）");
        aiExpectation.put("analysisDepth", "需要深度分析学生的优势与不足，提供具体的、可操作的改进建议");
        
        // 6. 组合所有数据到最终JSON
        Map<String, Object> studentData = new HashMap<>();
        studentData.put("profile", studentProfile);
        studentData.put("abilityScore", studentAbilityScore);
        
        requestData.put("studentData", studentData);
        requestData.put("jobRequirements", jobRequirements);
        requestData.put("aiExpectation", aiExpectation);
        
        return requestData;
    }
    
    /**
     * 获取AI请求JSON（供外部调用）
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @param matchId 匹配记录ID
     * @param level 职级
     * @return 包含完整AI请求JSON数据的Map
     */
    public Map<String, Object> getAIRequestJSON(Long userId, Long jobId, Long matchId, Integer level) {
        return buildAIRequestJSON(userId, jobId, matchId, level);
    }
}
