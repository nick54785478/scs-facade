package com.example.scsfacade.domain.finding.event;

import com.example.scsfacade.domain.event.DomainEvent;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingId;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/**
 * 弱點已匯入領域事件 (FindingIngestedEvent)。
 * <p>
 * 遵循 N + Ved + Event 命名規範與 DomainEvent 契約。
 * 當一筆新的弱點被分析並匯入系統時由 Finding 聚合根觸發。
 * </p>
 */
public record FindingIngestedEvent(
        UUID eventId,
        FindingId findingId,
        String projectId,
        Severity severity,
        LocalDateTime occurredAt
) implements DomainEvent {

    public FindingIngestedEvent {
        Objects.requireNonNull(eventId, "eventId 不可為空");
        Objects.requireNonNull(findingId, "findingId 不可為空");
        Objects.requireNonNull(projectId, "projectId 不可為空");
        Objects.requireNonNull(severity, "severity 不可為空");
        Objects.requireNonNull(occurredAt, "occurredAt 不可為空");
    }

    public FindingIngestedEvent(FindingId findingId, String projectId, Severity severity, LocalDateTime occurredAt) {
        this(UUID.randomUUID(), findingId, projectId, severity, occurredAt != null ? occurredAt : LocalDateTime.now());
    }

    public FindingIngestedEvent(FindingId findingId, String projectId, Severity severity, Instant occurredAt) {
        this(UUID.randomUUID(), findingId, projectId, severity,
                occurredAt != null ? LocalDateTime.ofInstant(occurredAt, ZoneId.systemDefault()) : LocalDateTime.now());
    }

    public FindingIngestedEvent(FindingId findingId, String projectId, Severity severity) {
        this(UUID.randomUUID(), findingId, projectId, severity, LocalDateTime.now());
    }

    @Override
    public String aggregateType() {
        return "FINDING";
    }

    @Override
    public String aggregateId() {
        return findingId.getValue();
    }

    public FindingId getFindingId() {
        return findingId;
    }

    public String getProjectId() {
        return projectId;
    }

    public Severity getSeverity() {
        return severity;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
