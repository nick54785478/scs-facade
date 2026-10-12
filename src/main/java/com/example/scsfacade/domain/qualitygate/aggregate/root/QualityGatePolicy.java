package com.example.scsfacade.domain.qualitygate.aggregate.root;

import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import com.example.scsfacade.domain.qualitygate.aggregate.vo.QualityGateStatus;
import com.example.scsfacade.domain.qualitygate.aggregate.vo.QualityGateThreshold;
import com.example.scsfacade.domain.qualitygate.event.QualityGateEvaluatedEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 品質門禁策略聚合根 (QualityGatePolicy Aggregate Root)。
 * <p>
 * 封裝專案門禁規則評估之領域邏輯。
 * 純 Java 實踐，嚴禁使用 Lombok。
 * </p>
 */
public class QualityGatePolicy {

    private final String projectId;
    private QualityGateThreshold threshold;
    private final List<Object> domainEvents = new ArrayList<>();

    public QualityGatePolicy(String projectId, QualityGateThreshold threshold) {
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("專案識別碼不可為空");
        }
        this.projectId = projectId.trim();
        this.threshold = threshold != null ? threshold : QualityGateThreshold.defaultStrict();
    }

    /**
     * 評估傳入之待處理弱點清單是否符合門禁標準。
     *
     * @param activeFindings 專案目前活躍中的弱點清單
     * @return 評估狀態 (PASSED 或 FAILED)
     */
    public QualityGateStatus evaluate(List<Finding> activeFindings) {
        int criticalCount = 0;
        int highCount = 0;

        if (activeFindings != null) {
            for (Finding finding : activeFindings) {
                if (finding.getStatus().isActive()) {
                    if (finding.getSeverity() == Severity.CRITICAL) {
                        criticalCount++;
                    } else if (finding.getSeverity() == Severity.HIGH) {
                        highCount++;
                    }
                }
            }
        }

        boolean passed = criticalCount <= threshold.getMaxCritical() && highCount <= threshold.getMaxHigh();
        QualityGateStatus status = passed ? QualityGateStatus.PASSED : QualityGateStatus.FAILED;

        Instant now = Instant.now();
        registerEvent(new QualityGateEvaluatedEvent(projectId, status, criticalCount, highCount, now));
        return status;
    }

    /**
     * 更新門檻設定。
     *
     * @param newThreshold 新的門檻實質物件
     */
    public void updateThreshold(QualityGateThreshold newThreshold) {
        if (newThreshold != null) {
            this.threshold = newThreshold;
        }
    }

    protected void registerEvent(Object event) {
        if (event != null) {
            this.domainEvents.add(event);
        }
    }

    public List<Object> pullDomainEvents() {
        List<Object> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public String getProjectId() {
        return projectId;
    }

    public QualityGateThreshold getThreshold() {
        return threshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QualityGatePolicy that)) return false;
        return Objects.equals(projectId, that.projectId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(projectId);
    }
}
