package org.example.web.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.web.entity.StudentImage;

import java.util.List;

@Mapper
public interface StudentImageMapper {
    /**
     * 新增图片记录
     */
    int insert(StudentImage image);

    /**
     * 根据ID删除（逻辑删除）
     */
    int deleteById(Long id);

    /**
     * 根据ID查询
     */
    StudentImage selectById(Long id);

    /**
     * 根据用户ID查询图片列表
     */
    List<StudentImage> selectByUserId(Long userId);

    /**
     * 根据用户ID和图片类型查询
     */
    List<StudentImage> selectByUserIdAndType(@Param("userId") Long userId, @Param("imageType") String imageType);
}