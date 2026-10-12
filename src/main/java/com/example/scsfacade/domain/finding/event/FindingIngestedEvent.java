package com.example.scsfacade.domain.finding.event;

import com.example.scsfacade.domain.finding.aggregate.vo.FindingId;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;

import java.time.Instant;

/**
 * 弱點已匯入領域事件 (FindingIngestedEvent)。
 * <p>
 * 遵循 N + Ved + Event 命名規範。當一筆新的弱點被分析並匯入系統時由 Aggregate 觸發。
 * </p>
 */
public final class FindingIngestedEvent {

    private final FindingId findingId;
    private final String projectId;
    private final Severity severity;
    private final Instant occurredAt;

    public FindingIngestedEvent(FindingId findingId, String projectId, Severity severity, Instant occurredAt) {
        this.findingId = findingId;
        this.projectId = projectId;
        this.severity = severity;
        this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
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

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
