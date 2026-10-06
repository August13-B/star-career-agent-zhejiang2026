package org.example.web.service.training;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Single backend process, bounded execution, durable input, explicit restart recovery. */
@Slf4j
@Component
@ConditionalOnProperty(name = "training.worker.enabled", havingValue = "true", matchIfMissing = true)
public class TrainingWorker {
    private final TrainingService service;
    private final ScenarioAgentGateway gateway;
    private final TrainingOutcomeService outcomes;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(20), runnable -> { Thread t = new Thread(runnable, "training-worker"); t.setDaemon(true); return t; });
    private final ScheduledExecutorService recovery = Executors.newSingleThreadScheduledExecutor(r -> { Thread t=new Thread(r,"training-profile-recovery"); t.setDaemon(true); return t; });
    private final Map<TrainingService.Queued, FutureTask<Void>> running = new ConcurrentHashMap<>();

    public TrainingWorker(TrainingService service, ScenarioAgentGateway gateway, TrainingOutcomeService outcomes) { this.service = service; this.gateway = gateway; this.outcomes = outcomes; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void queued(TrainingService.Queued event) {
        FutureTask<Void> task = new FutureTask<>(() -> { execute(event); return null; }) {
            @Override protected void done() { running.remove(event, this); }
        };
        if (running.putIfAbsent(event, task) != null) return;
        try { executor.execute(task); }
        catch (RejectedExecutionException e) {
            running.remove(event, task);
            service.fail(event.runId(), event.attempt(), "QUEUE_FULL", "训练队列暂满，请稍后重试");
        }
    }

    private void execute(TrainingService.Queued event) {
        try {
            var work = service.claim(event.runId(), event.attempt());
            if (work == null) return;
            AtomicLong lastSave = new AtomicLong(0);
            var output = gateway.execute("training-" + work.sessionId() + "-" + work.runId() + "-" + work.attempt(), work.prompt(), text -> {
                long now = System.nanoTime();
                if (!"evaluate".equals(work.operation()) && now - lastSave.get() > TimeUnit.MILLISECONDS.toNanos(400)) {
                    service.partial(work, text); lastSave.set(now);
                }
            });
            service.complete(work, output);
            if ("evaluate".equals(work.operation())) {
                try { outcomes.apply(outcomes.owner(work.sessionId()), work.sessionId()); }
                catch (Exception e) { outcomes.failed(work.sessionId()); log.warn("训练画像更新待重试 session={}", work.sessionId()); }
            }
        } catch (Exception error) {
            String code = error instanceof TrainingException te ? te.code() : "TRAINING_EXECUTION_FAILED";
            String message = error instanceof TrainingException ? error.getMessage() : "训练执行失败，已保存的回答可以重试";
            log.warn("训练任务失败 run={} attempt={} code={}", event.runId(), event.attempt(), code);
            service.fail(event.runId(), event.attempt(), code, message);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void canceled(TrainingService.Canceled event) {
        running.forEach((key, task) -> {
            if (key.runId().equals(event.runId())) { task.cancel(true); executor.remove(task); }
        });
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        try {
            recoverProfiles();
            recovery.scheduleWithFixedDelay(this::recoverProfiles, 30, 30, TimeUnit.SECONDS);
            for (var run : service.unfinished()) {
                if ("queued".equals(run.getStatus())) queued(new TrainingService.Queued(run.getId(), run.getAttempt()));
                else service.fail(run.getId(), run.getAttempt(), "BACKEND_RESTARTED", "后端重启中断了本次生成，回答已保存，请重试");
            }
        } catch (Exception e) { log.warn("训练恢复检查未完成，请确认数据库迁移 010、011 已应用"); }
    }

    private void recoverProfiles() {
        try {
            for (Long id : outcomes.pending()) {
                try { outcomes.apply(outcomes.owner(id), id); }
                catch (Exception e) { outcomes.failed(id); log.warn("训练画像恢复待重试 session={}", id); }
            }
        } catch (Exception e) { log.warn("训练画像恢复暂不可用"); }
    }
    @PreDestroy public void close() { recovery.shutdownNow(); executor.shutdownNow(); }
}
