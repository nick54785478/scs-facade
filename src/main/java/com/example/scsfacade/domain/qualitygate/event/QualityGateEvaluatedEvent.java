package com.example.scsfacade.domain.qualitygate.event;

import com.example.scsfacade.domain.event.DomainEvent;
import com.example.scsfacade.domain.qualitygate.aggregate.vo.QualityGatePolicyId;
import com.example.scsfacade.domain.qualitygate.aggregate.vo.QualityGateStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/**
 * 品質門禁已評估領域事件 (QualityGateEvaluatedEvent)。
 * <p>
 * 遵循 N + Ved + Event 命名規範與 DomainEvent 契約。
 * 當 CI/CD 或系統完成門禁審查時由 QualityGatePolicy 聚合根觸發。
 * </p>
 */
public record QualityGateEvaluatedEvent(
        UUID eventId,
        QualityGatePolicyId policyId,
        String projectId,
        QualityGateStatus status,
        int criticalCount,
        int highCount,
        LocalDateTime occurredAt
) implements DomainEvent {

    public QualityGateEvaluatedEvent {
        Objects.requireNonNull(eventId, "eventId 不可為空");
        Objects.requireNonNull(policyId, "policyId 不可為空");
        Objects.requireNonNull(projectId, "projectId 不可為空");
        Objects.requireNonNull(status, "status 不可為空");
        Objects.requireNonNull(occurredAt, "occurredAt 不可為空");
    }

    public QualityGateEvaluatedEvent(QualityGatePolicyId policyId,
                                    String projectId,
                                    QualityGateStatus status,
                                    int criticalCount,
                                    int highCount,
                                    LocalDateTime occurredAt) {
        this(UUID.randomUUID(), policyId, projectId, status, criticalCount, highCount,
                occurredAt != null ? occurredAt : LocalDateTime.now());
    }

    public QualityGateEvaluatedEvent(QualityGatePolicyId policyId,
                                    String projectId,
                                    QualityGateStatus status,
                                    int criticalCount,
                                    int highCount,
                                    Instant evaluatedAt) {
        this(UUID.randomUUID(), policyId, projectId, status, criticalCount, highCount,
                evaluatedAt != null ? LocalDateTime.ofInstant(evaluatedAt, ZoneId.systemDefault()) : LocalDateTime.now());
    }

    public QualityGateEvaluatedEvent(QualityGatePolicyId policyId,
                                    String projectId,
                                    QualityGateStatus status,
                                    int criticalCount,
                                    int highCount) {
        this(UUID.randomUUID(), policyId, projectId, status, criticalCount, highCount, LocalDateTime.now());
    }

    @Override
    public String aggregateType() {
        return "QUALITY_GATE_POLICY";
    }

    @Override
    public String aggregateId() {
        return policyId.getValue();
    }

    public QualityGatePolicyId getPolicyId() {
        return policyId;
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

    public LocalDateTime getEvaluatedAt() {
        return occurredAt;
    }
}
