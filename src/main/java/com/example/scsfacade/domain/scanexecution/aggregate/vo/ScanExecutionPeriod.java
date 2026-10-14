package com.example.scsfacade.domain.scanexecution.aggregate.vo;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 掃描執行時程與耗時 (ScanExecutionPeriod) 實質物件。
 * <p>
 * DDD 實質物件，封裝掃描的開始時間、完成時間、耗時與錯誤訊息。
 * 遵循純 Java、不可變 (Immutable) 原則。
 * </p>
 */
public final class ScanExecutionPeriod {

    /**
     * 掃描作業開始初始化與執行之時間戳記。
     */
    private final Instant startedAt;

    /**
     * 掃描作業完成或異常終止之時間戳記（若仍執行中則為 null）。
     */
    private final Instant completedAt;

    /**
     * 掃描作業失敗時之錯誤訊息或異常堆疊摘要（成功時為 null）。
     */
    private final String errorMessage;

    public ScanExecutionPeriod(Instant startedAt, Instant completedAt, String errorMessage) {
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt 不可為空");
        this.completedAt = completedAt;
        this.errorMessage = errorMessage;
    }

    public static ScanExecutionPeriod start(Instant startedAt) {
        return new ScanExecutionPeriod(startedAt != null ? startedAt : Instant.now(), null, null);
    }

    public static ScanExecutionPeriod of(Instant startedAt, Instant completedAt, String errorMessage) {
        return new ScanExecutionPeriod(startedAt, completedAt, errorMessage);
    }

    public ScanExecutionPeriod complete(Instant completedAt) {
        Instant completed = completedAt != null ? completedAt : Instant.now();
        return new ScanExecutionPeriod(this.startedAt, completed, null);
    }

    public ScanExecutionPeriod fail(String errorMessage, Instant completedAt) {
        Instant completed = completedAt != null ? completedAt : Instant.now();
        return new ScanExecutionPeriod(this.startedAt, completed, errorMessage != null ? errorMessage : "未知異常");
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Duration getDuration() {
        if (completedAt == null) {
            return Duration.between(startedAt, Instant.now());
        }
        return Duration.between(startedAt, completedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ScanExecutionPeriod that)) return false;
        return Objects.equals(startedAt, that.startedAt) &&
                Objects.equals(completedAt, that.completedAt) &&
                Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startedAt, completedAt, errorMessage);
    }
}
