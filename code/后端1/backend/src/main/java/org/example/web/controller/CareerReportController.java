package org.example.web.controller;

import java.util.List;
import java.util.Map;

import org.example.web.entity.CareerReport;
import org.example.web.entity.CareerReportHistory;
import org.example.web.entity.Result;
import org.example.web.service.CareerReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 职业报告控制器
 * 提供职业报告相关的RESTful API接口
 * 注意：实际报告内容生成由AI服务器完成，本控制器主要负责接收请求和返回结果
 * 包括报告创建、查询、更新、删除、分享等功能
 * 
 * @author 系统生成
 * @version 1.0
 */
@RestController
@RequestMapping("/career-report")
public class CareerReportController {

    @Autowired
    private CareerReportService careerReportService;

    /**
     * 创建职业报告
     * 
     * @param careerReport 报告对象
     * @return 包含报告信息的Result对象
     */
    @PostMapping("/create")
    public Result createCareerReport(@RequestBody CareerReport careerReport) {
        try {
            CareerReport createdReport = careerReportService.createCareerReport(careerReport);
            if (createdReport != null) {
                return Result.success("职业报告创建成功", createdReport);
            } else {
                return Result.error("职业报告创建失败");
            }
        } catch (Exception e) {
            return Result.error("创建职业报告时发生错误: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取职业报告
     * 
     * @param id 报告ID
     * @return 包含报告信息的Result对象
     */
    @GetMapping("/{id}")
    public Result getCareerReport(@PathVariable Long id) {
        try {
            CareerReport report = careerReportService.getCareerReportById(id);
            if (report != null) {
                return Result.success("获取职业报告成功", report);
            } else {
                return Result.error("未找到职业报告");
            }
        } catch (Exception e) {
            return Result.error("获取职业报告时发生错误: " + e.getMessage());
        }
    }

    /**
     * 根据用户ID获取职业报告列表
     * 
     * @param userId 用户ID
     * @return 包含报告列表的Result对象
     */
    @GetMapping("/user/{userId}")
    public Result getCareerReportsByUser(@PathVariable Long userId) {
        try {
            List<CareerReport> reports = careerReportService.getCareerReportsByUserId(userId);
            return Result.success("获取用户职业报告成功", reports);
        } catch (Exception e) {
            return Result.error("获取用户职业报告时发生错误: " + e.getMessage());
        }
    }

    /**
     * 根据匹配ID获取职业报告
     * 
     * @param matchId 匹配记录ID
     * @return 包含报告信息的Result对象
     */
    @GetMapping("/match/{matchId}")
    public Result getCareerReportByMatch(@PathVariable Long matchId) {
        try {
            CareerReport report = careerReportService.getCareerReportByMatchId(matchId);
            if (report != null) {
                return Result.success("获取匹配报告成功", report);
            } else {
                return Result.error("未找到匹配报告");
            }
        } catch (Exception e) {
            return Result.error("获取匹配报告时发生错误: " + e.getMessage());
        }
    }

    /**
     * 生成职业报告
     * 启动异步报告生成流程，调用AI服务器生成报告内容
     * 注意：实际报告生成由AI服务器完成，本接口负责触发AI生成
     * 
     * @param userId 用户ID
     * @param matchId 匹配记录ID
     * @param reportType 报告类型：1-职业探索报告，2-目标设定报告，3-完整职业规划报告
     * @return 包含报告生成状态和报告ID的Result对象
     */
    @PostMapping("/generate")
    public Result generateCareerReport(@RequestParam Long userId,
                                       @RequestParam(required = false) Long matchId,
                                       @RequestParam Integer reportType) {
        try {
            Map<String, Object> result = careerReportService.generateCareerReport(userId, matchId, reportType);
            if (result != null && !result.containsKey("error")) {
                return Result.success("报告生成请求已发送到AI服务器", result);
            } else {
                return Result.error(result != null ? (String) result.get("error") : "报告生成失败");
            }
        } catch (Exception e) {
            return Result.error("调用AI服务器生成报告时发生错误: " + e.getMessage());
        }
    }

    /**
     * 获取报告生成状态
     * 
     * @param reportId 报告ID
     * @return 包含生成状态的Result对象
     */
    @GetMapping("/status/{reportId}")
    public Result getReportGenerationStatus(@PathVariable Long reportId) {
        try {
            Map<String, Object> status = careerReportService.getReportGenerationStatus(reportId);
            if (status.containsKey("error")) {
                return Result.error((String) status.get("error"));
            }
            return Result.success("获取报告生成状态成功", status);
        } catch (Exception e) {
            return Result.error("获取报告生成状态时发生错误: " + e.getMessage());
        }
    }

    /**
     * 更新报告内容
     * 
     * @param reportId 报告ID
     * @param reportContent 报告内容（JSON格式）
     * @param changeReason 修改原因
     * @return 包含更新后报告的Result对象
     */
    @PutMapping("/content/{reportId}")
    public Result updateReportContent(@PathVariable Long reportId,
                                      @RequestParam String reportContent,
                                      @RequestParam String changeReason) {
        try {
            CareerReport updatedReport = careerReportService.updateReportContent(reportId, reportContent, changeReason);
            if (updatedReport != null) {
                return Result.success("报告内容更新成功", updatedReport);
            } else {
                return Result.error("报告内容更新失败");
            }
        } catch (Exception e) {
            return Result.error("更新报告内容时发生错误: " + e.getMessage());
        }
    }

    /**
     * 更新报告状态
     * 
     * @param reportId 报告ID
     * @param status 报告状态：1-草稿，2-生成中，3-已生成，4-已修改，5-已确认
     * @return 包含操作结果的Result对象
     */
    @PutMapping("/status/{reportId}")
    public Result updateReportStatus(@PathVariable Long reportId,
                                     @RequestParam Integer status) {
        try {
            boolean success = careerReportService.updateReportStatus(reportId, status);
            if (success) {
                return Result.success("报告状态更新成功");
            } else {
                return Result.error("报告状态更新失败");
            }
        } catch (Exception e) {
            return Result.error("更新报告状态时发生错误: " + e.getMessage());
        }
    }

    /**
     * 逻辑删除职业报告
     * 
     * @param id 报告ID
     * @return 包含操作结果的Result对象
     */
    @DeleteMapping("/{id}")
    public Result deleteCareerReport(@PathVariable Long id) {
        try {
            boolean success = careerReportService.deleteCareerReport(id);
            if (success) {
                return Result.success("职业报告删除成功");
            } else {
                return Result.error("职业报告删除失败");
            }
        } catch (Exception e) {
            return Result.error("删除职业报告时发生错误: " + e.getMessage());
        }
    }

    /**
     * 获取报告历史版本列表
     * 
     * @param reportId 报告ID
     * @return 包含历史版本列表的Result对象
     */
    @GetMapping("/history/{reportId}")
    public Result getReportHistory(@PathVariable Long reportId) {
        try {
            List<CareerReportHistory> history = careerReportService.getReportHistory(reportId);
            return Result.success("获取报告历史版本成功", history);
        } catch (Exception e) {
            return Result.error("获取报告历史版本时发生错误: " + e.getMessage());
        }
    }

    /**
     * 获取特定版本报告
     * 
     * @param reportId 报告ID
     * @param version 版本号
     * @return 包含历史版本报告的Result对象
     */
    @GetMapping("/history/{reportId}/version/{version}")
    public Result getReportByVersion(@PathVariable Long reportId,
                                     @PathVariable Integer version) {
        try {
            CareerReportHistory history = careerReportService.getReportByVersion(reportId, version);
            if (history != null) {
                return Result.success("获取报告历史版本成功", history);
            } else {
                return Result.error("未找到指定版本的报告");
            }
        } catch (Exception e) {
            return Result.error("获取报告历史版本时发生错误: " + e.getMessage());
        }
    }

    /**
     * 恢复报告到指定历史版本
     * 
     * @param reportId 报告ID
     * @param version 要恢复的版本号
     * @param changeReason 恢复原因
     * @return 包含恢复后报告的Result对象
     */
    @PostMapping("/restore/{reportId}")
    public Result restoreReportToVersion(@PathVariable Long reportId,
                                         @RequestParam Integer version,
                                         @RequestParam String changeReason) {
        try {
            CareerReport restoredReport = careerReportService.restoreReportToVersion(reportId, version, changeReason);
            if (restoredReport != null) {
                return Result.success("报告恢复成功", restoredReport);
            } else {
                return Result.error("报告恢复失败");
            }
        } catch (Exception e) {
            return Result.error("恢复报告时发生错误: " + e.getMessage());
        }
    }

    /**
     * 批量生成报告
     * 
     * @param userIds 用户ID列表
     * @param matchIds 匹配记录ID列表
     * @param reportType 报告类型
     * @return 包含批量生成结果的Result对象
     */
    @PostMapping("/batch-generate")
    public Result batchGenerateReports(@RequestParam List<Long> userIds,
                                       @RequestParam(required = false) List<Long> matchIds,
                                       @RequestParam Integer reportType) {
        try {
            List<CareerReport> reports = careerReportService.batchGenerateReports(userIds, matchIds, reportType);
            return Result.success("批量报告生成请求已发送", reports);
        } catch (Exception e) {
            return Result.error("批量生成报告时发生错误: " + e.getMessage());
        }
    }

    // ============== 高级报告功能API ==============
    
    /**
     * 生成指定类型的职业报告（高级）
     * 
     * @param userId 用户ID
     * @param matchId 匹配记录ID（可选）
     * @param reportType 报告类型
     * @param templateId 模板ID（可选）
     * @return 包含生成结果的Result对象
     */
    @PostMapping("/generate-by-type")
    public Result generateReportByType(@RequestParam Long userId,
                                       @RequestParam(required = false) Long matchId,
                                       @RequestParam Integer reportType,
                                       @RequestParam(required = false) Long templateId) {
        try {
            Map<String, Object> result = careerReportService.generateReportByType(userId, matchId, reportType, templateId);
            return Result.success("高级报告生成请求已发送", result);
        } catch (Exception e) {
            return Result.error("高级报告生成时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 重试报告生成
     * 
     * @param reportId 报告ID
     * @return 包含操作结果的Result对象
     */
    @PostMapping("/retry/{reportId}")
    public Result retryReportGeneration(@PathVariable Long reportId) {
        try {
            boolean success = careerReportService.retryReportGeneration(reportId);
            if (success) {
                return Result.success("报告重试生成成功");
            } else {
                return Result.error("报告重试生成失败");
            }
        } catch (Exception e) {
            return Result.error("重试报告生成时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 比较两个报告版本的差异
     * 
     * @param reportId 报告ID
     * @param version1 版本1
     * @param version2 版本2
     * @return 包含版本差异的Result对象
     */
    @GetMapping("/compare/{reportId}")
    public Result compareReportVersions(@PathVariable Long reportId,
                                        @RequestParam Integer version1,
                                        @RequestParam Integer version2) {
        try {
            Map<String, Object> comparison = careerReportService.compareReportVersions(reportId, version1, version2);
            if (comparison.containsKey("error")) {
                return Result.error((String) comparison.get("error"));
            }
            return Result.success("版本比较成功", comparison);
        } catch (Exception e) {
            return Result.error("比较报告版本时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 生成能力排名分析
     * 
     * @param userId 用户ID
     * @param reportId 报告ID（可选）
     * @return 包含能力排名分析的Result对象
     */
    @GetMapping("/ability-ranking/{userId}")
    public Result generateAbilityRankingAnalysis(@PathVariable Long userId,
                                                 @RequestParam(required = false) Long reportId) {
        try {
            Map<String, Object> analysis = careerReportService.generateAbilityRankingAnalysis(userId, reportId);
            return Result.success("能力排名分析请求已发送", analysis);
        } catch (Exception e) {
            return Result.error("生成能力排名分析时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 导出报告为指定格式
     * 
     * @param reportId 报告ID
     * @param format 导出格式：pdf, word, html
     * @return 包含导出信息的Result对象
     */
    @GetMapping("/export/{reportId}")
    public Result exportReport(@PathVariable Long reportId,
                               @RequestParam String format) {
        try {
            Map<String, Object> exportInfo = careerReportService.exportReport(reportId, format);
            if (exportInfo.containsKey("error")) {
                return Result.error((String) exportInfo.get("error"));
            }
            return Result.success("导出请求已处理", exportInfo);
        } catch (Exception e) {
            return Result.error("导出报告时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 生成报告分享链接
     * 
     * @param reportId 报告ID
     * @param permissionLevel 权限等级：1-仅自己可见，2-对指导老师可见，3-对授权企业可见
     * @param expireHours 过期时间（小时），0表示永不过期
     * @return 包含分享链接信息的Result对象
     */
    @PostMapping("/share/{reportId}")
    public Result generateShareLink(@PathVariable Long reportId,
                                    @RequestParam Integer permissionLevel,
                                    @RequestParam(defaultValue = "0") Integer expireHours) {
        try {
            Map<String, Object> shareInfo = careerReportService.generateShareLink(reportId, permissionLevel, expireHours);
            if (shareInfo.containsKey("error")) {
                return Result.error((String) shareInfo.get("error"));
            }
            return Result.success("分享链接生成成功", shareInfo);
        } catch (Exception e) {
            return Result.error("生成分享链接时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 验证报告分享链接
     * 
     * @param shareToken 分享令牌
     * @return 包含验证结果的Result对象
     */
    @GetMapping("/share/validate")
    public Result validateShareLink(@RequestParam String shareToken) {
        try {
            Map<String, Object> validation = careerReportService.validateShareLink(shareToken);
            return Result.success("分享链接验证完成", validation);
        } catch (Exception e) {
            return Result.error("验证分享链接时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 撤销报告分享链接
     * 
     * @param shareToken 分享令牌
     * @return 包含操作结果的Result对象
     */
    @DeleteMapping("/share/revoke")
    public Result revokeShareLink(@RequestParam String shareToken) {
        try {
            boolean success = careerReportService.revokeShareLink(shareToken);
            if (success) {
                return Result.success("分享链接撤销成功");
            } else {
                return Result.error("分享链接撤销失败");
            }
        } catch (Exception e) {
            return Result.error("撤销分享链接时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取报告的所有分享链接
     * 
     * @param reportId 报告ID
     * @return 包含分享链接列表的Result对象
     */
    @GetMapping("/share/{reportId}/all")
    public Result getReportShareLinks(@PathVariable Long reportId) {
        try {
            List<Map<String, Object>> shareLinks = careerReportService.getReportShareLinks(reportId);
            return Result.success("获取分享链接成功", shareLinks);
        } catch (Exception e) {
            return Result.error("获取分享链接时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户的报告列表（支持按类型和状态筛选）
     * 
     * @param userId 用户ID
     * @param reportType 报告类型（可选）
     * @param status 报告状态（可选）
     * @return 包含筛选后报告列表的Result对象
     */
    @GetMapping("/user/{userId}/filtered")
    public Result getUserReportsWithFilter(@PathVariable Long userId,
                                           @RequestParam(required = false) Integer reportType,
                                           @RequestParam(required = false) Integer status) {
        try {
            List<CareerReport> reports = careerReportService.getUserReportsWithFilter(userId, reportType, status);
            return Result.success("获取筛选后的报告列表成功", reports);
        } catch (Exception e) {
            return Result.error("获取筛选后的报告列表时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 确认报告（用户确认报告内容）
     * 
     * @param reportId 报告ID
     * @return 包含操作结果的Result对象
     */
    @PostMapping("/confirm/{reportId}")
    public Result confirmReport(@PathVariable Long reportId) {
        try {
            boolean success = careerReportService.confirmReport(reportId);
            if (success) {
                return Result.success("报告确认成功");
            } else {
                return Result.error("报告确认失败");
            }
        } catch (Exception e) {
            return Result.error("确认报告时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 提交报告反馈
     * 
     * @param reportId 报告ID
     * @param feedback 反馈内容
     * @param feedbackScore 反馈评分（1-5分）
     * @return 包含操作结果的Result对象
     */
    @PostMapping("/feedback/{reportId}")
    public Result submitReportFeedback(@PathVariable Long reportId,
                                       @RequestParam String feedback,
                                       @RequestParam Integer feedbackScore) {
        try {
            boolean success = careerReportService.submitReportFeedback(reportId, feedback, feedbackScore);
            if (success) {
                return Result.success("报告反馈提交成功");
            } else {
                return Result.error("报告反馈提交失败");
            }
        } catch (Exception e) {
            return Result.error("提交报告反馈时发生错误: " + e.getMessage());
        }
    }
}
