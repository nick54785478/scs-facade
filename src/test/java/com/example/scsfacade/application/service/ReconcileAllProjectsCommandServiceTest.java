package com.example.scsfacade.application.service;

import com.example.scsfacade.application.command.ReconcileAllProjectsCommand;
import com.example.scsfacade.application.command.ReconcileProjectScanCommand;
import com.example.scsfacade.application.dto.ScanReconciliationGottenResult;
import com.example.scsfacade.application.port.in.ReconcileProjectScanUseCase;
import com.example.scsfacade.application.port.out.DistributedLockPort;
import com.example.scsfacade.application.port.out.ProjectDiscoveryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 全專案低頻批次對帳命令服務單元測試 (ReconcileAllProjectsCommandServiceTest)。
 */
@ExtendWith(MockitoExtension.class)
class ReconcileAllProjectsCommandServiceTest {

    @Mock
    private ProjectDiscoveryPort projectDiscoveryPort;

    @Mock
    private ReconcileProjectScanUseCase reconcileProjectScanUseCase;

    @Mock
    private DistributedLockPort distributedLockPort;

    private ReconcileAllProjectsCommandService service;

    @BeforeEach
    void setUp() {
        service = new ReconcileAllProjectsCommandService(
                projectDiscoveryPort,
                reconcileProjectScanUseCase,
                distributedLockPort
        );
    }

    private ScanReconciliationGottenResult createDummyResult(String projectId) {
        return new ScanReconciliationGottenResult(
                projectId, "main", "SonarQube", 5, 5, 0, 0, 0, true, Instant.now()
        );
    }

    /**
     * Feature: 全專案低頻批次對帳
     * Scenario: 探索到多個納管專案，逐一執行個別專案對帳並彙總回傳
     * Given: 探索到 ["project-a", "project-b"] 且成功獲取全域排程鎖
     * When: 執行 ReconcileAllProjectsCommand
     * Then: 依序呼叫單一專案對帳用例，回傳包含 2 筆結果之清單，並正確釋放全域鎖
     */
    @Test
    @DisplayName("全專案批次對帳 - 應探索所有納管專案並逐一完成對帳")
    void shouldSuccessfullyReconcileAllProjects() {
        // Given
        ReconcileAllProjectsCommand command = new ReconcileAllProjectsCommand(false);
        when(distributedLockPort.tryLock(anyString(), anyLong())).thenReturn(true);
        when(projectDiscoveryPort.discoverConfiguredProjectIds()).thenReturn(List.of("project-a", "project-b"));
        when(reconcileProjectScanUseCase.execute(any(ReconcileProjectScanCommand.class)))
                .thenReturn(createDummyResult("project-a"))
                .thenReturn(createDummyResult("project-b"));

        // When
        List<ScanReconciliationGottenResult> results = service.execute(command);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).projectId()).isEqualTo("project-a");
        assertThat(results.get(1).projectId()).isEqualTo("project-b");

        verify(reconcileProjectScanUseCase, times(2)).execute(any(ReconcileProjectScanCommand.class));
        verify(distributedLockPort).releaseLock("lock:reconciliation:global");
    }

    /**
     * Feature: 全專案低頻批次對帳 - 全域鎖防併發
     * Scenario: 其他節點或排程正在執行全域對帳
     * Given: distributedLockPort.tryLock(...) 回傳 false
     * When: 執行 ReconcileAllProjectsCommand
     * Then: 自動略過並回傳空清單，不呼叫任何下游專案對帳
     */
    @Test
    @DisplayName("全域鎖防併發 - 獲取鎖失敗時應自動略過本次批次對帳並回傳空清單")
    void shouldSkipWhenGlobalLockCannotBeAcquired() {
        // Given
        ReconcileAllProjectsCommand command = new ReconcileAllProjectsCommand();
        when(distributedLockPort.tryLock(anyString(), anyLong())).thenReturn(false);

        // When
        List<ScanReconciliationGottenResult> results = service.execute(command);

        // Then
        assertThat(results).isEmpty();
        verify(projectDiscoveryPort, never()).discoverConfiguredProjectIds();
        verify(reconcileProjectScanUseCase, never()).execute(any());
        verify(distributedLockPort, never()).releaseLock(anyString());
    }

    /**
     * Feature: 全專案低頻批次對帳 - 無納管專案處理
     * Scenario: 組態清單中無任何納管專案代碼
     * Given: projectDiscoveryPort 回傳空清單
     * When: 執行批次對帳
     * Then: 回傳空清單並安全釋放鎖
     */
    @Test
    @DisplayName("無納管專案 - 專案清單為空時應安全結束並釋放全域鎖")
    void shouldHandleEmptyConfiguredProjects() {
        // Given
        ReconcileAllProjectsCommand command = new ReconcileAllProjectsCommand();
        when(distributedLockPort.tryLock(anyString(), anyLong())).thenReturn(true);
        when(projectDiscoveryPort.discoverConfiguredProjectIds()).thenReturn(Collections.emptyList());

        // When
        List<ScanReconciliationGottenResult> results = service.execute(command);

        // Then
        assertThat(results).isEmpty();
        verify(reconcileProjectScanUseCase, never()).execute(any());
        verify(distributedLockPort).releaseLock("lock:reconciliation:global");
    }

    /**
     * Feature: 全專案低頻批次對帳 - 容錯隔離
     * Scenario: 其中一個專案對帳時拋出異常，其餘專案不受影響
     * Given: project-a 拋出例外，project-b 正常執行完成
     * When: 執行批次對帳
     * Then: 不中斷整體流程，回傳成功之 project-b 結果 (size = 1)
     */
    @Test
    @DisplayName("容錯隔離 - 單一專案異常不應影響後續其他專案之對帳流程")
    void shouldContinueRemainingProjectsWhenOneFails() {
        // Given
        ReconcileAllProjectsCommand command = new ReconcileAllProjectsCommand();
        when(distributedLockPort.tryLock(anyString(), anyLong())).thenReturn(true);
        when(projectDiscoveryPort.discoverConfiguredProjectIds()).thenReturn(List.of("project-err", "project-ok"));
        when(reconcileProjectScanUseCase.execute(any(ReconcileProjectScanCommand.class)))
                .thenThrow(new RuntimeException("SonarQube API Timeout"))
                .thenReturn(createDummyResult("project-ok"));

        // When
        List<ScanReconciliationGottenResult> results = service.execute(command);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).projectId()).isEqualTo("project-ok");
        verify(distributedLockPort).releaseLock("lock:reconciliation:global");
    }
}
