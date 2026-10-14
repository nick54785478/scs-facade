package com.example.scsfacade.domain.qualitygate;

import com.example.scsfacade.domain.event.DomainEvent;
import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingCategory;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingLocation;
import com.example.scsfacade.domain.finding.aggregate.vo.RuleInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import com.example.scsfacade.domain.qualitygate.aggregate.root.QualityGatePolicy;
import com.example.scsfacade.domain.qualitygate.aggregate.vo.QualityGatePolicyId;
import com.example.scsfacade.domain.qualitygate.aggregate.vo.QualityGateStatus;
import com.example.scsfacade.domain.qualitygate.aggregate.vo.QualityGateThreshold;
import com.example.scsfacade.domain.qualitygate.event.QualityGateEvaluatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 品質門禁策略聚合根領域測試 (QualityGatePolicyTest)。
 */
class QualityGatePolicyTest {

    /**
     * Feature: 門禁策略建立與預設值
     * Scenario: 建立預設嚴格標準門禁策略
     * Given: 專案代碼
     * When: 呼叫 QualityGatePolicy.createDefault()
     * Then: 門檻為 0 Critical 與 2 High，且 ID 自動生成
     */
    @Test
    @DisplayName("建立預設門禁策略 - 應正確初始化識別碼與預設嚴格門檻")
    void shouldCreateDefaultPolicySuccessfully() {
        // When
        QualityGatePolicy policy = QualityGatePolicy.createDefault("test-proj");

        // Then
        assertThat(policy.getId()).isNotNull();
        assertThat(policy.getProjectId()).isEqualTo("test-proj");
        assertThat(policy.getThreshold().getMaxCritical()).isZero();
        assertThat(policy.getThreshold().getMaxHigh()).isEqualTo(2);
    }

    /**
     * Feature: 門禁評估通過
     * Scenario: 無 Critical 且 High 未超標時評估為 PASSED
     * Given: 活躍弱點清單中無 Critical 且有 1 個 High
     * When: 執行 evaluate()
     * Then: 回傳 PASSED，並發布 QualityGateEvaluatedEvent
     */
    @Test
    @DisplayName("門禁評估通過 - 弱點在門檻內時應判定為 PASSED 並發布事件")
    void shouldPassQualityGateWhenWithinThreshold() {
        // Given
        QualityGatePolicy policy = QualityGatePolicy.create("test-proj", new QualityGateThreshold(0, 2, true));
        Finding highFinding = Finding.createNew(
                "test-proj", "main", "commit-1",
                new ScannerToolInfo("SonarQube", "10.0", "SAST"),
                FindingCategory.VULNERABILITY,
                new RuleInfo("rule-1", "High Vuln", List.of(), null, List.of()),
                Severity.HIGH,
                new FindingLocation("src/App.java", 10, 10, "code"),
                "{}"
        );

        // When
        QualityGateStatus status = policy.evaluate(List.of(highFinding));

        // Then
        assertThat(status).isEqualTo(QualityGateStatus.PASSED);

        List<DomainEvent> events = policy.pullDomainEvents();
        assertThat(events).hasSize(1);
        DomainEvent event = events.get(0);
        assertThat(event).isInstanceOf(QualityGateEvaluatedEvent.class);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.aggregateType()).isEqualTo("QUALITY_GATE_POLICY");
        assertThat(event.aggregateId()).isEqualTo(policy.getId().getValue());
        assertThat(event.occurredAt()).isNotNull();
    }

    /**
     * Feature: 門禁評估失敗
     * Scenario: 存在 Critical 弱點時阻擋
     * Given: 活躍弱點清單中含有 1 個 Critical 弱點
     * When: 執行 evaluate()
     * Then: 回傳 FAILED
     */
    @Test
    @DisplayName("門禁評估阻擋 - 存在 Critical 弱點時應判定為 FAILED")
    void shouldFailQualityGateWhenCriticalExists() {
        // Given
        QualityGatePolicy policy = QualityGatePolicy.createDefault("test-proj");
        Finding criticalFinding = Finding.createNew(
                "test-proj", "main", "commit-1",
                new ScannerToolInfo("SonarQube", "10.0", "SAST"),
                FindingCategory.VULNERABILITY,
                new RuleInfo("rule-crit", "SQL Injection", List.of("CWE-89"), null, List.of()),
                Severity.CRITICAL,
                new FindingLocation("src/Dao.java", 20, 20, "query"),
                "{}"
        );

        // When
        QualityGateStatus status = policy.evaluate(List.of(criticalFinding));

        // Then
        assertThat(status).isEqualTo(QualityGateStatus.FAILED);
    }

    /**
     * Feature: 門禁策略還原
     * Scenario: 成功從持久化資料重建 QualityGatePolicy
     * Given: 已知之 PolicyId 與設定
     * When: 呼叫 QualityGatePolicy.reconstitute()
     * Then: 重建聚合根成功，無事件累積
     */
    @Test
    @DisplayName("門禁策略還原 - 應正確從持久化資料重建聚合根")
    void shouldReconstituteQualityGatePolicySuccessfully() {
        // Given
        QualityGatePolicyId id = QualityGatePolicyId.generate();
        QualityGateThreshold threshold = new QualityGateThreshold(1, 5, false);

        // When
        QualityGatePolicy policy = QualityGatePolicy.reconstitute(id, "test-proj", threshold);

        // Then
        assertThat(policy.getId()).isEqualTo(id);
        assertThat(policy.getProjectId()).isEqualTo("test-proj");
        assertThat(policy.getThreshold().getMaxCritical()).isEqualTo(1);
        assertThat(policy.pullDomainEvents()).isEmpty();
    }

    /**
     * Feature: 門禁策略門檻動態調整
     * Scenario: 呼叫 updateThreshold 更新門檻設定
     * Given: 現有門禁策略
     * When: 傳入新的 QualityGateThreshold
     * Then: 門檻成功更新，傳入 null 則予以忽略
     */
    @Test
    @DisplayName("門檻調整 - 應正確更新門禁策略閾值設定")
    void shouldUpdateThreshold() {
        // Given
        QualityGatePolicy policy = QualityGatePolicy.createDefault("test-proj");
        QualityGateThreshold newThreshold = new QualityGateThreshold(2, 10, false);

        // When
        policy.updateThreshold(newThreshold);

        // Then
        assertThat(policy.getThreshold().getMaxCritical()).isEqualTo(2);
        assertThat(policy.getThreshold().getMaxHigh()).isEqualTo(10);
        assertThat(policy.getThreshold().isBlockOnSecrets()).isFalse();

        // When: passing null
        policy.updateThreshold(null);
        assertThat(policy.getThreshold().getMaxCritical()).isEqualTo(2);
    }

    /**
     * Feature: 門禁策略實體相等性與參數防護
     * Scenario: 驗證 QualityGatePolicy 之 equals, hashCode 與無效參數防護
     * Given: Policy 與無效之負數門檻
     * When: 建立與比對
     * Then: 拋出 IllegalArgumentException，且 equals 依據 PolicyId 運作
     */
    @Test
    @DisplayName("實體相等性與參數防護 - 應基於 PolicyId 判定相等性並阻擋負數門檻")
    void shouldVerifyQualityGatePolicyEqualityAndValidation() {
        // Given
        QualityGatePolicy p1 = QualityGatePolicy.createDefault("test-proj");
        QualityGatePolicy p2 = QualityGatePolicy.reconstitute(p1.getId(), "test-proj", p1.getThreshold());
        QualityGatePolicy p3 = QualityGatePolicy.createDefault("test-proj");

        // Then
        assertThat(p1).isEqualTo(p2);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
        assertThat(p1).isNotEqualTo(p3);
        assertThat(p1.equals(null)).isFalse();
        assertThat(p1.equals(p1)).isTrue();

        // Validation
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new QualityGateThreshold(-1, 0, false))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new QualityGateThreshold(0, -1, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
