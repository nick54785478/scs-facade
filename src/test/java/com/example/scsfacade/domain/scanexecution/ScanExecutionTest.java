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
}
