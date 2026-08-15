package org.example.web.service;

import org.example.web.entity.StudentAbility;
import java.util.List;

public interface StudentAbilityService {
    // 查询所有能力
    List<StudentAbility> selectAll();

    // 多条件查询
    List<StudentAbility> selectByCondition(StudentAbility ability);

    // 新增能力
    List<StudentAbility> insert(StudentAbility ability);

    // 更新能力
    List<StudentAbility> update(StudentAbility ability);

    // 根据ID删除（逻辑删除）
    List<StudentAbility> deleteById(Long id);

    // 批量删除（逻辑删除）
    List<StudentAbility> batchDelete(List<Long> ids);

    // 批量查询
    List<StudentAbility> batchSelect(List<Long> ids);

    // 根据用户ID查询
    List<StudentAbility> selectByUserId(Long userId);

    // 根据ID查询
    List<StudentAbility> selectById(Long id);
}