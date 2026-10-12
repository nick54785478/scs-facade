package com.example.scsfacade.domain.qualitygate.event;

import com.example.scsfacade.domain.qualitygate.aggregate.vo.QualityGateStatus;

import java.time.Instant;

/**
 * 品質門禁已評估領域事件 (QualityGateEvaluatedEvent)。
 * <p>
 * 遵循 N + Ved + Event 命名規範。當 CI/CD 或系統完成門禁審查時由 Aggregate 觸發。
 * </p>
 */
public final class QualityGateEvaluatedEvent {

    private final String projectId;
    private final QualityGateStatus status;
    private final int criticalCount;
    private final int highCount;
    private final Instant evaluatedAt;

    public QualityGateEvaluatedEvent(String projectId, QualityGateStatus status, int criticalCount, int highCount, Instant evaluatedAt) {
        this.projectId = projectId;
        this.status = status;
        this.criticalCount = criticalCount;
        this.highCount = highCount;
        this.evaluatedAt = evaluatedAt != null ? evaluatedAt : Instant.now();
    }

    public String getProjectId() {
        return projectId;
    }

    public QualityGateStatus getStatus() {
        return status;
    }

    public int getCriticalCount() {
        return criticalCount;
    }

    public int getHighCount() {
        return highCount;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }
}
