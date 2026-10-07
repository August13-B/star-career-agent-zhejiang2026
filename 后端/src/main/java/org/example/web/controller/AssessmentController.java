package org.example.web.controller;

import java.util.Map;
import org.example.web.entity.Result;
import org.example.web.service.assessment.AssessmentException;
import org.example.web.service.assessment.AssessmentService;
import org.example.web.tool.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 能力补充测评（`/ai-score`）：AI 出题 → 答题 → 追问 → 六维评分。
 *
 * <p>前置条件：必须已填「基本情况」（硬实力四项）；未填时 {@code POST /sessions} 返回
 * {@code 422 PROFILE_REQUIRED}，前端据此先弹出硬实力表单（{@code POST /api/ability/basic}）。
 */
@RestController
@RequestMapping("/assessment")
public class AssessmentController {
    private final AssessmentService service;

    public AssessmentController(AssessmentService service) {
        this.service = service;
    }

    public record Answer(Integer chosen, String answer, Integer expectedVersion) {
    }

    public record Draft(String content, Integer expectedVersion) {
    }

    /** 测评状态与前置校验（含硬实力回显与当前 10 维分）。 */
    @GetMapping("/state")
    public Result<?> state(@RequestHeader("Authorization") String token) {
        return Result.success("获取测评状态成功", service.state(user(token)));
    }

    /** 开始测评（同时出第 1 题）。 */
    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<?> create(@RequestHeader("Authorization") String token,
                            @RequestBody(required = false) Map<String, Object> body) {
        String requestId = body == null ? null : String.valueOf(body.get("clientRequestId"));
        if (requestId == null || requestId.isBlank() || "null".equals(requestId)) {
            throw new AssessmentException(422, "REQUEST_ID_REQUIRED", "缺少请求标识，请刷新页面后重试");
        }
        return Result.success(service.create(user(token), requestId));
    }

    @GetMapping("/sessions/{id}")
    public Result<?> snapshot(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        return Result.success(service.snapshot(user(token), id));
    }

    /** 提交当前题作答（客观题传 chosen，主观题传 answer），并自动推进到下一题或评分。 */
    @PostMapping("/sessions/{id}/turns")
    public Result<?> answer(@RequestHeader("Authorization") String token, @PathVariable Long id,
                            @RequestBody Answer body) {
        return Result.success(service.answer(user(token), id, body.chosen(),
                body.answer() == null ? null : body.answer().strip(), body.expectedVersion()));
    }

    /** 草稿自动保存（断点续答）。 */
    @PutMapping("/sessions/{id}/draft")
    public Result<?> draft(@RequestHeader("Authorization") String token, @PathVariable Long id,
                           @RequestBody Draft body) {
        service.saveDraft(user(token), id, body.content() == null ? "" : body.content(),
                body.expectedVersion() == null ? 0 : body.expectedVersion());
        return Result.success("草稿已保存", Map.of());
    }

    @GetMapping("/sessions")
    public Result<?> list(@RequestHeader("Authorization") String token,
                          @RequestParam(defaultValue = "0") int offset,
                          @RequestParam(defaultValue = "10") int limit) {
        if (offset < 0 || offset > 10000 || limit < 1 || limit > 50) {
            throw new AssessmentException(422, "PAGE_INVALID", "分页参数无效");
        }
        return Result.success(service.list(user(token), offset, limit));
    }

    @ExceptionHandler(AssessmentException.class)
    public ResponseEntity<Result<?>> assessmentError(AssessmentException error) {
        return ResponseEntity.status(error.status())
                .body(Result.error(error.getMessage(), Map.of("errorCode", error.code())));
    }

    /**
     * 兜底：测评链路里未预期的异常也要以**真实错误状态**返回，不要被全局处理器包成 HTTP 200「网络错误」——
     * 否则前端只会「点了没反应」。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> unexpected(Exception error) {
        System.err.println("测评链路未预期异常: " + error);
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error("测评服务异常：" + message, Map.of("errorCode", "ASSESSMENT_FAILED")));
    }

    private Long user(String token) {
        try {
            return Long.parseLong(String.valueOf(JwtUtil.parseToken(token).get("id")));
        } catch (Exception e) {
            throw new AssessmentException(401, "LOGIN_REQUIRED", "登录已失效，请重新登录");
        }
    }
}
