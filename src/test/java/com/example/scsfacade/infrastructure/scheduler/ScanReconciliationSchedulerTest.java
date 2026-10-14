package com.example.scsfacade.infrastructure.scheduler;

import com.example.scsfacade.application.command.ReconcileAllProjectsCommand;
import com.example.scsfacade.application.port.in.ReconcileAllProjectsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 掃描狀態低頻對帳排程器單元測試 (ScanReconciliationSchedulerTest)。
 */
@ExtendWith(MockitoExtension.class)
class ScanReconciliationSchedulerTest {

    @Mock
    private ReconcileAllProjectsUseCase reconcileAllProjectsUseCase;

    /**
     * Feature: 排程低頻對帳定時觸發
     * Scenario: 當排程啟用 (enabled=true) 時呼叫批次對帳用例
     * Given: enabled 設定為 true
     * When: 觸發 runScheduledReconciliation
     * Then: 正確呼叫 reconcileAllProjectsUseCase.execute
     */
    @Test
    @DisplayName("排程觸發 - 當 enabled=true 時應正常執行全專案批次對帳")
    void shouldTriggerBatchReconciliationWhenEnabled() {
        // Given
        ScanReconciliationScheduler scheduler = new ScanReconciliationScheduler(reconcileAllProjectsUseCase, true);

        // When
        scheduler.runScheduledReconciliation();

        // Then
        verify(reconcileAllProjectsUseCase).execute(any(ReconcileAllProjectsCommand.class));
    }

    /**
     * Feature: 排程開關控制
     * Scenario: 當排程停用 (enabled=false) 時略過定時任務
     * Given: enabled 設定為 false
     * When: 觸發 runScheduledReconciliation
     * Then: 不呼叫 reconcileAllProjectsUseCase
     */
    @Test
    @DisplayName("排程停用 - 當 enabled=false 時應略過定時執行")
    void shouldSkipWhenDisabled() {
        // Given
        ScanReconciliationScheduler scheduler = new ScanReconciliationScheduler(reconcileAllProjectsUseCase, false);

        // When
        scheduler.runScheduledReconciliation();

        // Then
        verify(reconcileAllProjectsUseCase, never()).execute(any());
    }

    /**
     * Feature: 排程異常容錯
     * Scenario: 批次對帳用例拋出未預期異常
     * Given: execute 拋出 RuntimeException
     * When: 觸發 runScheduledReconciliation
     * Then: 捕獲並記錄例外，不向外崩潰拋出
     */
    @Test
    @DisplayName("異常容錯 - 下游執行異常時排程應優雅捕獲不向外拋出")
    void shouldCatchAndLogExceptionsWithoutCrashing() {
        // Given
        ScanReconciliationScheduler scheduler = new ScanReconciliationScheduler(reconcileAllProjectsUseCase, true);
        when(reconcileAllProjectsUseCase.execute(any())).thenThrow(new RuntimeException("Redis connection refused"));

        // When & Then
        assertThatCode(scheduler::runScheduledReconciliation).doesNotThrowAnyException();
    }
}
