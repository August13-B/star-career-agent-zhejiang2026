package org.example.web.controller;

import lombok.RequiredArgsConstructor;
import org.example.web.service.profile.ProfileChangeService;
import org.example.web.tool.JwtUtil;
import org.springframework.web.bind.annotation.*;
import wwy.example.springboot.common.Result;

/**
 * 画像「最近一次变更」查询。
 *
 * <p>前端在完成任一画像相关操作（简历解析 / 能力补充测评 / 职场训练 / 多智能体报告）后调用，
 * 用于弹出「画像已更新」提示；个人中心也用同一条数据展示最近更新时间与变化明细。
 */
@RestController
@RequestMapping("/profile/change")
@RequiredArgsConstructor
public class ProfileChangeController {

    private final ProfileChangeService changes;

    private Long user(String token) {
        try {
            return Long.valueOf(String.valueOf(JwtUtil.parseToken(token).get("id")));
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/latest")
    public Result<?> latest(@RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = user(token);
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        return Result.success(changes.latest(userId));
    }
}
