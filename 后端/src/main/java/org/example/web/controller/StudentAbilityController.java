package org.example.web.controller;

import lombok.RequiredArgsConstructor;
import org.example.web.entity.StudentAbility;
import org.example.web.service.StudentAbilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ability")
@RequiredArgsConstructor
public class StudentAbilityController {

    private static final Logger logger = LoggerFactory.getLogger(StudentAbilityController.class);
    private final StudentAbilityService studentAbilityService;

    // 统一响应结果工具方法
    private Map<String, Object> buildResult(int code, String message, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", data);
        return result;
    }

    /**
     * 查询所有能力信息
     */
    @GetMapping("/all")
    public Map<String, Object> selectAll() {
        try {
            List<StudentAbility> abilities = studentAbilityService.selectAll();
            return buildResult(200, "查询成功", abilities);
        } catch (Exception e) {
            logger.error("【能力接口】查询所有能力失败", e);
            return buildResult(500, "查询失败：" + e.getMessage(), null);
        }
    }

    /**
     * 多条件模糊查询
     */
    @PostMapping("/condition")
    public Map<String, Object> selectByCondition(@RequestBody StudentAbility ability) {
        try {
            List<StudentAbility> abilities = studentAbilityService.selectByCondition(ability);
            return buildResult(200, "查询成功", abilities);
        } catch (Exception e) {
            logger.error("【能力接口】条件查询失败，查询条件：{}", ability, e);
            return buildResult(500, "查询失败：" + e.getMessage(), null);
        }
    }

    /**
     * 新增能力信息
     */
    @PostMapping("/insert")
    public Map<String, Object> insert(@RequestBody StudentAbility ability) {
        try {
            List<StudentAbility> newAbility = studentAbilityService.insert(ability);
            return buildResult(200, "新增成功", newAbility);
        } catch (Exception e) {
            logger.error("【能力接口】新增能力失败，提交数据：{}", ability, e);
            return buildResult(500, "新增失败：" + e.getMessage(), null);
        }
    }

    /**
     * 更新能力信息
     */
    @PutMapping("/update")
    public Map<String, Object> update(@RequestBody StudentAbility ability) {
        try {
            List<StudentAbility> updatedAbility = studentAbilityService.update(ability);
            return buildResult(200, "更新成功", updatedAbility);
        } catch (IllegalArgumentException e) {
            return buildResult(400, "参数错误：" + e.getMessage(), null);
        } catch (RuntimeException e) {
            if ("更新后与原先相同".equals(e.getMessage())) {
                return buildResult(200, "更新后与原先相同", null);
            }
            logger.error("【能力接口】更新能力失败，提交数据：{}", ability, e);
            return buildResult(500, "更新失败：" + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("【能力接口】更新能力失败，提交数据：{}", ability, e);
            return buildResult(500, "更新失败：" + e.getMessage(), null);
        }
    }

    /**
     * 根据ID查询能力信息
     */
    @GetMapping("/{id}")
    public Map<String, Object> selectById(@PathVariable Long id) {
        try {
            List<StudentAbility> ability = studentAbilityService.selectById(id);
            return buildResult(200, "查询成功", ability);
        } catch (IllegalArgumentException e) {
            return buildResult(400, "参数错误：" + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("【能力接口】根据ID查询失败，能力ID：{}", id, e);
            return buildResult(500, "查询失败：" + e.getMessage(), null);
        }
    }

    /**
     * 根据ID逻辑删除能力
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteById(@PathVariable Long id) {
        try {
            List<StudentAbility> deletedAbility = studentAbilityService.deleteById(id);
            return buildResult(200, "删除成功", deletedAbility);
        } catch (IllegalArgumentException e) {
            return buildResult(400, "参数错误：" + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("【能力接口】删除能力失败，能力ID：{}", id, e);
            return buildResult(500, "删除失败：" + e.getMessage(), null);
        }
    }

    /**
     * 批量逻辑删除能力
     */
    @PostMapping("/batchdelete")
    public Map<String, Object> batchDelete(@RequestBody List<Long> ids) {
        try {
            List<StudentAbility> deletedAbilities = studentAbilityService.batchDelete(ids);
            return buildResult(200, "批量删除成功", deletedAbilities);
        } catch (IllegalArgumentException e) {
            return buildResult(400, "参数错误：" + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("【能力接口】批量删除失败，ID列表：{}", ids, e);
            return buildResult(500, "批量删除失败：" + e.getMessage(), null);
        }
    }

    /**
     * 批量查询能力
     */
    @PostMapping("/batchselect")
    public Map<String, Object> batchSelect(@RequestBody List<Long> ids) {
        try {
            List<StudentAbility> abilities = studentAbilityService.batchSelect(ids);
            return buildResult(200, "批量查询成功", abilities);
        } catch (IllegalArgumentException e) {
            return buildResult(400, "参数错误：" + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("【能力接口】批量查询失败，ID列表：{}", ids, e);
            return buildResult(500, "批量查询失败：" + e.getMessage(), null);
        }
    }

    /**
     * 根据用户ID查询能力
     */
    @GetMapping("/user/{userId}")
    public Map<String, Object> selectByUserId(@PathVariable Long userId) {
        try {
            List<StudentAbility> abilities = studentAbilityService.selectByUserId(userId);
            return buildResult(200, "查询成功", abilities);
        } catch (IllegalArgumentException e) {
            return buildResult(400, "参数错误：" + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("【能力接口】根据用户ID查询失败，用户ID：{}", userId, e);
            return buildResult(500, "查询失败：" + e.getMessage(), null);
        }
    }
}