package org.example.web.controller;

import org.example.web.entity.JobInfo;
import org.example.web.entity.Result;
import org.example.web.service.JobProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 岗位画像控制器
 * 提供岗位画像相关的RESTful API接口
 * 注意：当前版本只实现添加岗位信息接口，其他接口已删除
 * 
 * @author 系统生成
 * @version 1.0
 */
@RestController
@RequestMapping("/job_profile")
public class JobProfileController {

    @Autowired
    private JobProfileService jobProfileService;

    /**
     * 添加岗位信息
     * 接收JobInfo对象，调用Service层方法插入数据库
     * 
     * @param jobInfo 岗位信息实体，从请求体中获取
     * @return 操作结果，包含成功或失败信息
     */
    @PostMapping("/job_info")
    @ResponseBody
    public Result addJobInfo(@RequestBody JobInfo jobInfo) {
        try {
            // 调用Service层方法添加岗位信息
            boolean success = jobProfileService.addJobInfo(jobInfo);
            if (success) {
                return Result.success("岗位信息添加成功", jobInfo);
            } else {
                return Result.error("岗位信息添加失败");
            }
        } catch (Exception e) {
            return Result.error("添加岗位信息时发生错误: " + e.getMessage());
        }
    }

    @GetMapping("/create_job_profile")
    @CrossOrigin
    public Result createJobProfile(@RequestParam String user_id) {
        try {
            Result success = jobProfileService.createJobProfile(user_id);
            return success;
        }catch (Exception e) {
            return Result.error("创建岗位画像时发生错误: " + e.getMessage());
        }
    }
}
