package org.example.web.controller;

import lombok.RequiredArgsConstructor;
import org.example.web.entity.StudentProfile;
import org.example.web.service.StudentProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/student") // 统一接口前缀，方便网关/拦截器配置
@RequiredArgsConstructor
public class StudentProfileController {

    private static final Logger logger = LoggerFactory.getLogger(StudentProfileController.class);
    private final StudentProfileService studentProfileService;



    // 统一响应结果工具方法
    private Map<String, Object> buildResult(int code, String message, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", data);
        return result;
    }

//    @PostMapping("/save")
//    public String saveProfile(@RequestBody StudentProfile profile) {
//        studentProfileService.encryptAndSave(profile);
//        return "加密存储成功";
//    }
//
//    @GetMapping("/list")
//    public List<StudentProfile> getProfileList() {
//        return studentProfileService.decryptAndGetList();
//    }

    /**
     * 查询所有学生信息
     */
    @GetMapping("/all")
    public Map<String, Object> selectAll() {
        try {
            List<StudentProfile> students = studentProfileService.selectAll();
            return buildResult(200, "查询成功", students);
        } catch (Exception e) {
            logger.error("【学生接口】查询所有学生失败", e);
            return buildResult(500, "查询失败：" + e.getMessage(), null);
        }
    }

    /**
     * 多条件模糊查询
     */
    @PostMapping("/condition")
    public Map<String, Object> selectByCondition(@RequestBody StudentProfile student) {
        try {
            List<StudentProfile> students = studentProfileService.selectByCondition(student);
            return buildResult(200, "查询成功", students);
        } catch (Exception e) {
            logger.error("【学生接口】条件查询失败，查询条件：{}", student, e);
            return buildResult(500, "查询失败：" + e.getMessage(), null);
        }
    }

    /**
     * 新增学生信息
     */
    @PostMapping("insert")
    public Map<String, Object> insert(@RequestBody StudentProfile student) {
        try {
            List<StudentProfile> newStudent = studentProfileService.insert(student);
            return buildResult(200, "新增成功", newStudent);
        } catch (Exception e) {
            logger.error("【学生接口】新增学生失败，提交数据：{}", student, e);
            return buildResult(500, "新增失败：" + e.getMessage(), null);
        }
    }

    /**
     * 更新学生信息
     */
    @PutMapping("update")
    public Map<String, Object> update(@RequestBody StudentProfile student) {
        try {
            List<StudentProfile> updatedStudent = studentProfileService.update(student);
            return buildResult(200, "更新成功", updatedStudent);
        } catch (IllegalArgumentException e) {
            return buildResult(400, "参数错误：" + e.getMessage(), null);
        } catch (RuntimeException e) {
            if ("更新后与原先相同".equals(e.getMessage())) {
                return buildResult(200, "更新后与原先相同", null);
            }
            logger.error("【学生接口】更新学生失败，提交数据：{}", student, e);
            return buildResult(500, "更新失败：" + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("【学生接口】更新学生失败，提交数据：{}", student, e);
            return buildResult(500, "更新失败：" + e.getMessage(), null);
        }
    }

    /**
     * 根据ID查询学生信息
     */
    @GetMapping("/{id}")
    public Map<String, Object> selectById(@PathVariable Long id) {
        try {
            if (id == null || id <= 0) {
                throw new IllegalArgumentException("学生ID不能为空且必须大于0");
            }
            List<StudentProfile> student = studentProfileService.selectById(id);
            return buildResult(200, "查询成功", student);
        } catch (IllegalArgumentException e) {
            return buildResult(400, "参数错误：" + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("【学生接口】根据ID查询学生失败，学生ID：{}", id, e);
            return buildResult(500, "查询失败：" + e.getMessage(), null);
        }
    }

    /**
     * 根据ID逻辑删除学生
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteById(@PathVariable Long id) {
        try {
            List<StudentProfile> deletedStudent = studentProfileService.deleteById(id);
            return buildResult(200, "删除成功", deletedStudent);
        } catch (IllegalArgumentException e) {
            return buildResult(400, "参数错误：" + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("【学生接口】删除学生失败，学生ID：{}", id, e);
            return buildResult(500, "删除失败：" + e.getMessage(), null);
        }
    }

    /**
     * 批量逻辑删除学生
     */
    @PostMapping("/batchdelete")
    public Map<String, Object> batchDelete(@RequestBody List<Long> ids) {
        try {
            List<StudentProfile> deletedStudents = studentProfileService.batchDelete(ids);
            return buildResult(200, "批量删除成功", deletedStudents);
        } catch (IllegalArgumentException e) {
            return buildResult(400, "参数错误：" + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("【学生接口】批量删除失败，ID列表：{}", ids, e);
            return buildResult(500, "批量删除失败：" + e.getMessage(), null);
        }
    }


    /**
     * 优先级条件查询（年级 > 学院 > 姓名）
     */
    @PostMapping("/choose")
    public Map<String, Object> chooseSelect(@RequestBody StudentProfile student) {
        try {
            List<StudentProfile> students = studentProfileService.chooseSelect(student);
            return buildResult(200, "查询成功", students);
        } catch (Exception e) {
            logger.error("【学生接口】优先级查询失败，查询条件：{}", student, e);
            return buildResult(500, "查询失败：" + e.getMessage(), null);
        }
    }
}