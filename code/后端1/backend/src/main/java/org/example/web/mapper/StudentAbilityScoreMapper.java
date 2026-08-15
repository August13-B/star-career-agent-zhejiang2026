package org.example.web.mapper;

import java.util.List;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.web.entity.StudentAbilityScore;

@Mapper
public interface StudentAbilityScoreMapper {
    // 全部查询
    List<StudentAbilityScore> selectAll();

    // 条件查询
    List<StudentAbilityScore> selectByCondition(StudentAbilityScore score);

    // 新增评分
    int insert(StudentAbilityScore score);

    // 更新评分
    int update(StudentAbilityScore score);

    // 根据ID删除（逻辑删除）
    int deleteById(Long id);

    // 批量删除（逻辑删除）
    int batchDelete(@Param("ids") List<Long> ids);

    // 批量查询
    List<StudentAbilityScore> batchSelect(@Param("ids") List<Long> ids);

    // 根据用户ID查询
    List<StudentAbilityScore> selectByUserId(Long userId);

    // 根据能力ID查询
    List<StudentAbilityScore> selectByAbilityId(Long abilityId);

    // 根据ID查询
    List<StudentAbilityScore> selectById(Long id);
}
