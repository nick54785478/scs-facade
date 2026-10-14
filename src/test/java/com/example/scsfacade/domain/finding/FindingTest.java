package com.example.scsfacade.domain.finding;

import com.example.scsfacade.domain.event.DomainEvent;
import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingCategory;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingFingerprint;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingId;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingLifecycle;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingLocation;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingStatus;
import com.example.scsfacade.domain.finding.aggregate.vo.RuleInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import com.example.scsfacade.domain.finding.event.FindingIngestedEvent;
import com.example.scsfacade.domain.finding.event.FindingStatusChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 弱點聚合根領域單元測試 (FindingTest)。
 * <p>
 * 遵循 AGENTS.md 測試規範，使用 Gherkin 語法註解業務情境，全面驗證聚合根行為與領域事件契約。
 * </p>
 */
class FindingTest {

    private final ScannerToolInfo toolInfo = new ScannerToolInfo("SonarQube", "10.0", "SAST");
    private final RuleInfo ruleInfo = new RuleInfo("java:S2077", "SQL Injection", List.of("CWE-89"), "CVE-2023-1234");
    private final FindingLocation location = new FindingLocation("src/dao/UserDao.java", 42, 45, "SELECT * FROM users");

    /**
     * Feature: 弱點聚合根新建
     * Scenario: 建立新弱點並發布 FindingIngestedEvent
     * Given: 專案代碼、分支、Commit 與掃描規則資訊
     * When: 呼叫 Finding.createNew()
     * Then: 狀態為 OPEN，產生指紋，累積符合 DomainEvent 契約之 FindingIngestedEvent
     */
    @Test
    @DisplayName("弱點新建 - 應正確初始化聚合根並發布符合契約的 FindingIngestedEvent")
    void shouldCreateNewFindingAndPublishIngestedEvent() {
        // Given & When
        Finding finding = Finding.createNew(
                "scs-core",
                "main",
                "commit-101",
                toolInfo,
                FindingCategory.VULNERABILITY,
                ruleInfo,
                Severity.CRITICAL,
                location,
                "{}"
        );

        // Then
        assertThat(finding.getId()).isNotNull();
        assertThat(finding.getStatus()).isEqualTo(FindingStatus.OPEN);
        assertThat(finding.getSeverity()).isEqualTo(Severity.CRITICAL);
        assertThat(finding.getFingerprint()).isNotNull();

        List<DomainEvent> events = finding.pullDomainEvents();
        assertThat(events).hasSize(1);

        DomainEvent event = events.get(0);
        assertThat(event).isInstanceOf(FindingIngestedEvent.class);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.aggregateType()).isEqualTo("FINDING");
        assertThat(event.aggregateId()).isEqualTo(finding.getId().getValue());
        assertThat(event.occurredAt()).isNotNull();

        FindingIngestedEvent ingestedEvent = (FindingIngestedEvent) event;
        assertThat(ingestedEvent.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(ingestedEvent.projectId()).isEqualTo("scs-core");
    }

    /**
     * Feature: 弱點狀態轉移為已修復
     * Scenario: 標記弱點為 RESOLVED 並發布 FindingStatusChangedEvent
     * Given: 初始狀態為 OPEN 的弱點
     * When: 呼叫 markAsResolved("fix-commit", now)
     * Then: 狀態轉為 RESOLVED，累積 FindingStatusChangedEvent
     */
    @Test
    @DisplayName("弱點修復 - 應正確更新生命週期狀態並發布 FindingStatusChangedEvent")
    void shouldMarkAsResolvedAndPublishStatusChangedEvent() {
        // Given
        Finding finding = Finding.createNew("scs-core", "main", "commit-101", toolInfo, ruleInfo, Severity.HIGH, location, "{}");
        finding.pullDomainEvents(); // 清空初始化事件

        // When
        finding.markAsResolved("commit-202", Instant.now());

        // Then
        assertThat(finding.getStatus()).isEqualTo(FindingStatus.RESOLVED);
        assertThat(finding.getLifecycle().getResolvedInCommit()).isEqualTo("commit-202");

        List<DomainEvent> events = finding.pullDomainEvents();
        assertThat(events).hasSize(1);

        DomainEvent event = events.get(0);
        assertThat(event).isInstanceOf(FindingStatusChangedEvent.class);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.aggregateType()).isEqualTo("FINDING");
        assertThat(event.aggregateId()).isEqualTo(finding.getId().getValue());
        assertThat(event.occurredAt()).isNotNull();

        FindingStatusChangedEvent statusEvent = (FindingStatusChangedEvent) event;
        assertThat(statusEvent.previousStatus()).isEqualTo(FindingStatus.OPEN);
        assertThat(statusEvent.newStatus()).isEqualTo(FindingStatus.RESOLVED);
    }

    /**
     * Feature: 弱點狀態標記為誤報
     * Scenario: 標記弱點為 FALSE_POSITIVE
     * Given: 初始狀態為 OPEN 的弱點
     * When: 呼叫 markAsFalsePositive()
     * Then: 狀態轉移為 FALSE_POSITIVE，累積 FindingStatusChangedEvent
     */
    @Test
    @DisplayName("弱點誤報標記 - 應正確更新狀態並發布事件")
    void shouldMarkAsFalsePositiveAndPublishStatusChangedEvent() {
        // Given
        Finding finding = Finding.createNew("scs-core", "main", "commit-101", toolInfo, ruleInfo, Severity.MEDIUM, location, "{}");
        finding.pullDomainEvents();

        // When
        finding.markAsFalsePositive();

        // Then
        assertThat(finding.getStatus()).isEqualTo(FindingStatus.FALSE_POSITIVE);

        List<DomainEvent> events = finding.pullDomainEvents();
        assertThat(events).hasSize(1);
        DomainEvent event = events.get(0);
        assertThat(event).isInstanceOf(FindingStatusChangedEvent.class);
        FindingStatusChangedEvent statusEvent = (FindingStatusChangedEvent) event;
        assertThat(statusEvent.previousStatus()).isEqualTo(FindingStatus.OPEN);
        assertThat(statusEvent.newStatus()).isEqualTo(FindingStatus.FALSE_POSITIVE);
    }

    /**
     * Feature: 弱點持久化還原
     * Scenario: 透過 reconstitute 工廠從資料庫還原聚合根
     * Given: 完整的持久化屬性與生命週期物件
     * When: 呼叫 Finding.reconstitute()
     * Then: 成功重建聚合根，且不發布任何領域事件
     */
    @Test
    @DisplayName("弱點還原 - 從資料庫實體還原時不應累積任何領域事件")
    void shouldReconstituteWithoutDomainEvents() {
        // Given
        FindingId id = FindingId.generate();
        FindingFingerprint fingerprint = FindingFingerprint.compute("scs-core", "main", "src/Test.java", "rule-1", "code");
        FindingLifecycle lifecycle = FindingLifecycle.init("c1", Instant.now());

        // When
        Finding reconstituted = Finding.reconstitute(
                id,
                "scs-core",
                "main",
                fingerprint,
                toolInfo,
                FindingCategory.VULNERABILITY,
                ruleInfo,
                Severity.HIGH,
                location,
                FindingStatus.OPEN,
                lifecycle,
                "{}"
        );

        // Then
        assertThat(reconstituted.getId()).isEqualTo(id);
        assertThat(reconstituted.pullDomainEvents()).isEmpty();
    }
}
