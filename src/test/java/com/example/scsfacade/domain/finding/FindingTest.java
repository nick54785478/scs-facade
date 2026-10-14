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

    /**
     * Feature: 弱點狀態確認為真實風險
     * Scenario: 標記弱點為 CONFIRMED 並發布 FindingStatusChangedEvent
     * Given: 初始狀態為 OPEN 的弱點
     * When: 呼叫 markAsConfirmed()
     * Then: 狀態轉移為 CONFIRMED，重複呼叫時保持等冪不發布額外事件
     */
    @Test
    @DisplayName("弱點確認 - 應正確更新狀態為 CONFIRMED 並具備等冪性")
    void shouldMarkAsConfirmedAndPublishStatusChangedEvent() {
        // Given
        Finding finding = Finding.createNew("scs-core", "main", "commit-101", toolInfo, ruleInfo, Severity.HIGH, location, "{}");
        finding.pullDomainEvents();

        // When
        finding.markAsConfirmed();

        // Then
        assertThat(finding.getStatus()).isEqualTo(FindingStatus.CONFIRMED);
        List<DomainEvent> events = finding.pullDomainEvents();
        assertThat(events).hasSize(1);
        FindingStatusChangedEvent event = (FindingStatusChangedEvent) events.get(0);
        assertThat(event.newStatus()).isEqualTo(FindingStatus.CONFIRMED);

        // Idempotency check
        finding.markAsConfirmed();
        assertThat(finding.pullDomainEvents()).isEmpty();
    }

    /**
     * Feature: 弱點掃描時間戳記刷新
     * Scenario: 於後續掃描再次偵測到同一弱點時刷新生命週期
     * Given: 已存在的弱點聚合根
     * When: 呼叫 touchScan(commitHash, scanTime) 與 touchScan(scanTime)
     * Then: 生命週期中之最後見到提交與時間被成功更新
     */
    @Test
    @DisplayName("掃描時間刷新 - 應正確更新生命週期中最後見到 Commit 與時間戳記")
    void shouldTouchScanAndUpdateLifecycle() {
        // Given
        Finding finding = Finding.createNew("scs-core", "main", "commit-101", toolInfo, ruleInfo, Severity.HIGH, location, "{}");
        Instant t1 = Instant.now().plusSeconds(60);

        // When
        finding.touchScan("commit-102", t1);

        // Then
        assertThat(finding.getLifecycle().getLastSeenCommit()).isEqualTo("commit-102");
        assertThat(finding.getLifecycle().getLastSeenAt()).isEqualTo(t1);

        // When: touchScan with single arg
        Instant t2 = Instant.now().plusSeconds(120);
        finding.touchScan(t2);
        assertThat(finding.getLifecycle().getLastSeenAt()).isEqualTo(t2);
    }

    /**
     * Feature: 弱點嚴重等級調整
     * Scenario: 調整弱點之 Severity
     * Given: Severity 為 HIGH 之弱點
     * When: 調整為 CRITICAL
     * Then: 嚴重度成功變更為 CRITICAL
     */
    @Test
    @DisplayName("嚴重等級調整 - 應正確更新弱點之嚴重度評級")
    void shouldAdjustSeverity() {
        // Given
        Finding finding = Finding.createNew("scs-core", "main", "commit-101", toolInfo, ruleInfo, Severity.HIGH, location, "{}");

        // When
        finding.adjustSeverity(Severity.CRITICAL);

        // Then
        assertThat(finding.getSeverity()).isEqualTo(Severity.CRITICAL);

        // When: adjust with null or same
        finding.adjustSeverity(null);
        assertThat(finding.getSeverity()).isEqualTo(Severity.CRITICAL);
    }

    /**
     * Feature: 弱點修復無參數重載與等冪保護
     * Scenario: 呼叫 markAsResolved() 無參數版本，且已修復狀態不重複觸發
     * Given: 初始 OPEN 弱點
     * When: 呼叫 markAsResolved() 兩次
     * Then: 僅第一次觸發事件，第二次保持等冪
     */
    @Test
    @DisplayName("弱點修復等冪性 - 已修復弱點再次呼叫修復不應重複觸發領域事件")
    void shouldMarkAsResolvedWithDefaultOverloadAndIdempotency() {
        // Given
        Finding finding = Finding.createNew("scs-core", "main", "commit-101", toolInfo, ruleInfo, Severity.MEDIUM, location, "{}");
        finding.pullDomainEvents();

        // When
        finding.markAsResolved();

        // Then
        assertThat(finding.getStatus()).isEqualTo(FindingStatus.RESOLVED);
        assertThat(finding.pullDomainEvents()).hasSize(1);

        // Repeat
        finding.markAsResolved();
        assertThat(finding.pullDomainEvents()).isEmpty();
    }

    /**
     * Feature: 弱點聚合根相等性與屬性查詢
     * Scenario: 驗證 Finding 之 equals, hashCode 與各種 getters
     * Given: 相同 ID 與不同 ID 之 Finding
     * When: 比對與讀取屬性
     * Then: 基於 FindingId 判定相等性，所有屬性正確回傳
     */
    @Test
    @DisplayName("實體相等性與屬性 - 應基於 FindingId 判定 equals 與 hashCode")
    void shouldVerifyFindingEqualityAndGetters() {
        // Given
        Finding f1 = Finding.createNew("scs-core", "main", "commit-101", toolInfo, ruleInfo, Severity.HIGH, location, "{\"raw\":1}");
        Finding f2 = Finding.reconstitute(
                f1.getId(), f1.getProjectId(), f1.getBranch(), f1.getFingerprint(),
                f1.getToolInfo(), f1.getCategory(), f1.getRuleInfo(), f1.getSeverity(),
                f1.getLocation(), f1.getStatus(), f1.getLifecycle(), f1.getRawDetails()
        );
        Finding f3 = Finding.createNew("scs-core", "main", "commit-101", toolInfo, ruleInfo, Severity.HIGH, location, "{\"raw\":2}");

        // Then
        assertThat(f1).isEqualTo(f2);
        assertThat(f1.hashCode()).isEqualTo(f2.hashCode());
        assertThat(f1).isNotEqualTo(f3);
        assertThat(f1.equals(null)).isFalse();
        assertThat(f1.equals(f1)).isTrue();

        assertThat(f1.getCategory()).isEqualTo(FindingCategory.CODE_SMELL);
        assertThat(f1.getRawDetails()).isEqualTo("{\"raw\":1}");
        assertThat(f1.getBranch()).isEqualTo("main");
        assertThat(f1.getToolInfo()).isEqualTo(toolInfo);
    }
}
