package org.example.web.service;



import org.example.web.entity.StudentAbilityScore;
import org.example.web.entity.StudentAbilityScore;

import java.util.List;

public interface StudentAbilityScoreService {
    // 查询所有评分
    List<StudentAbilityScore> selectAll();

    // 多条件查询
    List<StudentAbilityScore> selectByCondition(StudentAbilityScore score);

    // 新增评分
    int insert(StudentAbilityScore score);

    // 更新评分
    int update(StudentAbilityScore score);

    // 根据ID删除（逻辑删除）
    int deleteById(Long id);

    // 批量删除（逻辑删除）
    int batchDelete(List<Long> ids);

    // 批量查询
    List<StudentAbilityScore> batchSelect(List<Long> ids);

    // 根据用户ID查询
    List<StudentAbilityScore> selectByUserId(Long userId);

    // 根据能力ID查询
    List<StudentAbilityScore> selectByAbilityId(Long abilityId);

    // 根据ID查询
    List<StudentAbilityScore> selectById(Long id);
}
