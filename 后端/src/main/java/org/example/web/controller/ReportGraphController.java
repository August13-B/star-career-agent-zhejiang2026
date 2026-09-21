package org.example.web.controller;

import lombok.RequiredArgsConstructor;
import org.example.web.service.impl.ReportGraphService;
import org.example.web.tool.JwtUtil;
import org.springframework.web.bind.annotation.*;
import wwy.example.springboot.common.Result;

@RestController
@RequiredArgsConstructor
@RequestMapping("/report-graph")
public class ReportGraphController {
    private final ReportGraphService graphs;
    private Long user(String token) {
        try { return Long.valueOf(String.valueOf(JwtUtil.parseToken(token).get("id"))); }
        catch (Exception e) { return null; }
    }
    @GetMapping("/reports")
    public Result<?> list(@RequestHeader(value="Authorization", required=false) String token) {
        Long userId = user(token);
        if (userId == null) return Result.unauthorized("请先登录");
        return Result.success(graphs.list(userId));
    }
    @GetMapping("/{id}")
    public Result<?> get(@PathVariable Long id, @RequestHeader(value="Authorization", required=false) String token) {
        Long userId = user(token);
        if (userId == null) return Result.unauthorized("请先登录");
        try { return Result.success(graphs.get(userId, id)); }
        catch (IllegalArgumentException e) { return Result.error(e.getMessage()); }
    }
    @PostMapping("/{id}")
    public Result<?> generate(@PathVariable Long id, @RequestHeader(value="Authorization", required=false) String token) {
        Long userId = user(token);
        if (userId == null) return Result.unauthorized("请先登录");
        try { return Result.success(graphs.generate(userId, id)); }
        catch (IllegalArgumentException e) { return Result.error(e.getMessage()); }
        catch (Exception e) { return Result.error("星图生成暂时失败，请重试；已保存的测评不受影响"); }
    }
}
