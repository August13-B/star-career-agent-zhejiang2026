package org.example.web.service;

import org.example.web.entity.StudentProfile;
import java.util.List;

public interface StudentProfileService {
    // 查询所有学生
    List<StudentProfile> selectAll();

    // 多条件查询
    List<StudentProfile> selectByCondition(StudentProfile student);

    // 新增学生
    List<StudentProfile> insert(StudentProfile student);

    // 更新学生
    List<StudentProfile> update(StudentProfile student);

    // 根据ID删除（逻辑删除）
    List<StudentProfile> deleteById(Long studentId);

    // 批量删除（逻辑删除）
    List<StudentProfile> batchDelete(List<Long> ids);

    // 批量查询
    List<StudentProfile> batchSelect(List<Long> ids);

    // 根据ID查询
    List<StudentProfile> selectById(Long id);

    // 根据用户ID查询
    List<StudentProfile> selectByUserId(Long userId);

    List<StudentProfile> encryptAndSave(StudentProfile student);

    List<StudentProfile> decryptAndGetById(Long id);

    List<StudentProfile> decryptAndGetList();

    // 优先级条件查询
    List<StudentProfile> chooseSelect(StudentProfile student);
}