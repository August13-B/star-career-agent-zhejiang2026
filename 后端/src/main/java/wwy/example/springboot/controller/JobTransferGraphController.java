package wwy.example.springboot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import wwy.example.springboot.common.Result;
import wwy.example.springboot.entity.JobTransferGraph;
import wwy.example.springboot.service.JobTransferGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/job-transfer-graph")
@RequiredArgsConstructor
public class JobTransferGraphController {

    private final JobTransferGraphService jobTransferGraphService;

    @PostMapping
    public Result<JobTransferGraph> create(@RequestBody JobTransferGraph graph) {
        boolean success = jobTransferGraphService.add(graph);
        if (success) {
            return Result.success(graph);
        } else {
            return Result.error("新增失败");
        }
    }

    @GetMapping("/{id}")
    public Result<JobTransferGraph> getById(@PathVariable Long id) {
        JobTransferGraph graph = jobTransferGraphService.findById(id);
        if (graph != null) {
            return Result.success(graph);
        } else {
            return Result.notFound("换岗图谱不存在");
        }
    }

    @GetMapping("/by-main-job/{mainJobId}")
    public Result<List<JobTransferGraph>> getByMainJobId(@PathVariable Long mainJobId) {
        List<JobTransferGraph> list = jobTransferGraphService.findByMainJobId(mainJobId);
        return Result.success(list);
    }

    @GetMapping("/page")
    public Result<IPage<JobTransferGraph>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long mainJobId) {
        IPage<JobTransferGraph> page = jobTransferGraphService.pageQuery(current, size, mainJobId);
        return Result.success(page);
    }

    @PutMapping("/{id}")
    public Result<JobTransferGraph> update(@PathVariable Long id, @RequestBody JobTransferGraph graph) {
        graph.setId(id);
        boolean success = jobTransferGraphService.update(graph);
        if (success) {
            return Result.success(jobTransferGraphService.findById(id));
        } else {
            return Result.notFound("换岗图谱不存在，更新失败");
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = jobTransferGraphService.deleteById(id);
        if (success) {
            return Result.success(null);
        } else {
            return Result.notFound("换岗图谱不存在，删除失败");
        }
    }
}