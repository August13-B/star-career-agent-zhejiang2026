package org.example.web.service;

import org.example.web.entity.StudentImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StudentImageService {
    /**
     * 上传图片
     */
    StudentImage uploadImage(Long userId, String imageType, MultipartFile file);

    /**
     * 删除图片
     */
    void deleteImage(Long id);

    /**
     * 根据用户ID查询图片列表
     */
    List<StudentImage> getImagesByUserId(Long userId);

    /**
     * 根据ID查询图片
     */
    StudentImage getImageById(Long id);
}