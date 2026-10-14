package com.example.scsfacade.application.service;

import com.example.scsfacade.application.assembler.ScanExecutionDtoAssembler;
import com.example.scsfacade.application.dto.ScanExecutionGottenResult;
import com.example.scsfacade.application.port.out.ScanExecutionRepositoryPort;
import com.example.scsfacade.application.query.GetScanExecutionQuery;
import com.example.scsfacade.application.query.ListScanExecutionsQuery;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.scanexecution.aggregate.root.ScanExecution;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionId;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanTriggerType;
import com.example.scsfacade.domain.scanexecution.exception.ScanExecutionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 掃描執行查詢應用服務測試 (ScanExecutionQueryServiceTest)。
 * <p>
 * 遵循 AGENTS.md 測試規範，使用 Gherkin 語法撰寫 BDD 測試案例，覆蓋正常流程與例外分支。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ScanExecutionQueryServiceTest {

    @Mock
    private ScanExecutionRepositoryPort scanExecutionRepositoryPort;

    private ScanExecutionDtoAssembler scanExecutionDtoAssembler;
    private ScanExecutionQueryService scanExecutionQueryService;

    @BeforeEach
    void setUp() {
        scanExecutionDtoAssembler = new ScanExecutionDtoAssembler();
        scanExecutionQueryService = new ScanExecutionQueryService(scanExecutionRepositoryPort, scanExecutionDtoAssembler);
    }

    /**
     * Feature: 掃描執行紀錄查詢
     * Scenario: 依據 ID 成功查詢掃描執行詳細資訊
     * Given: 儲存庫存在指定 ID 之掃描紀錄
     * When: 執行 GetScanExecutionQuery
     * Then: 回傳 ScanExecutionGottenResult 且屬性相符
     */
    @Test
    @DisplayName("依 ID 查詢 - 紀錄存在時應正確回傳結果")
    void shouldReturnScanExecutionWhenFound() {
        // Given
        ScannerToolInfo toolInfo = new ScannerToolInfo("SonarQube", "10.0", "SAST");
        ScanExecution execution = ScanExecution.start("proj-1", "main", "commit-abc", toolInfo, ScanTriggerType.PIPELINE);
        ScanExecutionId executionId = execution.getId();

        when(scanExecutionRepositoryPort.findById(executionId)).thenReturn(Optional.of(execution));

        // When
        GetScanExecutionQuery query = new GetScanExecutionQuery(executionId.getValue());
        ScanExecutionGottenResult result = scanExecutionQueryService.execute(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.executionId()).isEqualTo(executionId.getValue());
        assertThat(result.projectId()).isEqualTo("proj-1");
        assertThat(result.branch()).isEqualTo("main");
        assertThat(result.commitHash()).isEqualTo("commit-abc");
    }

    /**
     * Feature: 掃描執行紀錄查詢例外
     * Scenario: 查詢不存在之掃描紀錄 ID
     * Given: 儲存庫查無該 ID
     * When: 執行 GetScanExecutionQuery
     * Then: 拋出 ScanExecutionNotFoundException
     */
    @Test
    @DisplayName("依 ID 查詢 - 查無紀錄時應拋出 ScanExecutionNotFoundException")
    void shouldThrowExceptionWhenNotFound() {
        // Given
        String nonExistentId = "non-existent-id";
        when(scanExecutionRepositoryPort.findById(ScanExecutionId.of(nonExistentId))).thenReturn(Optional.empty());

        // When & Then
        GetScanExecutionQuery query = new GetScanExecutionQuery(nonExistentId);
        assertThatThrownBy(() -> scanExecutionQueryService.execute(query))
                .isInstanceOf(ScanExecutionNotFoundException.class)
                .hasMessageContaining(nonExistentId);
    }

    /**
     * Feature: 依專案與分支查詢掃描清單
     * Scenario: 提供專案與分支名稱進行篩選
     * Given: 儲存庫存在符合條件之掃描執行紀錄
     * When: 執行 ListScanExecutionsQuery (包含 branch)
     * Then: 呼叫 findByProjectIdAndBranch 並回傳轉換後之 DTO 清單
     */
    @Test
    @DisplayName("依專案與分支查詢 - 應呼叫 findByProjectIdAndBranch 並回傳清單")
    void shouldListScanExecutionsByProjectAndBranch() {
        // Given
        ScannerToolInfo toolInfo = new ScannerToolInfo("SonarQube", "10.0", "SAST");
        ScanExecution execution = ScanExecution.start("proj-1", "develop", "commit-dev", toolInfo, ScanTriggerType.WEBHOOK);

        when(scanExecutionRepositoryPort.findByProjectIdAndBranch("proj-1", "develop"))
                .thenReturn(List.of(execution));

        // When
        ListScanExecutionsQuery query = new ListScanExecutionsQuery("proj-1", "develop");
        List<ScanExecutionGottenResult> results = scanExecutionQueryService.execute(query);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).projectId()).isEqualTo("proj-1");
        assertThat(results.get(0).branch()).isEqualTo("develop");
        verify(scanExecutionRepositoryPort).findByProjectIdAndBranch("proj-1", "develop");
    }

    /**
     * Feature: 依專案查詢掃描清單
     * Scenario: 未提供分支名稱時查詢專案所有掃描
     * Given: 儲存庫存在該專案所有分支之掃描執行紀錄
     * When: 執行 ListScanExecutionsQuery (branch 為 null)
     * Then: 呼叫 findByProjectId 並回傳轉換後之 DTO 清單
     */
    @Test
    @DisplayName("依專案查詢 - 分支為空時應呼叫 findByProjectId 並回傳清單")
    void shouldListScanExecutionsByProjectWhenBranchNotProvided() {
        // Given
        ScannerToolInfo toolInfo = new ScannerToolInfo("SonarQube", "10.0", "SAST");
        ScanExecution execution = ScanExecution.start("proj-1", "main", "commit-main", toolInfo, ScanTriggerType.PIPELINE);

        when(scanExecutionRepositoryPort.findByProjectId("proj-1"))
                .thenReturn(List.of(execution));

        // When
        ListScanExecutionsQuery query = new ListScanExecutionsQuery("proj-1", null);
        List<ScanExecutionGottenResult> results = scanExecutionQueryService.execute(query);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).projectId()).isEqualTo("proj-1");
        verify(scanExecutionRepositoryPort).findByProjectId("proj-1");
    }
}
