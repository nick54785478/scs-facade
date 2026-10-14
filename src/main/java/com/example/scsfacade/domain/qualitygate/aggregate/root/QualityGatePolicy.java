package com.example.scsfacade.domain.qualitygate.aggregate.root;

import com.example.scsfacade.domain.event.DomainEvent;
import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import com.example.scsfacade.domain.qualitygate.aggregate.vo.QualityGatePolicyId;
import com.example.scsfacade.domain.qualitygate.aggregate.vo.QualityGateStatus;
import com.example.scsfacade.domain.qualitygate.aggregate.vo.QualityGateThreshold;
import com.example.scsfacade.domain.qualitygate.event.QualityGateEvaluatedEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 品質門禁策略聚合根 (QualityGatePolicy Aggregate Root)。
 * <p>
 * 封裝專案門禁規則評估之領域邏輯。
 * 遵循 DDD 規範：純 Java 實作，零外層依賴，嚴禁使用 Lombok。
 * 建構子私有化，透過 create 業務工廠或 reconstitute 還原工廠建立。
 * </p>
 */
public class QualityGatePolicy {

    /**
     * 門禁策略聚合根宇宙唯一識別碼 (QualityGatePolicyId)。
     */
    private final QualityGatePolicyId id;

    /**
     * 該策略所套用之專案代碼 (Project Identifier)。
     */
    private final String projectId;

    /**
     * 品質門禁門檻標準實質物件 (QualityGateThreshold)，封裝重大風險上限與阻擋規則。
     */
    private QualityGateThreshold threshold;

    /**
     * 內部累積之領域事件清單 (In-Memory Domain Event Buffer)。
     */
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private QualityGatePolicy(QualityGatePolicyId id, String projectId, QualityGateThreshold threshold) {
        this.id = Objects.requireNonNull(id, "QualityGatePolicyId 不可為空");
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("專案識別碼不可為空");
        }
        this.projectId = projectId.trim();
        this.threshold = threshold != null ? threshold : QualityGateThreshold.defaultStrict();
    }

    /**
     * 業務工廠方法：依專案代碼與門檻標準建立門禁策略。
     *
     * @param projectId 專案代碼
     * @param threshold 門檻標準
     * @return 新建的 QualityGatePolicy 實例
     */
    public static QualityGatePolicy create(String projectId, QualityGateThreshold threshold) {
        return new QualityGatePolicy(QualityGatePolicyId.generate(), projectId, threshold);
    }

    /**
     * 業務工廠方法：建立預設嚴格標準之門禁策略。
     *
     * @param projectId 專案代碼
     * @return 預設之 QualityGatePolicy 實例
     */
    public static QualityGatePolicy createDefault(String projectId) {
        return create(projectId, QualityGateThreshold.defaultStrict());
    }

    /**
     * 持久化還原工廠方法 (Reconstitution Factory)。
     */
    public static QualityGatePolicy reconstitute(QualityGatePolicyId id, String projectId, QualityGateThreshold threshold) {
        return new QualityGatePolicy(id, projectId, threshold);
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

        LocalDateTime now = LocalDateTime.now();
        registerEvent(new QualityGateEvaluatedEvent(this.id, projectId, status, criticalCount, highCount, now));
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

    protected void registerEvent(DomainEvent event) {
        if (event != null) {
            this.domainEvents.add(event);
        }
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    public QualityGatePolicyId getId() {
        return id;
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
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
