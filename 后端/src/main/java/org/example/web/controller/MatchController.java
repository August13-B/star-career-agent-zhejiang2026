package org.example.web.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.example.web.entity.MatchDetail;
import org.example.web.entity.MatchRecord;
import org.example.web.entity.Result;
import org.example.web.service.MatchService;
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
 * 人岗匹配控制器
 * 提供人岗匹配相关的RESsadwTful API接口
 * 注意：实际的匹配计算和报告生成由AI服务器完成，本控制器主要负责接收请求和返回结果
 * 包括匹配记录创建、查询、更新、删除等功能
 * 
 * @author 系统生成
 * @version 1.0
 */
@RestController
@RequestMapping("/match")
public class MatchController {

    @Autowired
    private MatchService matchService;

    /**
     * 创建人岗匹配记录（同时触发AI计算）
     * 合并创建和计算两个步骤，创建记录后立即触发AI匹配计算
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @param level 职级（1-入门，2-中级，3-高级），默认为1
     * @return 包含匹配记录信息和AI计算触发状态的Result对象
     */
    @PostMapping("/create")
    public Result createMatchRecord(@RequestParam Long userId,
                                    @RequestParam Long jobId,
                                    @RequestParam(defaultValue = "1") Integer level) {
        try {
            // 调用calculateMatch方法，同时创建记录和触发AI计算
            Map<String, Object> result = matchService.calculateMatch(userId, jobId, level);
            if (result != null) {
                return Result.success("匹配记录创建成功并已触发AI计算", result);
            } else {
                return Result.error("匹配记录创建失败");
            }
        } catch (Exception e) {
            return Result.error("创建匹配记录时发生错误: " + e.getMessage());
        }
    }

    /**
     * 计算人岗匹配
     * 启动异步匹配计算流程，调用AI服务器进行匹配计算
     * 注意：实际匹配计算由AI服务器完成，本接口负责触发AI计算
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @param level 职级，默认为1
     * @return 包含匹配计算状态和匹配ID的Result对象
     */
    @PostMapping("/calculate")
    public Result calculateMatch(@RequestParam Long userId,
                                 @RequestParam Long jobId,
                                 @RequestParam(defaultValue = "1") Integer level) {
        try {
            Map<String, Object> result = matchService.calculateMatch(userId, jobId, level);
            if (result != null) {
                return Result.success("匹配计算请求已发送到AI服务器", result);
            } else {
                return Result.error("匹配计算失败");
            }
        } catch (Exception e) {
            return Result.error("调用AI服务器进行匹配计算时发生错误: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取匹配记录详情
     * 
     * @param id 匹配记录ID
     * @return 包含匹配记录信息的Result对象
     */
    @GetMapping("/record/{id}")
    public Result getMatchRecord(@PathVariable Long id) {
        try {
            MatchRecord matchRecord = matchService.getMatchRecordById(id);
            if (matchRecord != null) {
                return Result.success("获取匹配记录成功", matchRecord);
            } else {
                return Result.error("未找到匹配记录");
            }
        } catch (Exception e) {
            return Result.error("获取匹配记录时发生错误: " + e.getMessage());
        }
    }

    /**
     * 根据用户ID获取匹配记录列表
     * 
     * @param userId 用户ID
     * @return 包含匹配记录列表的Result对象
     */
    @GetMapping("/user/{userId}")
    public Result getMatchRecordsByUser(@PathVariable Long userId) {
        try {
            List<MatchRecord> matchRecords = matchService.getMatchRecordsByUserId(userId);
            return Result.success("获取用户匹配记录成功", matchRecords);
        } catch (Exception e) {
            return Result.error("获取用户匹配记录时发生错误: " + e.getMessage());
        }
    }

    /**
     * 根据岗位ID获取匹配记录列表
     * 
     * @param jobId 岗位ID
     * @return 包含匹配记录列表的Result对象
     */
    @GetMapping("/job/{jobId}")
    public Result getMatchRecordsByJob(@PathVariable Long jobId) {
        try {
            List<MatchRecord> matchRecords = matchService.getMatchRecordsByJobId(jobId);
            return Result.success("获取岗位匹配记录成功", matchRecords);
        } catch (Exception e) {
            return Result.error("获取岗位匹配记录时发生错误: " + e.getMessage());
        }
    }

    /**
     * 获取匹配报告
     * 从数据库获取匹配记录和详情，如果需要生成详细报告则调用AI服务器
     * 注意：详细报告摘要和建议应由AI服务器生成
     * 
     * @param matchId 匹配记录ID
     * @return 包含匹配报告的Result对象
     */
    @GetMapping("/report/{matchId}")
    public Result getMatchReport(@PathVariable Long matchId) {
        try {
            Map<String, Object> report = matchService.getMatchReport(matchId);
            if (report.containsKey("error")) {
                return Result.error((String) report.get("error"));
            }
            return Result.success("获取匹配报告成功", report);
        } catch (Exception e) {
            return Result.error("获取匹配报告时发生错误: " + e.getMessage());
        }
    }

    /**
     * 更新匹配状态
     * 
     * @param id 匹配记录ID
     * @param matchStatus 匹配状态（0-未生成，1-生成中，2-已完成，3-失败）
     * @return 包含操作结果的Result对象
     */
    @PutMapping("/status/{id}")
    public Result updateMatchStatus(@PathVariable Long id,
                                    @RequestParam Integer matchStatus) {
        try {
            boolean success = matchService.updateMatchStatus(id, matchStatus);
            if (success) {
                return Result.success("匹配状态更新成功");
            } else {
                return Result.error("匹配状态更新失败");
            }
        } catch (Exception e) {
            return Result.error("更新匹配状态时发生错误: " + e.getMessage());
        }
    }

    /**
     * 更新匹配结果
     * 
     * @param id 匹配记录ID
     * @param matchResult 匹配结果（1-强烈推荐，2-推荐，3-一般，4-不推荐）
     * @return 包含操作结果的Result对象
     */
    @PutMapping("/result/{id}")
    public Result updateMatchResult(@PathVariable Long id,
                                    @RequestParam Integer matchResult) {
        try {
            boolean success = matchService.updateMatchResult(id, matchResult);
            if (success) {
                return Result.success("匹配结果更新成功");
            } else {
                return Result.error("匹配结果更新失败");
            }
        } catch (Exception e) {
            return Result.error("更新匹配结果时发生错误: " + e.getMessage());
        }
    }

    /**
     * 逻辑删除匹配记录
     * 将is_deleted字段置为1，实现软删除
     * 
     * @param id 匹配记录ID
     * @return 包含操作结果的Result对象
     */
    @DeleteMapping("/record/{id}")
    public Result deleteMatchRecord(@PathVariable Long id) {
        try {
            boolean success = matchService.deleteMatchRecord(id);
            if (success) {
                return Result.success("匹配记录删除成功");
            } else {
                return Result.error("匹配记录删除失败");
            }
        } catch (Exception e) {
            return Result.error("删除匹配记录时发生错误: " + e.getMessage());
        }
    }

    /**
     * 获取匹配详情列表
     * 获取匹配记录的10个维度详细分析信息
     * 
     * @param matchId 匹配记录ID
     * @return 包含匹配详情列表的Result对象
     */
    @GetMapping("/detail/{matchId}")
    public Result getMatchDetails(@PathVariable Long matchId) {
        try {
            List<MatchDetail> matchDetails = matchService.getMatchDetailsByMatchId(matchId);
            return Result.success("获取匹配详情成功", matchDetails);
        } catch (Exception e) {
            return Result.error("获取匹配详情时发生错误: " + e.getMessage());
        }
    }

    /**
     * 更新匹配分数
     * 批量更新匹配记录的各项分数
     * 注意：实际分数应由AI服务器计算后更新，此接口主要用于手动调整或AI服务器回调更新
     * 
     * @param id 匹配记录ID
     * @param scores 分数Map，key为分数字段名，value为分数值
     * @return 包含操作结果的Result对象
     */
    @PutMapping("/scores/{id}")
    public Result updateMatchScores(@PathVariable Long id,
                                    @RequestBody Map<String, BigDecimal> scores) {
        try {
            boolean success = matchService.updateMatchScores(id, scores);
            if (success) {
                return Result.success("匹配分数更新成功");
            } else {
                return Result.error("匹配分数更新失败");
            }
        } catch (Exception e) {
            return Result.error("更新匹配分数时发生错误: " + e.getMessage());
        }
    }

    /**
     * 批量匹配
     * 对多个用户和多个岗位进行批量匹配计算，调用AI服务器处理
     * 注意：实际匹配计算由AI服务器完成，本接口负责创建记录并触发批量AI计算
     * 
     * @param userIds 用户ID列表
     * @param jobIds 岗位ID列表
     * @return 包含批量匹配结果的Result对象
     */
    @PostMapping("/batch")
    public Result batchMatch(@RequestParam List<Long> userIds,
                             @RequestParam List<Long> jobIds) {
        try {
            List<Map<String, Object>> results = matchService.batchMatch(userIds, jobIds);
            return Result.success("批量匹配请求已发送到AI服务器", results);
        } catch (Exception e) {
            return Result.error("调用AI服务器进行批量匹配时发生错误: " + e.getMessage());
        }
    }

    /**
     * 获取用户与岗位的匹配记录
     * 查询特定用户和岗位的匹配记录
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @return 包含匹配记录的Result对象
     */
    @GetMapping("/user-job")
    public Result getMatchRecordByUserAndJob(@RequestParam Long userId,
                                             @RequestParam Long jobId) {
        try {
            MatchRecord matchRecord = matchService.getMatchRecordByUserAndJob(userId, jobId);
            if (matchRecord != null) {
                return Result.success("获取匹配记录成功", matchRecord);
            } else {
                return Result.error("未找到匹配记录");
            }
        } catch (Exception e) {
            return Result.error("获取匹配记录时发生错误: " + e.getMessage());
        }
    }

    // ============== 高级匹配功能API ==============
    
    /**
     * 自动触发全量人岗匹配
     * 在学生画像生成/更新、能力数据变更后自动调用
     * 注意：此接口应由事件监听器触发，实际匹配计算由AI服务器完成
     * 
     * @param userId 用户ID
     * @param triggerType 触发类型：1-画像生成，2-画像更新，3-能力数据变更
     * @return 包含操作结果的Result对象
     */
    @PostMapping("/trigger-auto-match")
    public Result triggerAutoMatch(@RequestParam Long userId,
                                   @RequestParam Integer triggerType) {
        try {
            boolean success = matchService.triggerAutoMatch(userId, triggerType);
            if (success) {
                return Result.success("自动匹配触发成功");
            } else {
                return Result.error("自动匹配触发失败");
            }
        } catch (Exception e) {
            return Result.error("触发自动匹配时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 硬门槛一票否决校验
     * 先校验学历、实习经历2个硬门槛维度，不达标直接终止匹配
     * 注意：此接口应在AI计算前调用，如果返回false则直接设置匹配结果为不推荐
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @return 包含校验结果的Result对象
     */
    @GetMapping("/check-hard-threshold")
    public Result checkHardThreshold(@RequestParam Long userId,
                                     @RequestParam Long jobId) {
        try {
            boolean passed = matchService.checkHardThreshold(userId, jobId);
            if (passed) {
                return Result.success("硬门槛校验通过", true);
            } else {
                return Result.success("硬门槛校验不通过", false);
            }
        } catch (Exception e) {
            return Result.error("硬门槛校验时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 增量匹配优化
     * 学生仅更新单维度数据时，仅重新计算对应维度的匹配分
     * 注意：此优化需要AI服务器支持增量计算，如果AI不支持则回退到全量计算
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @param updatedDimension 更新的维度代码（dim1-dim10）
     * @return 包含增量匹配结果的Result对象
     */
    @PostMapping("/incremental-match")
    public Result incrementalMatch(@RequestParam Long userId,
                                   @RequestParam Long jobId,
                                   @RequestParam String updatedDimension) {
        try {
            Map<String, Object> result = matchService.incrementalMatch(userId, jobId, updatedDimension);
            return Result.success("增量匹配请求已发送", result);
        } catch (Exception e) {
            return Result.error("增量匹配时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取匹配历史记录（支持筛选）
     * 
     * @param userId 用户ID（必填）
     * @param jobId 岗位ID（可选）
     * @param startTime 开始时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param endTime 结束时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param matchResult 匹配结果（可选，1-4）
     * @return 包含匹配历史记录的Result对象
     */
    @GetMapping("/history")
    public Result getMatchHistory(@RequestParam Long userId,
                                  @RequestParam(required = false) Long jobId,
                                  @RequestParam(required = false) String startTime,
                                  @RequestParam(required = false) String endTime,
                                  @RequestParam(required = false) Integer matchResult) {
        try {
            List<MatchRecord> history = matchService.getMatchHistory(userId, jobId, startTime, endTime, matchResult);
            return Result.success("获取匹配历史成功", history);
        } catch (Exception e) {
            return Result.error("获取匹配历史时发生错误: " + e.getMessage());
        }
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
     * @return 包含匹配结果的Result对象
     */
    @GetMapping("/filtered-results")
    public Result getMatchResultsWithFilter(@RequestParam Long userId,
                                            @RequestParam(defaultValue = "match_score") String sortBy,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) String industry,
                                            @RequestParam(required = false) String city,
                                            @RequestParam(required = false) BigDecimal minSalary,
                                            @RequestParam(required = false) BigDecimal maxSalary) {
        try {
            List<MatchRecord> results = matchService.getMatchResultsWithFilter(userId, sortBy, sortOrder, 
                                                                                industry, city, minSalary, maxSalary);
            return Result.success("获取筛选后的匹配结果成功", results);
        } catch (Exception e) {
            return Result.error("获取筛选后的匹配结果时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 收藏匹配岗位
     * 
     * @param userId 用户ID
     * @param matchId 匹配记录ID
     * @return 包含操作结果的Result对象
     */
    @PostMapping("/favorite")
    public Result favoriteMatch(@RequestParam Long userId,
                                @RequestParam Long matchId) {
        try {
            boolean success = matchService.favoriteMatch(userId, matchId);
            if (success) {
                return Result.success("收藏匹配岗位成功");
            } else {
                return Result.error("收藏匹配岗位失败");
            }
        } catch (Exception e) {
            return Result.error("收藏匹配岗位时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 取消收藏匹配岗位
     * 
     * @param userId 用户ID
     * @param matchId 匹配记录ID
     * @return 包含操作结果的Result对象
     */
    @DeleteMapping("/favorite")
    public Result unfavoriteMatch(@RequestParam Long userId,
                                  @RequestParam Long matchId) {
        try {
            boolean success = matchService.unfavoriteMatch(userId, matchId);
            if (success) {
                return Result.success("取消收藏匹配岗位成功");
            } else {
                return Result.error("取消收藏匹配岗位失败");
            }
        } catch (Exception e) {
            return Result.error("取消收藏匹配岗位时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 置顶匹配岗位
     * 
     * @param userId 用户ID
     * @param matchId 匹配记录ID
     * @return 包含操作结果的Result对象
     */
    @PostMapping("/pin")
    public Result pinMatch(@RequestParam Long userId,
                           @RequestParam Long matchId) {
        try {
            boolean success = matchService.pinMatch(userId, matchId);
            if (success) {
                return Result.success("置顶匹配岗位成功");
            } else {
                return Result.error("置顶匹配岗位失败");
            }
        } catch (Exception e) {
            return Result.error("置顶匹配岗位时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 取消置顶匹配岗位
     * 
     * @param userId 用户ID
     * @param matchId 匹配记录ID
     * @return 包含操作结果的Result对象
     */
    @DeleteMapping("/pin")
    public Result unpinMatch(@RequestParam Long userId,
                             @RequestParam Long matchId) {
        try {
            boolean success = matchService.unpinMatch(userId, matchId);
            if (success) {
                return Result.success("取消置顶匹配岗位成功");
            } else {
                return Result.error("取消置顶匹配岗位失败");
            }
        } catch (Exception e) {
            return Result.error("取消置顶匹配岗位时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取收藏的匹配列表
     * 
     * @param userId 用户ID
     * @return 包含收藏匹配列表的Result对象
     */
    @GetMapping("/favorites")
    public Result getFavoriteMatches(@RequestParam Long userId) {
        try {
            List<MatchRecord> favorites = matchService.getFavoriteMatches(userId);
            return Result.success("获取收藏匹配列表成功", favorites);
        } catch (Exception e) {
            return Result.error("获取收藏匹配列表时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取置顶的匹配列表
     * 
     * @param userId 用户ID
     * @return 包含置顶匹配列表的Result对象
     */
    @GetMapping("/pinned")
    public Result getPinnedMatches(@RequestParam Long userId) {
        try {
            List<MatchRecord> pinned = matchService.getPinnedMatches(userId);
            return Result.success("获取置顶匹配列表成功", pinned);
        } catch (Exception e) {
            return Result.error("获取置顶匹配列表时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取匹配结果的完整详情（包含岗位市场信息、薪资趋势、晋升路径等）
     * 
     * @param matchId 匹配记录ID
     * @return 包含完整详情的Result对象
     */
    @GetMapping("/full-detail/{matchId}")
    public Result getMatchFullDetail(@PathVariable Long matchId) {
        try {
            Map<String, Object> fullDetail = matchService.getMatchFullDetail(matchId);
            if (fullDetail.containsKey("error")) {
                return Result.error((String) fullDetail.get("error"));
            }
            return Result.success("获取匹配完整详情成功", fullDetail);
        } catch (Exception e) {
            return Result.error("获取匹配完整详情时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 重试失败的匹配计算
     * 
     * @param matchId 匹配记录ID
     * @return 包含操作结果的Result对象
     */
    @PostMapping("/retry/{matchId}")
    public Result retryFailedMatch(@PathVariable Long matchId) {
        try {
            boolean success = matchService.retryFailedMatch(matchId);
            if (success) {
                return Result.success("重试匹配计算成功");
            } else {
                return Result.error("重试匹配计算失败");
            }
        } catch (Exception e) {
            return Result.error("重试匹配计算时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 终止超时的匹配计算
     * 
     * @param matchId 匹配记录ID
     * @return 包含操作结果的Result对象
     */
    @PostMapping("/terminate/{matchId}")
    public Result terminateTimeoutMatch(@PathVariable Long matchId) {
        try {
            boolean success = matchService.terminateTimeoutMatch(matchId);
            if (success) {
                return Result.success("终止超时匹配计算成功");
            } else {
                return Result.error("终止超时匹配计算失败");
            }
        } catch (Exception e) {
            return Result.error("终止超时匹配计算时发生错误: " + e.getMessage());
        }
    }
}
