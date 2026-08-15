package wwy.example.springboot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import wwy.example.springboot.common.Result;
import wwy.example.springboot.entity.JobSoftRequirement;
import wwy.example.springboot.service.JobSoftRequirementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/job-soft-requirement")
@RequiredArgsConstructor
public class JobSoftRequirementController {

    private final JobSoftRequirementService jobSoftRequirementService;

    @PostMapping
    public Result<JobSoftRequirement> create(@RequestBody JobSoftRequirement requirement) {
        boolean success = jobSoftRequirementService.add(requirement);
        if (success) {
            return Result.success(requirement);
        } else {
            return Result.error("新增失败");
        }
    }

    @GetMapping("/{id}")
    public Result<JobSoftRequirement> getById(@PathVariable Long id) {
        JobSoftRequirement requirement = jobSoftRequirementService.findById(id);
        if (requirement != null) {
            return Result.success(requirement);
        } else {
            return Result.notFound("软实力需求不存在");
        }
    }

    @GetMapping("/by-job/{jobId}")
    public Result<JobSoftRequirement> getByJobId(@PathVariable Long jobId) {
        JobSoftRequirement requirement = jobSoftRequirementService.findByJobId(jobId);
        if (requirement != null) {
            return Result.success(requirement);
        } else {
            return Result.notFound("该岗位的软实力需求不存在");
        }
    }

    @GetMapping("/page")
    public Result<IPage<JobSoftRequirement>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long jobId) {
        IPage<JobSoftRequirement> page = jobSoftRequirementService.pageQuery(current, size, jobId);
        return Result.success(page);
    }

    @PutMapping("/{id}")
    public Result<JobSoftRequirement> update(@PathVariable Long id, @RequestBody JobSoftRequirement requirement) {
        requirement.setId(id);
        boolean success = jobSoftRequirementService.update(requirement);
        if (success) {
            return Result.success(jobSoftRequirementService.findById(id));
        } else {
            return Result.notFound("软实力需求不存在，更新失败");
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = jobSoftRequirementService.deleteById(id);
        if (success) {
            return Result.success(null);
        } else {
            return Result.notFound("软实力需求不存在，删除失败");
        }
    }

    @DeleteMapping("/by-job/{jobId}")
    public Result<Void> deleteByJobId(@PathVariable Long jobId) {
        boolean success = jobSoftRequirementService.deleteByJobId(jobId);
        if (success) {
            return Result.success(null);
        } else {
            return Result.notFound("该岗位的软实力需求不存在，删除失败");
        }
    }
}
