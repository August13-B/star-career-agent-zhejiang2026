package org.example.web.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.web.entity.StudentImage;
import org.example.web.mapper.StudentImageMapper;
import org.example.web.service.StudentImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentImageServiceImpl implements StudentImageService {

    private static final Logger logger = LoggerFactory.getLogger(StudentImageServiceImpl.class);

    private final StudentImageMapper studentImageMapper;
    private final org.example.web.config.FileUploadConfig fileUploadConfig;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentImage uploadImage(Long userId, String imageType, MultipartFile file) {
        var validated = org.example.web.security.ImageUploadValidator.validate(file);

        // 获取上传目录绝对路径（配置类已确保目录存在，且末尾有文件分隔符）
        String uploadDir = fileUploadConfig.getUploadDir();
        logger.info("【图片服务】上传目录：{}", uploadDir);

        // 生成唯一文件名
        String extension = validated.extension();
        String newFilename = UUID.randomUUID().toString() + extension;
        
        // 使用绝对路径保存文件
        String filePath = uploadDir + newFilename;
        logger.info("【图片服务】文件保存路径：{}", filePath);

        try {
            // 保存文件到本地
            java.nio.file.Files.write(java.nio.file.Path.of(filePath), validated.bytes());

            // 创建图片记录
            StudentImage image = new StudentImage();
            image.setId(org.example.web.tool.SnowIdCreater.generateId(7)); // 类别7=student_image
            image.setUserId(userId);
            image.setFileName(newFilename);
            image.setFilePath(filePath);
            image.setImageType(imageType != null ? imageType : "other");
            image.setCreateTime(LocalDateTime.now());
            image.setIsDeleted(0);

            // 保存到数据库
            studentImageMapper.insert(image);
            logger.info("【图片服务】图片上传成功，用户ID：{}，文件路径：{}", userId, filePath);
            return image;

        } catch (IOException | RuntimeException e) {
            // Remove only this upload if its database insert or file write failed.
            try { java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(filePath)); }
            catch (IOException cleanupError) { e.addSuppressed(cleanupError); }
            logger.error("【图片服务】图片保存失败", e);
            throw new RuntimeException("图片保存失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteImage(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("图片ID不能为空且必须大于0");
        }

        // 查询图片记录
        StudentImage image = studentImageMapper.selectById(id);
        if (image == null || image.getIsDeleted() == 1) {
            throw new RuntimeException("图片不存在或已被删除");
        }

        // 删除本地文件
        File file = new File(image.getFilePath());
        if (file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                logger.warn("【图片服务】本地文件删除失败：{}", image.getFilePath());
            }
        }

        // 逻辑删除数据库记录
        studentImageMapper.deleteById(id);
        logger.info("【图片服务】图片删除成功，图片ID：{}", id);
    }

    @Override
    public List<StudentImage> getImagesByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID不能为空且必须大于0");
        }
        return studentImageMapper.selectByUserId(userId);
    }

    @Override
    public StudentImage getImageById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("图片ID不能为空且必须大于0");
        }
        return studentImageMapper.selectById(id);
    }
}
