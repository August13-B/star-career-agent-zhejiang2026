package org.example.web.service;

import java.util.List;
import java.util.Map;

import org.example.web.entity.CareerReport;
import org.example.web.entity.CareerReportHistory;

public interface CareerReportService {
    
    // 报告创建与获取
    CareerReport createCareerReport(CareerReport careerReport);
    
    CareerReport getCareerReportById(Long id);
    
    List<CareerReport> getCareerReportsByUserId(Long userId);
    
    CareerReport getCareerReportByMatchId(Long matchId);
    
    List<CareerReport> getCareerReportsByType(Long userId, Integer reportType);
    
    // 报告更新
    CareerReport updateCareerReport(CareerReport careerReport);
    
    boolean updateReportStatus(Long id, Integer status);
    
    boolean updateReportVersion(Long id, Integer version);
    
    boolean updateReportFeedback(Long id, String feedback, Integer feedbackScore);
    
    // 报告删除
    boolean deleteCareerReport(Long id);
    
    // 报告历史版本
    List<CareerReportHistory> getReportHistory(Long reportId);
    
    CareerReportHistory getReportHistoryByVersion(Long reportId, Integer version);
    
    boolean createReportHistory(CareerReportHistory reportHistory);
    
    // 报告生成与处理
    Map<String, Object> generateCareerReport(Long userId, Long matchId, Integer reportType);
    
    Map<String, Object> analyzeReportData(Long reportId);
    
    // 报告模板相关
    Map<String, Object> applyReportTemplate(Long reportId, Long templateId);
    
    // 批量操作
    List<CareerReport> batchGenerateReports(List<Long> userIds, List<Long> matchIds, Integer reportType);
    
    // ============== 高级报告功能 ==============
    
    /**
     * 生成指定类型的职业报告
     * 注意：实际报告内容生成由AI服务器完成，本方法负责触发AI生成
     * 
     * @param userId 用户ID
     * @param matchId 匹配记录ID
     * @param reportType 报告类型：1-职业探索报告，2-目标设定报告，3-完整职业规划报告
     * @param templateId 模板ID（可选）
     * @return 包含报告ID和生成状态的结果Map
     */
    Map<String, Object> generateReportByType(Long userId, Long matchId, Integer reportType, Long templateId);
    
    /**
     * 更新报告内容（报告可修改）
     * 
     * @param reportId 报告ID
     * @param reportContent 报告内容（JSON格式）
     * @param changeReason 修改原因
     * @return 更新后的报告对象
     */
    CareerReport updateReportContent(Long reportId, String reportContent, String changeReason);
    
    /**
     * 获取报告生成状态
     * 
     * @param reportId 报告ID
     * @return 报告状态信息
     */
    Map<String, Object> getReportGenerationStatus(Long reportId);
    
    /**
     * 重试报告生成
     * 当报告生成失败时，可以重新触发生成
     * 
     * @param reportId 报告ID
     * @return 重试是否成功
     */
    boolean retryReportGeneration(Long reportId);
    
    /**
     * 获取报告历史版本列表
     * 
     * @param reportId 报告ID
     * @return 报告历史版本列表
     */
    List<CareerReportHistory> getReportHistoryVersions(Long reportId);
    
    /**
     * 获取特定版本报告
     * 
     * @param reportId 报告ID
     * @param version 版本号
     * @return 报告历史版本对象
     */
    CareerReportHistory getReportByVersion(Long reportId, Integer version);
    
    /**
     * 恢复报告到指定历史版本
     * 
     * @param reportId 报告ID
     * @param version 要恢复的版本号
     * @param changeReason 恢复原因
     * @return 恢复后的报告对象
     */
    CareerReport restoreReportToVersion(Long reportId, Integer version, String changeReason);
    
    /**
     * 比较两个报告版本的差异
     * 
     * @param reportId 报告ID
     * @param version1 版本1
     * @param version2 版本2
     * @return 版本差异信息
     */
    Map<String, Object> compareReportVersions(Long reportId, Integer version1, Integer version2);
    
    /**
     * 生成能力排名分析
     * 注意：能力排名分析需要AI算法支持，计算行业排名和同届学生排名百分比
     * 
     * @param userId 用户ID
     * @param reportId 报告ID（可选，如果为空则基于最新数据）
     * @return 能力排名分析结果
     */
    Map<String, Object> generateAbilityRankingAnalysis(Long userId, Long reportId);
    
    /**
     * 导出报告为指定格式
     * 注意：PDF/Word格式的导出需要集成第三方库或服务
     * 
     * @param reportId 报告ID
     * @param format 导出格式：pdf, word, html
     * @return 导出文件信息（包含文件路径、文件名等）
     */
    Map<String, Object> exportReport(Long reportId, String format);
    
    /**
     * 生成报告分享链接
     * 
     * @param reportId 报告ID
     * @param permissionLevel 权限等级：1-仅自己可见，2-对指导老师可见，3-对授权企业可见
     * @param expireHours 过期时间（小时），0表示永不过期
     * @return 分享链接信息
     */
    Map<String, Object> generateShareLink(Long reportId, Integer permissionLevel, Integer expireHours);
    
    /**
     * 验证报告分享链接
     * 
     * @param shareToken 分享令牌
     * @return 验证结果和报告信息
     */
    Map<String, Object> validateShareLink(String shareToken);
    
    /**
     * 撤销报告分享链接
     * 
     * @param shareToken 分享令牌
     * @return 撤销是否成功
     */
    boolean revokeShareLink(String shareToken);
    
    /**
     * 获取报告的所有分享链接
     * 
     * @param reportId 报告ID
     * @return 分享链接列表
     */
    List<Map<String, Object>> getReportShareLinks(Long reportId);
    
    /**
     * 获取用户的报告列表（支持按类型和状态筛选）
     * 
     * @param userId 用户ID
     * @param reportType 报告类型（可选）
     * @param status 报告状态（可选）
     * @return 报告列表
     */
    List<CareerReport> getUserReportsWithFilter(Long userId, Integer reportType, Integer status);
    
    /**
     * 更新报告状态（完整状态管理）
     * 覆盖草稿、生成中、已生成、已修改、已确认全状态
     * 
     * @param reportId 报告ID
     * @param status 状态：1-草稿，2-生成中，3-已生成，4-已修改，5-已确认
     * @return 更新是否成功
     */
    boolean updateReportStatusWithFullControl(Long reportId, Integer status);
    
    /**
     * 确认报告（用户确认报告内容）
     * 
     * @param reportId 报告ID
     * @return 确认是否成功
     */
    boolean confirmReport(Long reportId);
    
    /**
     * 提交报告反馈
     * 
     * @param reportId 报告ID
     * @param feedback 反馈内容
     * @param feedbackScore 反馈评分（1-5分）
     * @return 提交是否成功
     */
    boolean submitReportFeedback(Long reportId, String feedback, Integer feedbackScore);
}
