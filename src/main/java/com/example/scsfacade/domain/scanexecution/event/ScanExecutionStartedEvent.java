package com.example.scsfacade.domain.scanexecution.event;

import com.example.scsfacade.domain.event.DomainEvent;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionId;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/**
 * 掃描執行已啟動領域事件 (ScanExecutionStartedEvent)。
 * <p>
 * 遵循 N + Ved + Event 命名規範與 DomainEvent 契約。
 * 當掃描作業開始初始化並執行時由 ScanExecution 聚合根觸發。
 * </p>
 */
public record ScanExecutionStartedEvent(
        UUID eventId,
        ScanExecutionId executionId,
        String projectId,
        String branch,
        String commitHash,
        LocalDateTime occurredAt
) implements DomainEvent {

    public ScanExecutionStartedEvent {
        Objects.requireNonNull(eventId, "eventId 不可為空");
        Objects.requireNonNull(executionId, "executionId 不可為空");
        Objects.requireNonNull(projectId, "projectId 不可為空");
        Objects.requireNonNull(occurredAt, "occurredAt 不可為空");
    }

    public ScanExecutionStartedEvent(ScanExecutionId executionId, String projectId, String branch, String commitHash, LocalDateTime occurredAt) {
        this(UUID.randomUUID(), executionId, projectId, branch, commitHash, occurredAt != null ? occurredAt : LocalDateTime.now());
    }

    public ScanExecutionStartedEvent(ScanExecutionId executionId, String projectId, String branch, String commitHash, Instant startedAt) {
        this(UUID.randomUUID(), executionId, projectId, branch, commitHash,
                startedAt != null ? LocalDateTime.ofInstant(startedAt, ZoneId.systemDefault()) : LocalDateTime.now());
    }

    public ScanExecutionStartedEvent(ScanExecutionId executionId, String projectId, String branch, String commitHash) {
        this(UUID.randomUUID(), executionId, projectId, branch, commitHash, LocalDateTime.now());
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
