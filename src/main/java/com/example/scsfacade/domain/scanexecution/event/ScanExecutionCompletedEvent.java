package com.example.scsfacade.domain.scanexecution.event;

import com.example.scsfacade.domain.event.DomainEvent;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionId;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanSummary;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/**
 * 掃描執行已完成領域事件 (ScanExecutionCompletedEvent)。
 * <p>
 * 遵循 N + Ved + Event 命名規範與 DomainEvent 契約。
 * 當掃描作業完成結算並產出指標摘要時由 ScanExecution 聚合根觸發。
 * </p>
 */
public record ScanExecutionCompletedEvent(
        UUID eventId,
        ScanExecutionId executionId,
        String projectId,
        String branch,
        String commitHash,
        boolean passedQualityGate,
        ScanSummary summary,
        LocalDateTime occurredAt
) implements DomainEvent {

    public ScanExecutionCompletedEvent {
        Objects.requireNonNull(eventId, "eventId 不可為空");
        Objects.requireNonNull(executionId, "executionId 不可為空");
        Objects.requireNonNull(projectId, "projectId 不可為空");
        Objects.requireNonNull(summary, "summary 不可為空");
        Objects.requireNonNull(occurredAt, "occurredAt 不可為空");
    }

    public ScanExecutionCompletedEvent(ScanExecutionId executionId,
                                       String projectId,
                                       String branch,
                                       String commitHash,
                                       boolean passedQualityGate,
                                       ScanSummary summary,
                                       LocalDateTime occurredAt) {
        this(UUID.randomUUID(), executionId, projectId, branch, commitHash, passedQualityGate, summary,
                occurredAt != null ? occurredAt : LocalDateTime.now());
    }

    public ScanExecutionCompletedEvent(ScanExecutionId executionId,
                                       String projectId,
                                       String branch,
                                       String commitHash,
                                       boolean passedQualityGate,
                                       ScanSummary summary,
                                       Instant completedAt) {
        this(UUID.randomUUID(), executionId, projectId, branch, commitHash, passedQualityGate, summary,
                completedAt != null ? LocalDateTime.ofInstant(completedAt, ZoneId.systemDefault()) : LocalDateTime.now());
    }

    @Override
    public String aggregateType() {
        return "SCAN_EXECUTION";
    }

    @Override
    public String aggregateId() {
        return executionId.getValue();
    }
}
