package org.example.web.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.example.web.entity.Result;
import org.example.web.service.training.TrainingException;
import org.example.web.service.training.TrainingService;
import org.example.web.service.training.TrainingOutcomeService;
import com.fasterxml.jackson.databind.JsonNode;
import org.example.web.tool.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/training")
public class TrainingController {
    private final TrainingService service;
    private final TrainingOutcomeService outcomes;
    public TrainingController(TrainingService service, TrainingOutcomeService outcomes) { this.service = service; this.outcomes = outcomes; }

    public record Create(@NotBlank @Size(max=80) String templateId,
                         @Pattern(regexp="entry|standard") String difficulty, Boolean useForProfile,
                         @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{8,80}") String clientRequestId) {}
    public record Answer(@NotBlank @Size(max=4000) String content, Boolean skip,
                         @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{8,80}") String clientRequestId,
                         @NotNull @PositiveOrZero Integer expectedVersion) {}
    public record Finish(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{8,80}") String clientRequestId,
                         @NotNull @PositiveOrZero Integer expectedVersion) {}
    public record Draft(@NotNull @Size(max=4000) String content, @NotNull @PositiveOrZero Integer expectedVersion) {}
    public record Retry(@NotNull @Min(1) @Max(3) Integer expectedAttempt) {}

    @GetMapping("/templates")
    public Result<?> templates() { return Result.success(service.templates()); }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<?> create(@RequestHeader("Authorization") String token, @Valid @RequestBody Create body) {
        return Result.success(service.create(user(token), body.templateId(), body.clientRequestId(), body.difficulty() == null ? "standard" : body.difficulty(), Boolean.TRUE.equals(body.useForProfile())));
    }

    @GetMapping("/sessions")
    public Result<?> list(@RequestHeader("Authorization") String token,
                          @RequestParam(defaultValue="0") int offset, @RequestParam(defaultValue="20") int limit, @RequestParam(required=false) String templateId, @RequestParam(required=false) String status) {
        if (offset < 0 || offset > 10000 || limit < 1 || limit > 50) throw new TrainingException(422, "PAGE_INVALID", "分页参数无效");
        return Result.success(service.list(user(token), offset, limit, templateId, status));
    }

    @GetMapping("/sessions/{id}")
    public Result<?> snapshot(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        return Result.success(service.snapshot(user(token), id));
    }

    @PostMapping("/sessions/{id}/turns")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Result<?> answer(@RequestHeader("Authorization") String token, @PathVariable Long id, @Valid @RequestBody Answer body) {
        return Result.success(service.answer(user(token), id, body.content(), body.clientRequestId(), body.expectedVersion(), Boolean.TRUE.equals(body.skip())));
    }

    @PutMapping("/sessions/{id}/draft")
    public Result<?> draft(@RequestHeader("Authorization") String token, @PathVariable Long id, @Valid @RequestBody Draft body) {
        return Result.success(service.draft(user(token), id, body.content(), body.expectedVersion()));
    }

    @PostMapping("/sessions/{id}/finish")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Result<?> finish(@RequestHeader("Authorization") String token, @PathVariable Long id, @Valid @RequestBody Finish body) {
        return Result.success(service.finish(user(token), id, body.clientRequestId(), body.expectedVersion()));
    }

    @GetMapping("/sessions/{id}/evaluation")
    public Result<?> evaluation(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        return Result.success(service.snapshot(user(token), id).get("evaluation"));
    }

    @GetMapping("/runs/{id}")
    public Result<?> run(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        return Result.success(service.getRun(user(token), id));
    }

    @PostMapping("/runs/{id}/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Result<?> retry(@RequestHeader("Authorization") String token, @PathVariable Long id, @Valid @RequestBody Retry body) {
        return Result.success(service.retry(user(token), id, body.expectedAttempt()));
    }

    @PostMapping("/sessions/{id}/cancel")
    public Result<?> cancel(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        return Result.success(service.cancel(user(token), id));
    }

    public record ArtifactDraft(@NotNull JsonNode content, @NotNull @PositiveOrZero Integer expectedVersion) {}
    public record Artifact(@NotNull JsonNode content, @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{8,80}") String clientRequestId, @NotNull @Min(0) @Max(50) Integer expectedRevision) {}
    public record GrowthTask(@NotNull @Positive Long planId, @NotNull @Min(0) @Max(4) Integer suggestionIndex) {}

    @PutMapping("/sessions/{id}/artifact-draft")
    public Result<?> artifactDraft(@RequestHeader("Authorization") String token, @PathVariable Long id, @Valid @RequestBody ArtifactDraft body) {
        return Result.success(service.artifactDraft(user(token),id,body.content(),body.expectedVersion()));
    }
    @PostMapping("/sessions/{id}/artifacts")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<?> artifact(@RequestHeader("Authorization") String token, @PathVariable Long id, @Valid @RequestBody Artifact body) {
        return Result.success(service.artifact(user(token),id,body.content(),body.clientRequestId(),body.expectedRevision()));
    }
    @GetMapping("/sessions/{id}/artifacts/{revision}")
    public Result<?> artifactRevision(@RequestHeader("Authorization") String token, @PathVariable Long id, @PathVariable int revision) {
        return Result.success(service.artifactRevision(user(token),id,revision));
    }
    @PostMapping("/sessions/{id}/profile/retry")
    public Result<?> applyProfile(@RequestHeader("Authorization") String token, @PathVariable Long id) {
        outcomes.apply(user(token),id); return Result.success(service.snapshot(user(token),id).get("profileApplication"));
    }
    @GetMapping("/growth/plans")
    public Result<?> plans(@RequestHeader("Authorization") String token) { return Result.success(outcomes.plans(user(token))); }
    @PostMapping("/sessions/{id}/growth-task")
    public Result<?> growthTask(@RequestHeader("Authorization") String token, @PathVariable Long id, @Valid @RequestBody GrowthTask body) {
        return Result.success(outcomes.link(user(token),id,body.planId(),body.suggestionIndex()));
    }

    @ExceptionHandler(TrainingException.class)
    public ResponseEntity<Result<?>> trainingError(TrainingException error) {
        return ResponseEntity.status(error.status()).body(Result.error(error.getMessage(), Map.of("errorCode", error.code())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<?>> invalidBody() {
        return ResponseEntity.badRequest().body(Result.error("请检查输入内容、请求标识和版本号；回答最多4000字"));
    }

    private Long user(String token) {
        try { return Long.parseLong(String.valueOf(JwtUtil.parseToken(token).get("id"))); }
        catch (Exception e) { throw new TrainingException(401, "LOGIN_REQUIRED", "登录已失效，请重新登录"); }
    }
}
