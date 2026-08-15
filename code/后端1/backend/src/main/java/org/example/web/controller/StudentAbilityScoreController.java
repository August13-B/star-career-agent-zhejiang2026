package org.example.web.controller;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.example.web.entity.StudentAbilityScore;
import org.example.web.service.StudentAbilityScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ability/score")
@RequiredArgsConstructor
public class StudentAbilityScoreController {

    private static final Logger logger = LoggerFactory.getLogger(StudentAbilityScoreController.class);
    private final StudentAbilityScoreService studentAbilityScoreService;

    // 统一响应结果工具方法
    private Map<String, Object> buildResult(int code, String message, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", data);
        // 打印标准JSON格式返回数据
        logger.info("【评分接口】返回前端标准JSON数据：{}", JSONUtil.toJsonStr(result));
        return result;
    }

    /**
     * 查询所有评分信息
     */
    @GetMapping("/all")
    public Map<String, Object> selectAll() {
        try {
            List<StudentAbilityScore> scores = studentAbilityScoreService.selectAll();
            logger.info("【评分接口】查询所有评分，返回数据：{}", scores);
            return buildResult(200, "查询成功", scores);
        } catch (Exception e) {
            logger.error("【评分接口】查询所有评分失败", e);
            return buildResult(500, "查询失败：" + e.getMessage(), null);
        }
    }

    /**
     * 多条件模糊查询
     */
    @PostMapping("/condition")
    public Map<String, Object> selectByCondition(@RequestBody StudentAbilityScore score) {
        try {
            List<StudentAbilityScore> scores = studentAbilityScoreService.selectByCondition(score);
            logger.info("【评分接口】条件查询，返回数据：{}", scores);
            return buildResult(200, "查询成功", scores);
        } catch (Exception e) {
            logger.error("【评分接口】条件查询失败，查询条件：{}", score, e);
            return buildResult(500, "查询失败：" + e.getMessage(), null);
        }
    }

    /**
     * 新增评分信息
     */
    @PostMapping("/insert")
    public Map<String, Object> insert(@RequestBody StudentAbilityScore score) {
        try {
            int insertResult = studentAbilityScoreService.insert(score);
            if (insertResult <= 0) {
                return buildResult(500, "新增失败：插入行数为0", null);
            }
            // 插入成功后，根据用户ID和能力ID查询最新插入的记录
            List<StudentAbilityScore> newScore = studentAbilityScoreService.selectByUserId(score.getUserId());
            logger.info("【评分接口】新增评分，返回数据：{}", newScore);
            return buildResult(200, "新增成功", newScore);
        } catch (Exception e) {
            logger.error("【评分接口】新增评分失败，提交数据：{}", score, e);
            return buildResult(500, "新增失败：" + e.getMessage(), null);
        }
    }

    /**
     * 更新评分信息
     */
    @PutMapping("/update")
    public Map<String, Object> update(@RequestBody StudentAbilityScore score) {
        try {
            int updateResult = studentAbilityScoreService.update(score);
            if (updateResult <= 0) {
                return buildResult(500, "更新失败：更新行数为0", null);
            }
            // 更新成功后，根据ID查询更新后的记录
            List<StudentAbilityScore> updatedScore = studentAbilityScoreService.selectById(score.getId());
            logger.info("【评分接口】更新评分，返回数据：{}", updatedScore);
            return buildResult(200, "更新成功", updatedScore);
        } catch (IllegalArgumentException e) {
            return buildResult(400, "参数错误：" + e.getMessage(), null);
        } catch (RuntimeException e) {
            if ("更新后与原先相同".equals(e.getMessage())) {
                return buildResult(200, "更新后与原先相同", null);
            }
            logger.error("【评分接口】更新评分失败，提交数据：{}", score, e);
            return buildResult(500, "更新失败：" + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("【评分接口】更新评分失败，提交数据：{}", score, e);
            return buildResult(500, "更新失败：" + e.getMessage(), null);
        }
    }

    /**
     * 根据ID查询评分信息
     */
    @GetMapping("/{id}")
    public Map<String, Object> selectById(@PathVariable Long id) {
        try {
            List<StudentAbilityScore> score = studentAbilityScoreService.selectById(id);
            logger.info("【评分接口】根据ID查询，返回数据：{}", score);
            return buildResult(200, "查询成功", score);
        } catch (IllegalArgumentException e) {
            return buildResult(400, "参数错误：" + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("【评分接口】根据ID查询失败，评分ID：{}", id, e);
            return buildResult(500, "查询失败：" + e.getMessage(), null);
        }
    }

    /**
     * 根据ID逻辑删除评分
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteById(@PathVariable Long id) {
        try {
            // 先查询出要删除的记录
            List<StudentAbilityScore> toDelete = studentAbilityScoreService.selectById(id);
            if (toDelete == null || toDelete.isEmpty()) {
                return buildResult(404, "未找到要删除的记录", null);
            }
            int deleteResult = studentAbilityScoreService.deleteById(id);
            if (deleteResult <= 0) {
                return buildResult(500, "删除失败：删除行数为0", null);
            }
            logger.info("【评分接口】删除评分，返回数据：{}", toDelete);
            return buildResult(200, "删除成功", toDelete);
        } catch (IllegalArgumentException e) {
            return buildResult(400, "参数错误：" + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("【评分接口】删除评分失败，评分ID：{}", id, e);
            return buildResult(500, "删除失败：" + e.getMessage(), null);
        }
    }

    /**
     * 批量逻辑删除评分
     */
    @PostMapping("/batchdelete")
    public Map<String, Object> batchDelete(@RequestBody List<Long> ids) {
        try {
            // 先查询出要删除的记录
            List<StudentAbilityScore> toDelete = studentAbilityScoreService.batchSelect(ids);
            if (toDelete == null || toDelete.isEmpty()) {
                return buildResult(404, "未找到要删除的记录", null);
            }
            int deleteResult = studentAbilityScoreService.batchDelete(ids);
            if (deleteResult <= 0) {
                return buildResult(500, "批量删除失败：删除行数为0", null);
            }
            logger.info("【评分接口】批量删除评分，返回数据：{}", toDelete);
            return buildResult(200, "批量删除成功", toDelete);
        } catch (IllegalArgumentException e) {
            return buildResult(400, "参数错误：" + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("【评分接口】批量删除失败，ID列表：{}", ids, e);
            return buildResult(500, "批量删除失败：" + e.getMessage(), null);
        }
    }

    /**
     * 批量查询评分
     */
    @PostMapping("/batchselect")
    public Map<String, Object> batchSelect(@RequestBody List<Long> ids) {
        try {
            List<StudentAbilityScore> scores = studentAbilityScoreService.batchSelect(ids);
            logger.info("【评分接口】批量查询评分，返回数据：{}", scores);
            return buildResult(200, "批量查询成功", scores);
        } catch (IllegalArgumentException e) {
            return buildResult(400, "参数错误：" + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("【评分接口】批量查询失败，ID列表：{}", ids, e);
            return buildResult(500, "批量查询失败：" + e.getMessage(), null);
        }
    }

    /**
     * 根据用户ID查询评分
     */
    @GetMapping("/user/{userId}")
    public Map<String, Object> selectByUserId(@PathVariable Long userId) {
        try {
            List<StudentAbilityScore> scores = studentAbilityScoreService.selectByUserId(userId);
            logger.info("【评分接口】根据用户ID查询评分，返回数据：{}", scores);
            return buildResult(200, "查询成功", scores);
        } catch (IllegalArgumentException e) {
            return buildResult(400, "参数错误：" + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("【评分接口】根据用户ID查询失败，用户ID：{}", userId, e);
            return buildResult(500, "查询失败：" + e.getMessage(), null);
        }
    }

    /**
     * 根据能力ID查询评分
     */
    @GetMapping("/ability/{abilityId}")
    public Map<String, Object> selectByAbilityId(@PathVariable Long abilityId) {
        try {
            List<StudentAbilityScore> scores = studentAbilityScoreService.selectByAbilityId(abilityId);
            logger.info("【评分接口】根据能力ID查询评分，返回数据：{}", scores);
            return buildResult(200, "查询成功", scores);
        } catch (IllegalArgumentException e) {
            return buildResult(400, "参数错误：" + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("【评分接口】根据能力ID查询失败，能力ID：{}", abilityId, e);
            return buildResult(500, "查询失败：" + e.getMessage(), null);
        }
    }
}
