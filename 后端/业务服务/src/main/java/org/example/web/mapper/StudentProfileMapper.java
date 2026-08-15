package org.example.web.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.web.entity.StudentProfile;

import java.util.List;

@Mapper
public interface StudentProfileMapper {
    // 全部查询
    List<StudentProfile> selectAll();

    // 条件查询
    List<StudentProfile> selectByCondition(StudentProfile st);

    // 新增学生（先插入，再返回新增记录）
    List<StudentProfile> insert(StudentProfile student);

    // 更新学生（先查原记录 → 更新 → 返回更新后记录）
    List<StudentProfile> update(StudentProfile student);

    // 根据ID删除（先查要删除的记录 → 删除 → 返回删除前记录）
    List<StudentProfile> deleteById(Long studentId);

    // 批量删除（先查所有要删除的记录 → 批量删除 → 返回删除前记录集合）
    List<StudentProfile> batchDelete(@Param("ids") List<Long> ids);

    // 批量查询
    List<StudentProfile> batchSelect(@Param("ids") List<Long> ids);

    // 条件选择查询
    List<StudentProfile> chooseSelect(StudentProfile st);

    // 根据ID查询
    List<StudentProfile> selectById(Long id);
    
    // 根据用户ID查询
    List<StudentProfile> selectByUserId(Long userId);
}