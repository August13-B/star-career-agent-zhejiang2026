package wwy.example.springboot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import wwy.example.springboot.common.Result;
import wwy.example.springboot.entity.JobInfo;
import wwy.example.springboot.service.JobInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/job-info")
@RequiredArgsConstructor
public class JobInfoController {

    private final JobInfoService jobInfoService;

    @PostMapping
    public Result<JobInfo> create(@RequestBody JobInfo jobInfo) {
        boolean success = jobInfoService.add(jobInfo);
        if (success) {
            return Result.success(jobInfo);
        } else {
            return Result.error("新增失败");
        }
    }

    @GetMapping("/{id}")
    public Result<JobInfo> getById(@PathVariable Long id) {
        JobInfo jobInfo = jobInfoService.findById(id);
        if (jobInfo != null) {
            return Result.success(jobInfo);
        } else {
            return Result.notFound("岗位信息不存在");
        }
    }

    @GetMapping("/list")
    public Result<List<JobInfo>> list() {
        List<JobInfo> list = jobInfoService.findAll();
        return Result.success(list);
    }

    @GetMapping("/search")
    public Result<List<JobInfo>> search(@RequestParam(required = false) String jobName) {
        List<JobInfo> list = jobInfoService.findByJobName(jobName);
        return Result.success(list);
    }

    @GetMapping("/page")
    public Result<IPage<JobInfo>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String jobName) {
        IPage<JobInfo> page = jobInfoService.pageQuery(current, size, jobName);
        return Result.success(page);
    }

    @PutMapping("/{id}")
    public Result<JobInfo> update(@PathVariable Long id, @RequestBody JobInfo jobInfo) {
        jobInfo.setId(id);
        boolean success = jobInfoService.update(jobInfo);
        if (success) {
            return Result.success(jobInfoService.findById(id));
        } else {
            return Result.notFound("岗位信息不存在，更新失败");
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = jobInfoService.deleteById(id);
        if (success) {
            return Result.success(null);
        } else {
            return Result.notFound("岗位信息不存在，删除失败");
        }
    }
}