package org.example.web.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.web.entity.StudentAbility;

import java.util.List;

@Mapper
public interface StudentAbilityMapper {
    // 全部查询
    List<StudentAbility> selectAll();

    // 条件查询
    List<StudentAbility> selectByCondition(StudentAbility ability);

    // 新增能力
    List<StudentAbility> insert(StudentAbility ability);

    // 更新能力
    List<StudentAbility> update(StudentAbility ability);

    // 根据ID删除（逻辑删除）
    List<StudentAbility> deleteById(Long id);

    // 批量删除（逻辑删除）
    List<StudentAbility> batchDelete(@Param("ids") List<Long> ids);

    // 批量查询
    List<StudentAbility> batchSelect(@Param("ids") List<Long> ids);

    // 根据用户ID查询
    List<StudentAbility> selectByUserId(Long userId);

    // 根据ID查询
    List<StudentAbility> selectById(Long id);
}