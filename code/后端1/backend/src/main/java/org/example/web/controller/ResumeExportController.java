package org.example.web.controller;

import lombok.RequiredArgsConstructor;
import org.example.web.service.ResumeExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/resume")
@RequiredArgsConstructor
public class ResumeExportController {

    private static final Logger logger = LoggerFactory.getLogger(ResumeExportController.class);

    private final ResumeExportService resumeExportService;

    /**
     * 导出简历为Word文件
     * @param userId 用户ID
     * @return Word文件下载响应
     */
    @GetMapping({"/export/{userId}", "/export/{userId}/"})
    public ResponseEntity<?> exportResume(@PathVariable Long userId) {
        try {
            File resumeFile = resumeExportService.exportResume(userId);

            if (resumeFile.exists()) {
                byte[] fileContent = readFileToBytes(resumeFile);
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentDispositionFormData("attachment", 
                    "resume_" + userId + ".docx");
                headers.setContentLength(fileContent.length);

                logger.info("【简历导出接口】成功导出简历，用户ID：{}，文件路径：{}", userId, resumeFile.getAbsolutePath());
                return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
            } else {
                logger.error("【简历导出接口】简历文件不存在，用户ID：{}", userId);
                return buildResult(500, "简历文件生成失败", null);
            }

        } catch (IllegalArgumentException e) {
            logger.warn("【简历导出接口】参数错误：{}", e.getMessage());
            return buildResult(400, "参数错误：" + e.getMessage(), null);
        } catch (RuntimeException e) {
            logger.error("【简历导出接口】导出失败，用户ID：{}", userId, e);
            return buildResult(500, "导出失败：" + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("【简历导出接口】导出异常，用户ID：{}", userId, e);
            return buildResult(500, "导出异常：" + e.getMessage(), null);
        }
    }

    /**
     * 获取简历存储目录信息
     */
    @GetMapping("/info")
    public ResponseEntity<?> getResumeInfo() {
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("resumeDir", resumeExportService.getResumeDir());
            return buildResult(200, "success", info);
        } catch (Exception e) {
            logger.error("【简历导出接口】获取信息失败", e);
            return buildResult(500, "获取信息失败：" + e.getMessage(), null);
        }
    }

    /**
     * 读取文件内容为字节数组
     */
    private byte[] readFileToBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            return bytes;
        }
    }

    /**
     * 构建统一响应结果
     */
    private ResponseEntity<Map<String, Object>> buildResult(int code, String message, Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", data);
        return ResponseEntity.ok(result);
    }
}