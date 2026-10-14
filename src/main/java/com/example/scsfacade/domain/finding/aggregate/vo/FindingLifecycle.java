package com.example.scsfacade.domain.finding.aggregate.vo;

import java.time.Instant;
import java.util.Objects;

/**
 * 弱點生命週期與版本歷程 (FindingLifecycle) 實質物件。
 * <p>
 * DDD 實質物件，封裝弱點在 CI/CD 流程中的偵測、刷新與修復 Commit 雜湊及各階段時間戳記。
 * 遵守規範：純 Java 實作，不可變性 (Immutable)，不依賴任何外部框架。
 * </p>
 */
public final class FindingLifecycle {

    /**
     * 首次偵測到該弱點之 Git Commit 雜湊值。
     */
    private final String firstSeenCommit;

    /**
     * 最近一次掃描依然偵測到該弱點之 Git Commit 雜湊值。
     */
    private final String lastSeenCommit;

    /**
     * 該弱點被標記為修復結案時之 Git Commit 雜湊值（未修復時為 null）。
     */
    private final String resolvedInCommit;

    /**
     * 首次偵測到該弱點之精確時間戳記。
     */
    private final Instant firstSeenAt;

    /**
     * 最近一次掃描偵測到該弱點之時間戳記。
     */
    private final Instant lastSeenAt;

    /**
     * 該弱點被修復結案之時間戳記（未修復時為 null）。
     */
    private final Instant resolvedAt;

    public FindingLifecycle(String firstSeenCommit,
                            String lastSeenCommit,
                            String resolvedInCommit,
                            Instant firstSeenAt,
                            Instant lastSeenAt,
                            Instant resolvedAt) {
        this.firstSeenCommit = firstSeenCommit != null ? firstSeenCommit.trim() : "";
        this.lastSeenCommit = lastSeenCommit != null ? lastSeenCommit.trim() : this.firstSeenCommit;
        this.resolvedInCommit = resolvedInCommit != null ? resolvedInCommit.trim() : null;
        this.firstSeenAt = firstSeenAt != null ? firstSeenAt : Instant.now();
        this.lastSeenAt = lastSeenAt != null ? lastSeenAt : this.firstSeenAt;
        this.resolvedAt = resolvedAt;
    }

    /**
     * 工廠方法：首次發現弱點時初始化生命週期。
     *
     * @param commitHash 首次發現之 Commit
     * @param now        首次發現之時間
     * @return 新初始化的 FindingLifecycle 實質物件
     */
    public static FindingLifecycle init(String commitHash, Instant now) {
        Instant timestamp = now != null ? now : Instant.now();
        return new FindingLifecycle(commitHash, commitHash, null, timestamp, timestamp, null);
    }

    /**
     * 工廠方法：完整建構生命週期（供持久化還原使用）。
     */
    public static FindingLifecycle of(String firstSeenCommit,
                                      String lastSeenCommit,
                                      String resolvedInCommit,
                                      Instant firstSeenAt,
                                      Instant lastSeenAt,
                                      Instant resolvedAt) {
        return new FindingLifecycle(firstSeenCommit, lastSeenCommit, resolvedInCommit, firstSeenAt, lastSeenAt, resolvedAt);
    }

    /**
     * 狀態轉移：後續掃描再次命中，更新最新 Commit 與掃描時間。
     *
     * @param commitHash 當前掃描 Commit
     * @param scanTime   掃描時間
     * @return 衍生之新 FindingLifecycle 實質物件
     */
    public FindingLifecycle touch(String commitHash, Instant scanTime) {
        String newLastCommit = (commitHash != null && !commitHash.trim().isEmpty())
                ? commitHash.trim()
                : this.lastSeenCommit;
        Instant newLastSeenAt = scanTime != null ? scanTime : Instant.now();
        return new FindingLifecycle(
                this.firstSeenCommit,
                newLastCommit,
                this.resolvedInCommit,
                this.firstSeenAt,
                newLastSeenAt,
                this.resolvedAt
        );
    }

    /**
     * 狀態轉移：在特定 Commit 修復結案。
     *
     * @param commitHash 修復結案之 Commit
     * @param resolvedTime 修復時間
     * @return 衍生之新 FindingLifecycle 實質物件
     */
    public FindingLifecycle resolve(String commitHash, Instant resolvedTime) {
        Instant timestamp = resolvedTime != null ? resolvedTime : Instant.now();
        String resolveCommit = (commitHash != null && !commitHash.trim().isEmpty())
                ? commitHash.trim()
                : this.lastSeenCommit;
        return new FindingLifecycle(
                this.firstSeenCommit,
                this.lastSeenCommit,
                resolveCommit,
                this.firstSeenAt,
                timestamp,
                timestamp
        );
    }

    public String getFirstSeenCommit() {
        return firstSeenCommit;
    }

    public String getLastSeenCommit() {
        return lastSeenCommit;
    }

    public String getResolvedInCommit() {
        return resolvedInCommit;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FindingLifecycle that)) {
            return false;
        }
        return Objects.equals(firstSeenCommit, that.firstSeenCommit) &&
                Objects.equals(lastSeenCommit, that.lastSeenCommit) &&
                Objects.equals(resolvedInCommit, that.resolvedInCommit) &&
                Objects.equals(firstSeenAt, that.firstSeenAt) &&
                Objects.equals(lastSeenAt, that.lastSeenAt) &&
                Objects.equals(resolvedAt, that.resolvedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstSeenCommit, lastSeenCommit, resolvedInCommit, firstSeenAt, lastSeenAt, resolvedAt);
    }
}
