package org.example.web.service;

import java.io.File;

public interface ResumeExportService {

    /**
     * 根据用户ID导出简历为Word文件
     * @param userId 用户ID
     * @return 生成的Word文件
     */
    File exportResume(Long userId);

    /**
     * 获取简历存储目录
     * @return 简历存储目录路径
     */
    String getResumeDir();
}