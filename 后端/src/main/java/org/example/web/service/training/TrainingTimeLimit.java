package org.example.web.service.training;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 单题限时口径（服务端权威判定）。
 *
 * <p>计时从「该题生成完成并下发」开始；提交时给予 {@link #GRACE_SECONDS} 秒宽限，
 * 避免前端在截止瞬间自动提交却因网络延迟被判成超时。
 *
 * <p>模板未配置 {@code timeLimitSeconds}（或 ≤0）时表示不限时，全部判定一律放行。
 */
public final class TrainingTimeLimit {

    /** 前端在截止瞬间自动提交，宽限数秒吸收网络与服务端时钟误差 */
    public static final int GRACE_SECONDS = 5;

    private TrainingTimeLimit() {
    }

    /**
     * 剩余秒数（可能为负）。
     *
     * @return 不限时（未下发或 limit ≤ 0）时返回 {@code null}
     */
    public static Long remainingSeconds(LocalDateTime startedAt, LocalDateTime now, int limitSeconds) {
        if (startedAt == null || now == null || limitSeconds <= 0) {
            return null;
        }
        return (long) limitSeconds - Duration.between(startedAt, now).getSeconds();
    }

    /** 是否已超时（超过限时 + 宽限）；不限时永远返回 false */
    public static boolean timedOut(LocalDateTime startedAt, LocalDateTime now, int limitSeconds) {
        Long remaining = remainingSeconds(startedAt, now, limitSeconds);
        return remaining != null && remaining < -GRACE_SECONDS;
    }
}
