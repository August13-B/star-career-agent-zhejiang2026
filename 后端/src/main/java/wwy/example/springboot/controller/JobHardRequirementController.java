package wwy.example.springboot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import wwy.example.springboot.common.Result;
import wwy.example.springboot.entity.JobHardRequirement;
import wwy.example.springboot.service.JobHardRequirementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/job-hard-requirement")
@RequiredArgsConstructor
public class JobHardRequirementController {

    private final JobHardRequirementService jobHardRequirementService;

    @PostMapping
    public Result<JobHardRequirement> create(@RequestBody JobHardRequirement requirement) {
        boolean success = jobHardRequirementService.add(requirement);
        if (success) {
            return Result.success(requirement);
        } else {
            return Result.error("新增失败");
        }
    }

    @GetMapping("/{id}")
    public Result<JobHardRequirement> getById(@PathVariable Long id) {
        JobHardRequirement requirement = jobHardRequirementService.findById(id);
        if (requirement != null) {
            return Result.success(requirement);
        } else {
            return Result.notFound("硬门槛需求不存在");
        }
    }

    @GetMapping("/by-job/{jobId}")
    public Result<JobHardRequirement> getByJobId(@PathVariable Long jobId) {
        JobHardRequirement requirement = jobHardRequirementService.findByJobId(jobId);
        if (requirement != null) {
            return Result.success(requirement);
        } else {
            return Result.notFound("该岗位的硬门槛需求不存在");
        }
    }

    @GetMapping("/page")
    public Result<IPage<JobHardRequirement>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long jobId) {
        IPage<JobHardRequirement> page = jobHardRequirementService.pageQuery(current, size, jobId);
        return Result.success(page);
    }

    @PutMapping("/{id}")
    public Result<JobHardRequirement> update(@PathVariable Long id, @RequestBody JobHardRequirement requirement) {
        requirement.setId(id);
        boolean success = jobHardRequirementService.update(requirement);
        if (success) {
            return Result.success(jobHardRequirementService.findById(id));
        } else {
            return Result.notFound("硬门槛需求不存在，更新失败");
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = jobHardRequirementService.deleteById(id);
        if (success) {
            return Result.success(null);
        } else {
            return Result.notFound("硬门槛需求不存在，删除失败");
        }
    }

    @DeleteMapping("/by-job/{jobId}")
    public Result<Void> deleteByJobId(@PathVariable Long jobId) {
        boolean success = jobHardRequirementService.deleteByJobId(jobId);
        if (success) {
            return Result.success(null);
        } else {
            return Result.notFound("该岗位的硬门槛需求不存在，删除失败");
        }
    }
}