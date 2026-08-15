package org.example.web.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.example.web.entity.MatchDetail;
import org.example.web.entity.MatchRecord;

/**
 * 人岗匹配服务接口
 * 定义人岗匹配相关的核心业务方法
 * 注意：实际的匹配计算和报告生成由AI服务器完成，本服务主要负责数据管理和AI请求转发
 * 
 * @author 系统生成
 * @version 1.0
 */
public interface MatchService {
    
    // ============== 人岗匹配记录相关方法 ==============
    
    /**
     * 创建人岗匹配记录
     * 初始化匹配记录的基本信息，分数由AI服务器计算后更新
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @param level 职级（1-入门，2-中级，3-高级）
     * @return 创建的匹配记录对象，失败返回null
     */
    MatchRecord createMatchRecord(Long userId, Long jobId, Integer level);
    
    /**
     * 根据ID获取匹配记录
     * 
     * @param id 匹配记录ID
     * @return 匹配记录对象
     */
    MatchRecord getMatchRecordById(Long id);
    
    /**
     * 根据用户ID和岗位ID获取匹配记录
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @return 匹配记录对象
     */
    MatchRecord getMatchRecordByUserAndJob(Long userId, Long jobId);
    
    /**
     * 根据用户ID获取匹配记录列表
     * 
     * @param userId 用户ID
     * @return 匹配记录列表
     */
    List<MatchRecord> getMatchRecordsByUserId(Long userId);
    
    /**
     * 根据岗位ID获取匹配记录列表
     * 
     * @param jobId 岗位ID
     * @return 匹配记录列表
     */
    List<MatchRecord> getMatchRecordsByJobId(Long jobId);
    
    /**
     * 更新匹配状态
     * 
     * @param id 匹配记录ID
     * @param matchStatus 匹配状态（0-未生成，1-生成中，2-已完成，3-失败）
     * @return 更新成功返回true，失败返回false
     */
    boolean updateMatchStatus(Long id, Integer matchStatus);
    
    /**
     * 更新匹配结果
     * 
     * @param id 匹配记录ID
     * @param matchResult 匹配结果（1-强烈推荐，2-推荐，3-一般，4-不推荐）
     * @return 更新成功返回true，失败返回false
     */
    boolean updateMatchResult(Long id, Integer matchResult);
    
    /**
     * 更新匹配分数
     * 根据传入的分数Map更新匹配记录的各项分数
     * 注意：实际分数应由AI服务器计算后更新
     * 
     * @param id 匹配记录ID
     * @param scores 分数Map，key为分数字段名，value为分数值
     * @return 更新成功返回true，失败返回false
     */
    boolean updateMatchScores(Long id, Map<String, BigDecimal> scores);
    
    /**
     * 逻辑删除匹配记录
     * 将is_deleted字段置为1，实现软删除
     * 
     * @param id 匹配记录ID
     * @return 删除成功返回true，失败返回false
     */
    boolean deleteMatchRecord(Long id);
    
    // ============== 匹配详情相关方法 ==============
    
    /**
     * 创建匹配详情列表
     * 为匹配记录创建10个维度的详细匹配信息
     * 注意：实际匹配详情应由AI服务器生成
     * 
     * @param matchId 匹配记录ID
     * @param matchDetails 匹配详情列表
     * @return 创建成功返回true，失败返回false
     */
    boolean createMatchDetails(Long matchId, List<MatchDetail> matchDetails);
    
    /**
     * 根据匹配记录ID获取匹配详情列表
     * 
     * @param matchId 匹配记录ID
     * @return 匹配详情列表
     */
    List<MatchDetail> getMatchDetailsByMatchId(Long matchId);
    
    /**
     * 根据ID获取匹配详情
     * 
     * @param id 匹配详情ID
     * @return 匹配详情对象
     */
    MatchDetail getMatchDetailById(Long id);
    
    /**
     * 更新匹配详情
     * 
     * @param matchDetail 匹配详情对象
     * @return 更新成功返回true，失败返回false
     */
    boolean updateMatchDetail(MatchDetail matchDetail);
    
    /**
     * 删除匹配详情
     * 
     * @param id 匹配详情ID
     * @return 删除成功返回true，失败返回false
     */
    boolean deleteMatchDetail(Long id);
    
    // ============== 人岗匹配计算相关方法 ==============
    
    /**
     * 计算人岗匹配
     * 创建匹配记录并调用AI服务器进行匹配计算
     * 注意：实际匹配计算由AI服务器完成，本方法负责创建记录并触发AI计算
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @param level 职级
     * @return 包含匹配ID和状态的结果Map
     */
    Map<String, Object> calculateMatch(Long userId, Long jobId, Integer level);
    
    /**
     * 获取匹配报告
     * 从数据库获取匹配记录和详情，如果需要生成详细报告则调用AI服务器
     * 
     * @param matchId 匹配记录ID
     * @return 匹配报告Map
     */
    Map<String, Object> getMatchReport(Long matchId);
    
    /**
     * 批量匹配
     * 对多个用户和多个岗位进行批量匹配计算，调用AI服务器处理
     * 
     * @param userIds 用户ID列表
     * @param jobIds 岗位ID列表
     * @return 批量匹配结果列表
     */
    List<Map<String, Object>> batchMatch(List<Long> userIds, List<Long> jobIds);
    
    // ============== 高级匹配功能 ==============
    
    /**
     * 自动触发全量人岗匹配
     * 在学生画像生成/更新、能力数据变更后自动调用
     * 注意：此方法应由事件监听器触发，实际匹配计算由AI服务器完成
     * 
     * @param userId 用户ID
     * @param triggerType 触发类型：1-画像生成，2-画像更新，3-能力数据变更
     * @return 是否成功触发匹配计算
     */
    boolean triggerAutoMatch(Long userId, Integer triggerType);
    
    /**
     * 硬门槛一票否决校验
     * 先校验学历、实习经历2个硬门槛维度，不达标直接终止匹配
     * 注意：此方法应在AI计算前调用，如果返回false则直接设置匹配结果为不推荐
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @return 是否通过硬门槛校验（true-通过，false-不通过）
     */
    boolean checkHardThreshold(Long userId, Long jobId);
    
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
    Map<String, Object> incrementalMatch(Long userId, Long jobId, String updatedDimension);
    
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
    List<MatchRecord> getMatchHistory(Long userId, Long jobId, String startTime, String endTime, Integer matchResult);
    
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
    List<MatchRecord> getMatchResultsWithFilter(Long userId, String sortBy, String sortOrder, 
                                                String industry, String city, BigDecimal minSalary, BigDecimal maxSalary);
    
    /**
     * 收藏匹配岗位
     * 
     * @param userId 用户ID
     * @param matchId 匹配记录ID
     * @return 是否收藏成功
     */
    boolean favoriteMatch(Long userId, Long matchId);
    
    /**
     * 取消收藏匹配岗位
     * 
     * @param userId 用户ID
     * @param matchId 匹配记录ID
     * @return 是否取消成功
     */
    boolean unfavoriteMatch(Long userId, Long matchId);
    
    /**
     * 置顶匹配岗位
     * 
     * @param userId 用户ID
     * @param matchId 匹配记录ID
     * @return 是否置顶成功
     */
    boolean pinMatch(Long userId, Long matchId);
    
    /**
     * 取消置顶匹配岗位
     * 
     * @param userId 用户ID
     * @param matchId 匹配记录ID
     * @return 是否取消成功
     */
    boolean unpinMatch(Long userId, Long matchId);
    
    /**
     * 获取收藏的匹配列表
     * 
     * @param userId 用户ID
     * @return 匹配记录列表
     */
    List<MatchRecord> getFavoriteMatches(Long userId);
    
    /**
     * 获取置顶的匹配列表
     * 
     * @param userId 用户ID
     * @return 匹配记录列表
     */
    List<MatchRecord> getPinnedMatches(Long userId);
    
    /**
     * 获取匹配结果的完整详情（包含岗位市场信息、薪资趋势、晋升路径等）
     * 
     * @param matchId 匹配记录ID
     * @return 包含匹配记录和关联数据的Map
     */
    Map<String, Object> getMatchFullDetail(Long matchId);
    
    /**
     * 重试失败的匹配计算
     * 
     * @param matchId 匹配记录ID
     * @return 重试是否成功
     */
    boolean retryFailedMatch(Long matchId);
    
    /**
     * 终止超时的匹配计算
     * 
     * @param matchId 匹配记录ID
     * @return 终止是否成功
     */
    boolean terminateTimeoutMatch(Long matchId);
}
