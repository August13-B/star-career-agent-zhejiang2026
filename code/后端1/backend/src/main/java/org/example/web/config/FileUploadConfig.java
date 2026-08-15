package org.example.web.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 文件上传配置类
 * 应用启动时自动创建必要的文件夹
 */
@Component
public class FileUploadConfig {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadConfig.class);

    /**
     * 上传目录绝对路径
     */
    private String uploadDirAbsolutePath;

    /**
     * 应用启动时自动创建上传目录
     */
    @PostConstruct
    public void init() {
        // 使用项目根目录下的 uploads/images 文件夹存储图片
        String projectRoot = System.getProperty("user.dir");
        uploadDirAbsolutePath = projectRoot + File.separator + "uploads" + File.separator + "images";
        createDirectory(uploadDirAbsolutePath);
    }

    /**
     * 创建目录（如果不存在）
     * @param dirPath 目录路径
     */
    private void createDirectory(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                logger.info("【文件上传配置】成功创建目录：{}", dir.getAbsolutePath());
            } else {
                logger.warn("【文件上传配置】创建目录失败：{}", dir.getAbsolutePath());
            }
        } else {
            logger.info("【文件上传配置】目录已存在：{}", dir.getAbsolutePath());
        }
    }

    /**
     * 获取上传目录绝对路径（确保末尾有文件分隔符）
     */
    public String getUploadDir() {
        if (uploadDirAbsolutePath != null && !uploadDirAbsolutePath.endsWith(File.separator)) {
            return uploadDirAbsolutePath + File.separator;
        }
        return uploadDirAbsolutePath;
    }
}