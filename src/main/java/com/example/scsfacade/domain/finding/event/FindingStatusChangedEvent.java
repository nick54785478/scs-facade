package com.example.scsfacade.domain.finding.event;

import com.example.scsfacade.domain.finding.aggregate.vo.FindingId;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingStatus;

import java.time.Instant;

/**
 * 弱點狀態已變更領域事件 (FindingStatusChangedEvent)。
 * <p>
 * 遵循 N + Ved + Event 命名規範。當弱點生命週期狀態（如被修復或標記誤報）發生變化時發布。
 * </p>
 */
public final class FindingStatusChangedEvent {

    private final FindingId findingId;
    private final FindingStatus previousStatus;
    private final FindingStatus newStatus;
    private final Instant occurredAt;

    public FindingStatusChangedEvent(FindingId findingId, FindingStatus previousStatus, FindingStatus newStatus, Instant occurredAt) {
        this.findingId = findingId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
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

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
