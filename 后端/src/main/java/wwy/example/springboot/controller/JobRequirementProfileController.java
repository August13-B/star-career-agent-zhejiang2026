package wwy.example.springboot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import wwy.example.springboot.common.Result;
import wwy.example.springboot.entity.JobRequirementProfile;
import wwy.example.springboot.service.JobRequirementProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/job-requirement-profile")
@RequiredArgsConstructor
public class JobRequirementProfileController {

    private final JobRequirementProfileService jobRequirementProfileService;

    @PostMapping
    public Result<JobRequirementProfile> create(@RequestBody JobRequirementProfile profile) {
        boolean success = jobRequirementProfileService.add(profile);
        if (success) {
            return Result.success(profile);
        } else {
            return Result.error("新增失败");
        }
    }

    @GetMapping("/{id}")
    public Result<JobRequirementProfile> getById(@PathVariable Long id) {
        JobRequirementProfile profile = jobRequirementProfileService.findById(id);
        if (profile != null) {
            return Result.success(profile);
        } else {
            return Result.notFound("岗位需求不存在");
        }
    }

    @GetMapping("/list")
    public Result<List<JobRequirementProfile>> list() {
        List<JobRequirementProfile> list = jobRequirementProfileService.findAll();
        return Result.success(list);
    }

    @GetMapping("/search")
    public Result<List<JobRequirementProfile>> search(@RequestParam(required = false) String positionName) {
        List<JobRequirementProfile> list = jobRequirementProfileService.findByPositionName(positionName);
        return Result.success(list);
    }

    @GetMapping("/by-category/{category}")
    public Result<List<JobRequirementProfile>> getByCategory(@PathVariable String category) {
        List<JobRequirementProfile> list = jobRequirementProfileService.findByCategory(category);
        return Result.success(list);
    }

    @GetMapping("/by-level/{level}")
    public Result<List<JobRequirementProfile>> getByLevel(@PathVariable Integer level) {
        List<JobRequirementProfile> list = jobRequirementProfileService.findByLevel(level);
        return Result.success(list);
    }

    @GetMapping("/page")
    public Result<IPage<JobRequirementProfile>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String positionName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer level) {
        IPage<JobRequirementProfile> page = jobRequirementProfileService.pageQuery(current, size, positionName, category, level);
        return Result.success(page);
    }

    @PutMapping("/{id}")
    public Result<JobRequirementProfile> update(@PathVariable Long id, @RequestBody JobRequirementProfile profile) {
        profile.setId(id);
        boolean success = jobRequirementProfileService.update(profile);
        if (success) {
            return Result.success(jobRequirementProfileService.findById(id));
        } else {
            return Result.notFound("岗位需求不存在，更新失败");
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = jobRequirementProfileService.deleteById(id);
        if (success) {
            return Result.success(null);
        } else {
            return Result.notFound("岗位需求不存在，删除失败");
        }
    }
}