package wwy.example.springboot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import wwy.example.springboot.common.Result;
import wwy.example.springboot.entity.JobPromotionGraph;
import wwy.example.springboot.service.JobPromotionGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/job-promotion-graph")
@RequiredArgsConstructor
public class JobPromotionGraphController {

    private final JobPromotionGraphService jobPromotionGraphService;

    @PostMapping
    public Result<JobPromotionGraph> create(@RequestBody JobPromotionGraph graph) {
        boolean success = jobPromotionGraphService.add(graph);
        if (success) {
            return Result.success(graph);
        } else {
            return Result.error("新增失败");
        }
    }

    @GetMapping("/{id}")
    public Result<JobPromotionGraph> getById(@PathVariable Long id) {
        JobPromotionGraph graph = jobPromotionGraphService.findById(id);
        if (graph != null) {
            return Result.success(graph);
        } else {
            return Result.notFound("晋升图谱不存在");
        }
    }

    @GetMapping("/by-main-job/{mainJobId}")
    public Result<List<JobPromotionGraph>> getByMainJobId(@PathVariable Long mainJobId) {
        List<JobPromotionGraph> list = jobPromotionGraphService.findByMainJobId(mainJobId);
        return Result.success(list);
    }

    @GetMapping("/page")
    public Result<IPage<JobPromotionGraph>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long mainJobId) {
        IPage<JobPromotionGraph> page = jobPromotionGraphService.pageQuery(current, size, mainJobId);
        return Result.success(page);
    }

    @PutMapping("/{id}")
    public Result<JobPromotionGraph> update(@PathVariable Long id, @RequestBody JobPromotionGraph graph) {
        graph.setId(id);
        boolean success = jobPromotionGraphService.update(graph);
        if (success) {
            return Result.success(jobPromotionGraphService.findById(id));
        } else {
            return Result.notFound("晋升图谱不存在，更新失败");
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = jobPromotionGraphService.deleteById(id);
        if (success) {
            return Result.success(null);
        } else {
            return Result.notFound("晋升图谱不存在，删除失败");
        }
    }
}