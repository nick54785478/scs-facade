package com.example.scsfacade.domain.finding.event;

import com.example.scsfacade.domain.event.DomainEvent;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingId;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/**
 * 弱點狀態已變更領域事件 (FindingStatusChangedEvent)。
 * <p>
 * 遵循 N + Ved + Event 命名規範與 DomainEvent 契約。
 * 當弱點生命週期狀態（如被修復或標記誤報）發生變化時由 Finding 聚合根發布。
 * </p>
 */
public record FindingStatusChangedEvent(
        UUID eventId,
        FindingId findingId,
        FindingStatus previousStatus,
        FindingStatus newStatus,
        LocalDateTime occurredAt
) implements DomainEvent {

    public FindingStatusChangedEvent {
        Objects.requireNonNull(eventId, "eventId 不可為空");
        Objects.requireNonNull(findingId, "findingId 不可為空");
        Objects.requireNonNull(previousStatus, "previousStatus 不可為空");
        Objects.requireNonNull(newStatus, "newStatus 不可為空");
        Objects.requireNonNull(occurredAt, "occurredAt 不可為空");
    }

    public FindingStatusChangedEvent(FindingId findingId, FindingStatus previousStatus, FindingStatus newStatus, LocalDateTime occurredAt) {
        this(UUID.randomUUID(), findingId, previousStatus, newStatus, occurredAt != null ? occurredAt : LocalDateTime.now());
    }

    public FindingStatusChangedEvent(FindingId findingId, FindingStatus previousStatus, FindingStatus newStatus, Instant occurredAt) {
        this(UUID.randomUUID(), findingId, previousStatus, newStatus,
                occurredAt != null ? LocalDateTime.ofInstant(occurredAt, ZoneId.systemDefault()) : LocalDateTime.now());
    }

    public FindingStatusChangedEvent(FindingId findingId, FindingStatus previousStatus, FindingStatus newStatus) {
        this(UUID.randomUUID(), findingId, previousStatus, newStatus, LocalDateTime.now());
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

    public FindingStatus getPreviousStatus() {
        return previousStatus;
    }

    public FindingStatus getNewStatus() {
        return newStatus;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
