package wwy.example.springboot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import wwy.example.springboot.common.Result;
import wwy.example.springboot.entity.JobSkillRequirement;
import wwy.example.springboot.service.JobSkillRequirementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/job-skill-requirement")
@RequiredArgsConstructor
public class JobSkillRequirementController {

    private final JobSkillRequirementService jobSkillRequirementService;

    @PostMapping
    public Result<JobSkillRequirement> create(@RequestBody JobSkillRequirement requirement) {
        boolean success = jobSkillRequirementService.add(requirement);
        if (success) {
            return Result.success(requirement);
        } else {
            return Result.error("新增失败");
        }
    }

    @GetMapping("/{id}")
    public Result<JobSkillRequirement> getById(@PathVariable Long id) {
        JobSkillRequirement requirement = jobSkillRequirementService.findById(id);
        if (requirement != null) {
            return Result.success(requirement);
        } else {
            return Result.notFound("技能需求不存在");
        }
    }

    @GetMapping("/by-job/{jobId}")
    public Result<JobSkillRequirement> getByJobId(@PathVariable Long jobId) {
        JobSkillRequirement requirement = jobSkillRequirementService.findByJobId(jobId);
        if (requirement != null) {
            return Result.success(requirement);
        } else {
            return Result.notFound("该岗位的技能需求不存在");
        }
    }

    @GetMapping("/page")
    public Result<IPage<JobSkillRequirement>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long jobId) {
        IPage<JobSkillRequirement> page = jobSkillRequirementService.pageQuery(current, size, jobId);
        return Result.success(page);
    }

    @PutMapping("/{id}")
    public Result<JobSkillRequirement> update(@PathVariable Long id, @RequestBody JobSkillRequirement requirement) {
        requirement.setId(id);
        boolean success = jobSkillRequirementService.update(requirement);
        if (success) {
            return Result.success(jobSkillRequirementService.findById(id));
        } else {
            return Result.notFound("技能需求不存在，更新失败");
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = jobSkillRequirementService.deleteById(id);
        if (success) {
            return Result.success(null);
        } else {
            return Result.notFound("技能需求不存在，删除失败");
        }
    }

    @DeleteMapping("/by-job/{jobId}")
    public Result<Void> deleteByJobId(@PathVariable Long jobId) {
        boolean success = jobSkillRequirementService.deleteByJobId(jobId);
        if (success) {
            return Result.success(null);
        } else {
            return Result.notFound("该岗位的技能需求不存在，删除失败");
        }
    }
}