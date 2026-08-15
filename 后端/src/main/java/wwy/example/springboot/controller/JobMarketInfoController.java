package wwy.example.springboot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import wwy.example.springboot.common.Result;
import wwy.example.springboot.entity.JobMarketInfo;
import wwy.example.springboot.service.JobMarketInfoService;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/job-market-info")
@RequiredArgsConstructor
public class JobMarketInfoController {

    private final JobMarketInfoService jobMarketInfoService;

    @PostMapping
    public Result<JobMarketInfo> create(@RequestBody JobMarketInfo info) {
        boolean success = jobMarketInfoService.add(info);
        if (success) {
            return Result.success(info);
        } else {
            return Result.error("新增失败");
        }
    }

    @GetMapping("/{id}")
    public Result<JobMarketInfo> getById(@PathVariable Long id) {
        JobMarketInfo info = jobMarketInfoService.findById(id);
        if (info != null) {
            return Result.success(info);
        } else {
            return Result.notFound("市场信息不存在");
        }
    }

    @GetMapping("/by-job/{jobId}")
    public Result<JobMarketInfo> getByJobId(@PathVariable Long jobId) {
        JobMarketInfo info = jobMarketInfoService.findByJobId(jobId);
        if (info != null) {
            return Result.success(info);
        } else {
            return Result.notFound("该岗位的市场信息不存在");
        }
    }

    @GetMapping("/page")
    public Result<IPage<JobMarketInfo>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) String industry) {
        IPage<JobMarketInfo> page = jobMarketInfoService.pageQuery(current, size, jobId, industry);
        return Result.success(page);
    }

    @PutMapping("/{id}")
    public Result<JobMarketInfo> update(@PathVariable Long id, @RequestBody JobMarketInfo info) {
        info.setId(id);
        boolean success = jobMarketInfoService.update(info);
        if (success) {
            return Result.success(jobMarketInfoService.findById(id));
        } else {
            return Result.notFound("市场信息不存在，更新失败");
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = jobMarketInfoService.deleteById(id);
        if (success) {
            return Result.success(null);
        } else {
            return Result.notFound("市场信息不存在，删除失败");
        }
    }

    @DeleteMapping("/by-job/{jobId}")
    public Result<Void> deleteByJobId(@PathVariable Long jobId) {
        boolean success = jobMarketInfoService.deleteByJobId(jobId);
        if (success) {
            return Result.success(null);
        } else {
            return Result.notFound("该岗位的市场信息不存在，删除失败");
        }
    }
}