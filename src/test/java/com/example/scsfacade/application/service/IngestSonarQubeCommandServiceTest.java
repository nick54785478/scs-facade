package com.example.scsfacade.application.service;

import com.example.scsfacade.application.assembler.FindingDtoAssembler;
import com.example.scsfacade.application.assembler.ScanExecutionDtoAssembler;
import com.example.scsfacade.application.command.IngestSonarQubeCommand;
import com.example.scsfacade.application.dto.ScanExecutionGottenResult;
import com.example.scsfacade.application.port.out.DistributedLockPort;
import com.example.scsfacade.application.port.out.FindingEventPublisherPort;
import com.example.scsfacade.application.port.out.FindingRepositoryPort;
import com.example.scsfacade.application.port.out.ScanExecutionRepositoryPort;
import com.example.scsfacade.application.port.out.SonarQubeScannerPort;
import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingCategory;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingLocation;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingStatus;
import com.example.scsfacade.domain.finding.aggregate.vo.RuleInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import com.example.scsfacade.domain.scanexecution.aggregate.root.ScanExecution;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionStatus;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanSummary;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanTriggerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 匯入 SonarQube 掃描結果使用案例測試 (IngestSonarQubeCommandServiceTest)。
 */
@ExtendWith(MockitoExtension.class)
class IngestSonarQubeCommandServiceTest {

    @Mock
    private SonarQubeScannerPort sonarQubeScannerPort;

    @Mock
    private FindingRepositoryPort findingRepositoryPort;

    @Mock
    private ScanExecutionRepositoryPort scanExecutionRepositoryPort;

    @Mock
    private FindingEventPublisherPort findingEventPublisherPort;

    @Mock
    private DistributedLockPort distributedLockPort;

    private FindingDtoAssembler findingDtoAssembler;
    private ScanExecutionDtoAssembler scanExecutionDtoAssembler;
    private IngestSonarQubeCommandService ingestSonarQubeCommandService;

    @BeforeEach
    void setUp() {
        findingDtoAssembler = new FindingDtoAssembler();
        scanExecutionDtoAssembler = new ScanExecutionDtoAssembler();
        ingestSonarQubeCommandService = new IngestSonarQubeCommandService(
                sonarQubeScannerPort,
                findingRepositoryPort,
                scanExecutionRepositoryPort,
                findingEventPublisherPort,
                distributedLockPort,
                findingDtoAssembler,
                scanExecutionDtoAssembler
        );
    }

    /**
     * Feature: SonarQube 弱點報告匯入與 CI/CD 快照
     * Scenario: 成功匯入全新弱點並記錄 ScanExecution
     * Given: SonarQube 回傳一筆全新的 SQL Injection 弱點 (Critical 等級)，資料庫目前無任何活躍弱點
     * When: 執行 IngestSonarQubeCommand 匯入命令
     * Then: 成功儲存弱點與 ScanExecution，newCount 為 1，發布領域事件並回傳 ScanExecutionGottenResult
     */
    @Test
    @DisplayName("匯入新弱點 - 應正確儲存並發布領域事件與掃描歷程")
    void shouldSuccessfullyIngestNewFinding() {
        // Given
        String projectId = "test-project";
        IngestSonarQubeCommand command = new IngestSonarQubeCommand(projectId, "main", "commit-123");

        Finding newFinding = Finding.createNew(
                projectId,
                "main",
                "commit-123",
                new ScannerToolInfo("SonarQube", "10.0", "SAST"),
                FindingCategory.VULNERABILITY,
                new RuleInfo("java:S2077", "SQL Injection", List.of("CWE-89"), null, List.of("sql")),
                Severity.CRITICAL,
                new FindingLocation("src/OrderService.java", 42, 45, "SELECT * FROM orders"),
                "{}"
        );

        when(sonarQubeScannerPort.fetchProjectIssues(projectId, "main", "commit-123"))
                .thenReturn(List.of(newFinding));
        when(findingRepositoryPort.findActiveByProjectAndBranch(projectId, "main"))
                .thenReturn(Collections.emptyList());
        when(scanExecutionRepositoryPort.save(any(ScanExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(distributedLockPort.tryLock(any(), anyLong())).thenReturn(true);

        // When
        ScanExecutionGottenResult result = ingestSonarQubeCommandService.execute(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.projectId()).isEqualTo(projectId);
        assertThat(result.branch()).isEqualTo("main");
        assertThat(result.commitHash()).isEqualTo("commit-123");
        assertThat(result.status()).isEqualTo(ScanExecutionStatus.SUCCESS.name());
        assertThat(result.newCount()).isEqualTo(1);
        assertThat(result.existingCount()).isEqualTo(0);
        assertThat(result.resolvedCount()).isEqualTo(0);
        assertThat(result.criticalCount()).isEqualTo(1);
        assertThat(result.passedQualityGate()).isFalse(); // 1 Critical 應未通過品質門檻

        verify(findingRepositoryPort).saveAll(any());
        verify(scanExecutionRepositoryPort).save(any(ScanExecution.class));
        verify(findingEventPublisherPort).publishAll(any());
    }

    /**
     * Feature: SonarQube 弱點報告匯入
     * Scenario: 匯入已存在之弱點時能依指紋正確去重並更新 lastSeenCommit
     * Given: 儲存庫中已存在相同指紋之弱點記錄 (原本 commit-old)
     * When: 在 commit-456 再次執行掃描匯入命令
     * Then: existingCount 為 1，該弱點之 lastSeenCommit 更新為 commit-456
     */
    @Test
    @DisplayName("匯入既有弱點 - 應依指紋比對並更新最新 Commit")
    void shouldDeduplicateAndTouchExistingFinding() {
        // Given
        String projectId = "test-project";
        IngestSonarQubeCommand command = new IngestSonarQubeCommand(projectId, "main", "commit-456");

        Finding existingFinding = Finding.createNew(
                projectId,
                "main",
                "commit-old",
                new ScannerToolInfo("SonarQube", "10.0", "SAST"),
                FindingCategory.VULNERABILITY,
                new RuleInfo("java:S2077", "SQL Injection", List.of("CWE-89"), null, List.of("sql")),
                Severity.HIGH,
                new FindingLocation("src/OrderService.java", 42, 45, "SELECT * FROM orders"),
                "{}"
        );

        when(sonarQubeScannerPort.fetchProjectIssues(projectId, "main", "commit-456"))
                .thenReturn(List.of(existingFinding));
        when(findingRepositoryPort.findActiveByProjectAndBranch(projectId, "main"))
                .thenReturn(List.of(existingFinding));
        when(scanExecutionRepositoryPort.save(any(ScanExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(distributedLockPort.tryLock(any(), anyLong())).thenReturn(true);

        // When
        ScanExecutionGottenResult result = ingestSonarQubeCommandService.execute(command);

        // Then
        assertThat(result.existingCount()).isEqualTo(1);
        assertThat(result.newCount()).isEqualTo(0);
        assertThat(existingFinding.getLastSeenCommit()).isEqualTo("commit-456");
        assertThat(existingFinding.getFirstSeenCommit()).isEqualTo("commit-old");
        verify(findingRepositoryPort).saveAll(any());
    }

    /**
     * Feature: 弱點生命週期自動對齊 (Reconciliation)
     * Scenario: 程式碼修復後，消失的弱點自動標記為 RESOLVED
     * Given: 儲存庫存在一筆 OPEN 弱點，但本次 SonarQube 掃描回傳空清單（工程師已修復）
     * When: 執行匯入命令
     * Then: 該弱點自動標記為 RESOLVED，resolvedInCommit 為最新 commit，resolvedCount 為 1
     */
    @Test
    @DisplayName("自動對齊修復 - 本次未回報之弱點應自動標記為 RESOLVED")
    void shouldAutomaticallyReconcileAndResolveMissingFinding() {
        // Given
        String projectId = "test-project";
        IngestSonarQubeCommand command = new IngestSonarQubeCommand(projectId, "main", "commit-fix");

        Finding activeFinding = Finding.createNew(
                projectId,
                "main",
                "commit-bug",
                new ScannerToolInfo("SonarQube", "10.0", "SAST"),
                FindingCategory.VULNERABILITY,
                new RuleInfo("java:S2077", "SQL Injection", List.of("CWE-89"), null, List.of("sql")),
                Severity.HIGH,
                new FindingLocation("src/OrderService.java", 42, 45, "SELECT * FROM orders"),
                "{}"
        );

        when(sonarQubeScannerPort.fetchProjectIssues(projectId, "main", "commit-fix"))
                .thenReturn(Collections.emptyList()); // 本次掃描已無問題
        when(findingRepositoryPort.findActiveByProjectAndBranch(projectId, "main"))
                .thenReturn(List.of(activeFinding));
        when(scanExecutionRepositoryPort.save(any(ScanExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(distributedLockPort.tryLock(any(), anyLong())).thenReturn(true);

        // When
        ScanExecutionGottenResult result = ingestSonarQubeCommandService.execute(command);

        // Then
        assertThat(result.resolvedCount()).isEqualTo(1);
        assertThat(result.totalActiveCount()).isEqualTo(0);
        assertThat(activeFinding.getStatus()).isEqualTo(FindingStatus.RESOLVED);
        assertThat(activeFinding.getResolvedInCommit()).isEqualTo("commit-fix");
        verify(findingRepositoryPort).saveAll(any());
    }

    /**
     * Feature: Webhook 冪等性防重校驗 (Fast Return)
     * Scenario: 接收相同 externalTaskId 的重複 Webhook 請求時，直接回傳已成功的 ScanExecution，不重複向 SonarQube 拉取與儲存
     * Given: 倉儲中已存在相同 taskId 且狀態為 SUCCESS 的 ScanExecution
     * When: 再次發送相同 taskId 的 IngestSonarQubeCommand
     * Then: 快速回傳該結果，且不呼叫 sonarQubeScannerPort.fetchProjectIssues，不重複儲存 scanExecution
     */
    @Test
    @DisplayName("冪等性防重 - 相同外部任務已完成時應 Fast Return 歷史結果，不重複拉取與建檔")
    void shouldReturnExistingResultWhenExternalTaskIdAlreadyCompleted() {
        // Given
        String projectId = "test-project";
        String taskId = "task-aw70-12345";
        IngestSonarQubeCommand command = new IngestSonarQubeCommand(
                projectId, "main", "commit-123", ScanTriggerType.WEBHOOK, taskId
        );

        ScanExecution existingExecution = ScanExecution.start(
                projectId, "main", "commit-123",
                new ScannerToolInfo("SonarQube", "10.0", "SAST"),
                ScanTriggerType.WEBHOOK,
                taskId
        );
        existingExecution.complete(new ScanSummary(1, 1, 0, 0, 0, 0, true));

        when(scanExecutionRepositoryPort.findByExternalTaskId(taskId))
                .thenReturn(Optional.of(existingExecution));
        when(findingRepositoryPort.findActiveByProjectAndBranch(projectId, "main"))
                .thenReturn(Collections.emptyList());

        // When
        ScanExecutionGottenResult result = ingestSonarQubeCommandService.execute(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.projectId()).isEqualTo(projectId);
        assertThat(result.externalTaskId()).isEqualTo(taskId);
        assertThat(result.status()).isEqualTo(ScanExecutionStatus.SUCCESS.name());

        // 驗證核心防禦：絕不重新調用 SonarQube 埠，亦不重複發布事件或保存 ScanExecution
        verify(sonarQubeScannerPort, never()).fetchProjectIssues(any(), any(), any());
        verify(scanExecutionRepositoryPort, never()).save(any());
        verify(findingEventPublisherPort, never()).publishAll(any());
    }

    /**
     * Feature: 分散式鎖並發防禦
     * Scenario: 當相同任務正在處理中或鎖被佔用時，拒絕並發執行以防重複建檔
     * Given: distributedLockPort.tryLock 回傳 false
     * When: 發起 IngestSonarQubeCommand
     * Then: 拋出 IllegalStateException
     */
    @Test
    @DisplayName("分散式鎖防禦 - 鎖被佔用時應拋出例外防止並發衝突")
    void shouldRejectConcurrentExecutionWhenLockCannotBeAcquired() {
        // Given
        String projectId = "test-project";
        IngestSonarQubeCommand command = new IngestSonarQubeCommand(projectId, "main", "commit-123");

        when(distributedLockPort.tryLock(any(), anyLong())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> ingestSonarQubeCommandService.execute(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("相同掃描任務正在處理中或並發衝突");

        verify(scanExecutionRepositoryPort, never()).save(any());
    }
}
