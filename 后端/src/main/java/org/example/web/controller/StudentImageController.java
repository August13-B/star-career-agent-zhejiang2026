package org.example.web.controller;

import lombok.RequiredArgsConstructor;
import org.example.web.entity.StudentImage;
import org.example.web.service.StudentImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/student/image")
@RequiredArgsConstructor
public class StudentImageController {

    private static final Logger logger = LoggerFactory.getLogger(StudentImageController.class);

    private final StudentImageService studentImageService;

    /**
     * 上传图片接口
     * @param userId 用户ID
     * @param imageType 图片类型（可选，如：avatar, certificate）
     * @param file 图片文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadImage(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "imageType", required = false) String imageType,
            @RequestParam("file") MultipartFile file) {
        try {
            StudentImage image = studentImageService.uploadImage(userId, imageType, file);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "上传成功");
            result.put("data", image);
            return result;
        } catch (IllegalArgumentException e) {
            logger.warn("【图片接口】上传参数错误：{}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", e.getMessage());
            result.put("data", null);
            return result;
        } catch (Exception e) {
            logger.error("【图片接口】上传失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "上传失败：" + e.getMessage());
            result.put("data", null);
            return result;
        }
    }

    /**
     * 删除图片接口
     * @param id 图片ID
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteImage(@PathVariable("id") Long id) {
        try {
            studentImageService.deleteImage(id);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "删除成功");
            result.put("data", null);
            return result;
        } catch (IllegalArgumentException e) {
            logger.warn("【图片接口】删除参数错误：{}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", e.getMessage());
            result.put("data", null);
            return result;
        } catch (RuntimeException e) {
            logger.warn("【图片接口】删除失败：{}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("code", 404);
            result.put("message", e.getMessage());
            result.put("data", null);
            return result;
        } catch (Exception e) {
            logger.error("【图片接口】删除失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "删除失败：" + e.getMessage());
            result.put("data", null);
            return result;
        }
    }

    /**
     * 查询用户图片列表接口
     * @param userId 用户ID
     * @return 图片列表
     */
    @GetMapping("/list/{userId}")
    public Map<String, Object> getImagesByUserId(@PathVariable("userId") Long userId) {
        try {
            List<StudentImage> images = studentImageService.getImagesByUserId(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", images);
            return result;
        } catch (IllegalArgumentException e) {
            logger.warn("【图片接口】查询参数错误：{}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", e.getMessage());
            result.put("data", null);
            return result;
        } catch (Exception e) {
            logger.error("【图片接口】查询失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
            result.put("data", null);
            return result;
        }
    }
}