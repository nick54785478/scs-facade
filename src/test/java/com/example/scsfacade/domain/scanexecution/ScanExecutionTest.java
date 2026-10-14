package com.example.scsfacade.domain.scanexecution;

import com.example.scsfacade.domain.event.DomainEvent;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.scanexecution.aggregate.root.ScanExecution;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionId;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionPeriod;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionStatus;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanSummary;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanTriggerType;
import com.example.scsfacade.domain.scanexecution.event.ScanExecutionCompletedEvent;
import com.example.scsfacade.domain.scanexecution.event.ScanExecutionStartedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 掃描執行批次聚合根領域測試 (ScanExecutionTest)。
 */
class ScanExecutionTest {

    /**
     * Feature: 掃描批次啟動
     * Scenario: 建立新掃描並發布 ScanExecutionStartedEvent
     * Given: 專案資訊與觸發來源 PIPELINE
     * When: 呼叫 ScanExecution.start()
     * Then: 狀態為 RUNNING，累積 ScanExecutionStartedEvent
     */
    @Test
    @DisplayName("掃描啟動 - 應正確初始化並發布啟動事件")
    void shouldStartScanExecutionSuccessfully() {
        // Given
        ScannerToolInfo toolInfo = new ScannerToolInfo("SonarQube", "10.0", "SAST");

        // When
        ScanExecution execution = ScanExecution.start("my-proj", "main", "commit-aaa", toolInfo, ScanTriggerType.PIPELINE);

        // Then
        assertThat(execution.getStatus()).isEqualTo(ScanExecutionStatus.RUNNING);
        assertThat(execution.getProjectId()).isEqualTo("my-proj");
        assertThat(execution.getBranch()).isEqualTo("main");
        assertThat(execution.getCommitHash()).isEqualTo("commit-aaa");

        List<DomainEvent> events = execution.pullDomainEvents();
        assertThat(events).hasSize(1);
        DomainEvent startedEvent = events.get(0);
        assertThat(startedEvent).isInstanceOf(ScanExecutionStartedEvent.class);
        assertThat(startedEvent.eventId()).isNotNull();
        assertThat(startedEvent.aggregateType()).isEqualTo("SCAN_EXECUTION");
        assertThat(startedEvent.aggregateId()).isEqualTo(execution.getId().getValue());
        assertThat(startedEvent.occurredAt()).isNotNull();
    }

    /**
     * Feature: 掃描批次完成
     * Scenario: 結算統計指標並發布 ScanExecutionCompletedEvent
     * Given: 處於 RUNNING 狀態的 ScanExecution
     * When: 呼叫 complete(summary)
     * Then: 狀態轉移為 SUCCESS，累積 ScanExecutionCompletedEvent
     */
    @Test
    @DisplayName("掃描完成 - 應正確結算指標並發布完成事件")
    void shouldCompleteScanExecutionSuccessfully() {
        // Given
        ScannerToolInfo toolInfo = new ScannerToolInfo("SonarQube", "10.0", "SAST");
        ScanExecution execution = ScanExecution.start("my-proj", "main", "commit-aaa", toolInfo, ScanTriggerType.WEBHOOK);
        execution.pullDomainEvents(); // 清空啟動事件

        ScanSummary summary = new ScanSummary(10, 2, 8, 1, 0, 1, true);

        // When
        execution.complete(summary);

        // Then
        assertThat(execution.getStatus()).isEqualTo(ScanExecutionStatus.SUCCESS);
        assertThat(execution.getSummary().getNewCount()).isEqualTo(2);
        assertThat(execution.getSummary().getResolvedCount()).isEqualTo(1);
        assertThat(execution.getSummary().isPassedQualityGate()).isTrue();
        assertThat(execution.getCompletedAt()).isNotNull();

        List<DomainEvent> events = execution.pullDomainEvents();
        assertThat(events).hasSize(1);
        DomainEvent completedEvent = events.get(0);
        assertThat(completedEvent).isInstanceOf(ScanExecutionCompletedEvent.class);
        assertThat(completedEvent.eventId()).isNotNull();
        assertThat(completedEvent.aggregateType()).isEqualTo("SCAN_EXECUTION");
        assertThat(completedEvent.aggregateId()).isEqualTo(execution.getId().getValue());
        assertThat(completedEvent.occurredAt()).isNotNull();
    }

    /**
     * Feature: 掃描批次還原
     * Scenario: 成功從持久化資料還原 ScanExecution
     * Given: 完整之持久化欄位與 ScanExecutionPeriod
     * When: 呼叫 ScanExecution.reconstitute()
     * Then: 成功重建聚合根，無任何領域事件累積
     */
    @Test
    @DisplayName("掃描還原 - 應正確從持久化資料重建聚合根")
    void shouldReconstituteScanExecutionSuccessfully() {
        // Given
        ScanExecutionId id = ScanExecutionId.generate();
        ScannerToolInfo toolInfo = new ScannerToolInfo("SonarQube", "10.0", "SAST");
        ScanSummary summary = new ScanSummary(5, 1, 4, 0, 0, 0, true);
        ScanExecutionPeriod period = ScanExecutionPeriod.of(
                Instant.now().minusSeconds(60), Instant.now(), null);

        // When
        ScanExecution execution = ScanExecution.reconstitute(
                id, "my-proj", "main", "commit-aaa",
                toolInfo, ScanTriggerType.PIPELINE,
                ScanExecutionStatus.SUCCESS, summary, period
        );

        // Then
        assertThat(execution.getId()).isEqualTo(id);
        assertThat(execution.getStatus()).isEqualTo(ScanExecutionStatus.SUCCESS);
        assertThat(execution.pullDomainEvents()).isEmpty();
    }

    /**
     * Feature: 掃描批次失敗
     * Scenario: 記錄掃描異常原因並更新狀態為 FAILED
     * Given: 執行中之 ScanExecution
     * When: 呼叫 fail(errorMessage)
     * Then: 狀態為 FAILED，記錄錯誤訊息與結束時間
     */
    @Test
    @DisplayName("掃描失敗 - 應正確更新狀態為 FAILED 並記錄錯誤訊息")
    void shouldHandleScanFailureAndRecordErrorMessage() {
        // Given
        ScannerToolInfo toolInfo = new ScannerToolInfo("SonarQube", "10.0", "SAST");
        ScanExecution execution = ScanExecution.start("my-proj", "main", "commit-aaa", toolInfo, ScanTriggerType.PIPELINE);

        // When
        execution.fail("SonarQube 掃描超時或連線失敗");

        // Then
        assertThat(execution.getStatus()).isEqualTo(ScanExecutionStatus.FAILED);
        assertThat(execution.getErrorMessage()).isEqualTo("SonarQube 掃描超時或連線失敗");
        assertThat(execution.getCompletedAt()).isNotNull();
    }

    /**
     * Feature: 外部任務關聯與完成操作等冪保護
     * Scenario: 支援外部任務識別碼，且重複呼叫 complete 不重複累積事件
     * Given: 攜帶 externalTaskId 之 ScanExecution 已完成
     * When: 再次呼叫 complete(summary)
     * Then: 狀態維持 SUCCESS，不重複產生領域事件
     */
    @Test
    @DisplayName("外部任務與完成等冪性 - 已完成狀態下重複呼叫 complete 不應重新結算或重發事件")
    void shouldSupportExternalTaskIdAndIdempotentComplete() {
        // Given
        ScannerToolInfo toolInfo = new ScannerToolInfo("SonarQube", "10.0", "SAST");
        ScanExecution execution = ScanExecution.start(
                "my-proj", "main", "commit-aaa", toolInfo, ScanTriggerType.WEBHOOK, "sonar-task-999"
        );
        assertThat(execution.getExternalTaskId()).isEqualTo("sonar-task-999");

        ScanSummary summary = new ScanSummary(5, 1, 4, 0, 0, 0, true);
        execution.complete(summary);
        execution.pullDomainEvents();

        // When (重複完成)
        execution.complete(new ScanSummary(10, 5, 5, 0, 0, 0, false));

        // Then (不應被覆蓋)
        assertThat(execution.getSummary().getTotalActiveCount()).isEqualTo(5);
        assertThat(execution.pullDomainEvents()).isEmpty();

        // 驗證帶有 externalTaskId 的 reconstitute
        ScanExecution reconstituted = ScanExecution.reconstitute(
                execution.getId(), "my-proj", "main", "commit-aaa",
                toolInfo, ScanTriggerType.WEBHOOK,
                ScanExecutionStatus.SUCCESS, summary, execution.getPeriod(), "sonar-task-999"
        );
        assertThat(reconstituted.getExternalTaskId()).isEqualTo("sonar-task-999");
    }

    /**
     * Feature: 聚合根相等性與屬性查詢
     * Scenario: 驗證 ScanExecution 之 equals, hashCode 與 getters
     * Given: 相同 ID 與不同 ID 之 ScanExecution
     * When: 進行比對
     * Then: 基於 Entity ID 判定相等性
     */
    @Test
    @DisplayName("實體相等性與屬性 - 應基於 ScanExecutionId 判定 equals 與 hashCode")
    void shouldVerifyScanExecutionEqualityAndGetters() {
        // Given
        ScannerToolInfo toolInfo = new ScannerToolInfo("SonarQube", "10.0", "SAST");
        ScanExecution e1 = ScanExecution.start("my-proj", "main", "commit-aaa", toolInfo, ScanTriggerType.PIPELINE);
        ScanExecution e2 = ScanExecution.reconstitute(
                e1.getId(), "my-proj", "main", "commit-aaa", toolInfo,
                ScanTriggerType.PIPELINE, e1.getStatus(), e1.getSummary(), e1.getPeriod()
        );
        ScanExecution e3 = ScanExecution.start("my-proj", "main", "commit-aaa", toolInfo, ScanTriggerType.PIPELINE);

        // Then
        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
        assertThat(e1).isNotEqualTo(e3);
        assertThat(e1.equals("other")).isFalse();
        assertThat(e1.equals(e1)).isTrue();

        assertThat(e1.getToolInfo()).isEqualTo(toolInfo);
        assertThat(e1.getPeriod()).isNotNull();
        assertThat(e1.getStartedAt()).isNotNull();
    }
}
