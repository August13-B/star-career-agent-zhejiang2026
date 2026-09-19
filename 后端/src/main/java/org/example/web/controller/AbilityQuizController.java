package org.example.web.controller;

import java.util.Map;

import org.example.web.entity.Result;
import org.example.web.service.AbilityQuizService;
import org.example.web.tool.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 六维能力初步测评接口（注册后 / 个人中心）
 *
 * <p>题库在后端（不入库），只下发给前端题干与打乱后的选项（不含分值），
 * 提交后由后端评分并写 `student_ability` + `student_ability_score`。
 */
@RestController
@RequestMapping("/ability/quiz")
public class AbilityQuizController {

    @Autowired
    private AbilityQuizService abilityQuizService;

    /** 抽题：每维随机 1~2 道（约 10 题） */
    @GetMapping
    @CrossOrigin
    public Result<?> draw() {
        return Result.success("抽题成功", abilityQuizService.drawQuiz());
    }

    /** 提交作答：评分并落库（10 维分 + total + 评语） */
    @PostMapping("/submit")
    @CrossOrigin
    public Result<?> submit(@RequestBody Map<String, Object> body,
                            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = currentUserId(token);
        if (userId == null) {
            return Result.error("登录状态无效，请重新登录后再提交");
        }
        return Result.success("测评完成", abilityQuizService.submit(userId, body));
    }

    private Long currentUserId(String token) {
        try {
            Map<String, Object> claims = JwtUtil.parseToken(token);
            return Long.parseLong(String.valueOf(claims.get("id")));
        } catch (Exception e) {
            return null;
        }
    }
}
