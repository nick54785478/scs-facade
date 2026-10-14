package com.example.scsfacade.application.service;

import com.example.scsfacade.application.command.UploadScanReportCommand;
import com.example.scsfacade.application.dto.ScanReportUploadedResult;
import com.example.scsfacade.application.port.out.DistributedLockPort;
import com.example.scsfacade.application.port.out.FindingEventPublisherPort;
import com.example.scsfacade.application.port.out.FindingRepositoryPort;
import com.example.scsfacade.application.port.out.ScanExecutionRepositoryPort;
import com.example.scsfacade.application.port.out.ScanReportParserPort;
import com.example.scsfacade.application.port.out.ScanReportParserPort.ParsedFindingItem;
import com.example.scsfacade.application.port.out.ScanReportParserPort.ParsedScanReport;
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

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 掃描報告上傳命令服務單元測試 (UploadScanReportCommandServiceTest)。
 */
@ExtendWith(MockitoExtension.class)
class UploadScanReportCommandServiceTest {

    @Mock
    private ScanReportParserPort scanReportParserPort;

    @Mock
    private ScanExecutionRepositoryPort scanExecutionRepositoryPort;

    @Mock
    private FindingRepositoryPort findingRepositoryPort;

    @Mock
    private FindingEventPublisherPort findingEventPublisherPort;

    @Mock
    private DistributedLockPort distributedLockPort;

    private UploadScanReportCommandService service;

    private final ScannerToolInfo trivyTool = new ScannerToolInfo("Trivy", "0.49.0", "SCA");

    @BeforeEach
    void setUp() {
        service = new UploadScanReportCommandService(
                scanReportParserPort,
                scanExecutionRepositoryPort,
                findingRepositoryPort,
                findingEventPublisherPort,
                distributedLockPort
        );
    }

    /**
     * Feature: 掃描報告上傳成功處理
     * Scenario: 首次上傳 Trivy 報告，結算全數為新弱點並判定門禁
     * Given: 包含 1 個 High 弱點之 SARIF 報告
     * When: 呼叫 service.execute()
     * Then: 成功建立 ScanExecution 與 Finding，門禁合格 (Critical=0, High=1 <= 2)
     */
    @Test
    @DisplayName("報告上傳成功 - 首次上傳應建立新弱點並通過品質門禁")
    void shouldUploadReportSuccessfullyAndPassQualityGate() {
        // Given
        byte[] content = "{}".getBytes(StandardCharsets.UTF_8);
        UploadScanReportCommand command = new UploadScanReportCommand(
                "proj-1", "main", "commit-101", "Trivy", content, "trivy.sarif", "task-101"
        );

        ParsedFindingItem item = new ParsedFindingItem(
                new RuleInfo("CVE-2023-0001", "Vuln 1", List.of("CWE-1"), "CVE-2023-0001", List.of()),
                FindingCategory.SCA,
                Severity.HIGH,
                new FindingLocation("pom.xml", 10, 15, "code"),
                "{}"
        );
        ParsedScanReport parsedReport = new ParsedScanReport(trivyTool, List.of(item));

        when(scanExecutionRepositoryPort.findByExternalTaskId("task-101")).thenReturn(Optional.empty());
        when(distributedLockPort.tryLock(anyString(), anyLong())).thenReturn(true);
        when(scanReportParserPort.parse(content, "Trivy")).thenReturn(parsedReport);
        when(findingRepositoryPort.findActiveByProjectAndBranch("proj-1", "main")).thenReturn(Collections.emptyList());
        when(scanExecutionRepositoryPort.save(any(ScanExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ScanReportUploadedResult result = service.execute(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.projectId()).isEqualTo("proj-1");
        assertThat(result.branch()).isEqualTo("main");
        assertThat(result.toolName()).isEqualTo("Trivy");
        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.passedQualityGate()).isTrue();
        assertThat(result.totalActiveCount()).isEqualTo(1);
        assertThat(result.newCount()).isEqualTo(1);
        assertThat(result.resolvedCount()).isZero();
        assertThat(result.highCount()).isEqualTo(1);

        verify(findingRepositoryPort).saveAll(any());
        verify(findingEventPublisherPort).publishAll(any());
        verify(distributedLockPort).releaseLock(anyString());
    }

    /**
     * Feature: 弱點生命週期跨輪次對齊
     * Scenario: 既有弱點被刷新，已修復弱點被標記 RESOLVED，且不同工具之弱點不受影響
     * Given: DB 中有 1 個 Trivy 既有弱點與 1 個 Gitleaks 弱點，本次報告持續偵測到該 Trivy 弱點
     * When: 執行上傳
     * Then: 既有 Trivy 弱點被 touch，不同工具弱點未被 resolve
     */
    @Test
    @DisplayName("生命週期對齊 - 應精準對齊同工具既有弱點且不干擾其他工具")
    void shouldAlignLifecycleCorrectly() {
        // Given
        byte[] content = "{}".getBytes(StandardCharsets.UTF_8);
        UploadScanReportCommand command = new UploadScanReportCommand(
                "proj-1", "main", "commit-102", "Trivy", content, "trivy.sarif"
        );

        FindingLocation loc = new FindingLocation("pom.xml", 10, 15, "code");
        RuleInfo rule = new RuleInfo("CVE-2023-0001", "Vuln 1", List.of(), "CVE-2023-0001", List.of());

        // 庫存 Trivy 弱點
        Finding existingTrivyFinding = Finding.createNew(
                "proj-1", "main", "commit-101", trivyTool, FindingCategory.SCA, rule, Severity.HIGH, loc, "{}"
        );
        // 庫存 Gitleaks 弱點 (不同工具)
        ScannerToolInfo gitleaksTool = new ScannerToolInfo("gitleaks", "8.0", "SECRET");
        Finding existingGitleaksFinding = Finding.createNew(
                "proj-1", "main", "commit-101", gitleaksTool, FindingCategory.SECRET_LEAK,
                new RuleInfo("key", "Secret", List.of(), null, List.of()),
                Severity.CRITICAL, new FindingLocation("app.yml", 1, 1, "k"), "{}"
        );

        ParsedFindingItem incomingItem = new ParsedFindingItem(
                rule, FindingCategory.SCA, Severity.HIGH, loc, "{}"
        );
        ParsedScanReport parsedReport = new ParsedScanReport(trivyTool, List.of(incomingItem));

        when(distributedLockPort.tryLock(anyString(), anyLong())).thenReturn(true);
        when(scanReportParserPort.parse(content, "Trivy")).thenReturn(parsedReport);
        when(findingRepositoryPort.findActiveByProjectAndBranch("proj-1", "main"))
                .thenReturn(List.of(existingTrivyFinding, existingGitleaksFinding));
        when(scanExecutionRepositoryPort.save(any(ScanExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ScanReportUploadedResult result = service.execute(command);

        // Then
        assertThat(result.totalActiveCount()).isEqualTo(1);
        assertThat(result.existingCount()).isEqualTo(1);
        assertThat(result.newCount()).isZero();
        assertThat(result.resolvedCount()).isZero();

        // 驗證 Gitleaks 弱點未被標記為 RESOLVED
        assertThat(existingGitleaksFinding.getStatus()).isEqualTo(FindingStatus.OPEN);
        assertThat(existingTrivyFinding.getLifecycle().getLastSeenCommit()).isEqualTo("commit-102");
    }

    /**
     * Feature: 弱點消失標記為已修復
     * Scenario: 庫內有 Trivy 弱點，但本次報告中已無該弱點
     * Given: 庫內 1 個 Trivy 弱點，本次報告 items 為空
     * When: 執行上傳
     * Then: 該 Trivy 弱點被標記為 RESOLVED，resolvedCount = 1
     */
    @Test
    @DisplayName("弱點修復 - 本次未再偵測到之同工具弱點應標記為 RESOLVED")
    void shouldMarkDisappearedFindingsAsResolved() {
        // Given
        byte[] content = "{}".getBytes(StandardCharsets.UTF_8);
        UploadScanReportCommand command = new UploadScanReportCommand(
                "proj-1", "main", "commit-103", "Trivy", content, "trivy.sarif"
        );

        Finding existingTrivyFinding = Finding.createNew(
                "proj-1", "main", "commit-101", trivyTool, FindingCategory.SCA,
                new RuleInfo("CVE-2023-0001", "Vuln 1", List.of(), null, List.of()),
                Severity.HIGH, new FindingLocation("pom.xml", 10, 15, "code"), "{}"
        );

        ParsedScanReport emptyReport = new ParsedScanReport(trivyTool, Collections.emptyList());

        when(distributedLockPort.tryLock(anyString(), anyLong())).thenReturn(true);
        when(scanReportParserPort.parse(content, "Trivy")).thenReturn(emptyReport);
        when(findingRepositoryPort.findActiveByProjectAndBranch("proj-1", "main"))
                .thenReturn(List.of(existingTrivyFinding));
        when(scanExecutionRepositoryPort.save(any(ScanExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ScanReportUploadedResult result = service.execute(command);

        // Then
        assertThat(result.resolvedCount()).isEqualTo(1);
        assertThat(result.totalActiveCount()).isZero();
        assertThat(existingTrivyFinding.getStatus()).isEqualTo(FindingStatus.RESOLVED);
    }

    /**
     * Feature: 外部任務等冪性防重放
     * Scenario: 傳入之 externalTaskId 在資料庫中已成功完成
     * Given: 相同 externalTaskId 且狀態為 SUCCESS 之 ScanExecution
     * When: 再次執行 execute()
     * Then: 直接回傳歷史結算結果，不重複解析或發布事件
     */
    @Test
    @DisplayName("等冪防重放 - 已完成之外部任務應直接回傳歷史結果")
    void shouldReturnFastWhenExternalTaskIdAlreadyCompleted() {
        // Given
        byte[] content = "{}".getBytes(StandardCharsets.UTF_8);
        UploadScanReportCommand command = new UploadScanReportCommand(
                "proj-1", "main", "commit-101", "Trivy", content, "trivy.sarif", "task-completed-999"
        );

        ScanExecution completedExecution = ScanExecution.start("proj-1", "main", "commit-101", trivyTool, ScanTriggerType.UPLOAD, "task-completed-999");
        completedExecution.complete(new ScanSummary(5, 1, 4, 0, 0, 1, true));

        when(scanExecutionRepositoryPort.findByExternalTaskId("task-completed-999"))
                .thenReturn(Optional.of(completedExecution));

        // When
        ScanReportUploadedResult result = service.execute(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.scanExecutionId()).isEqualTo(completedExecution.getId().getValue());
        assertThat(result.status()).isEqualTo("SUCCESS");

        verify(distributedLockPort, never()).tryLock(anyString(), anyLong());
        verify(scanReportParserPort, never()).parse(any(), any());
    }

    /**
     * Feature: 分散式鎖衝突阻擋
     * Scenario: 相同任務正在處理中未能取得分散式鎖
     * Given: distributedLockPort.tryLock 回傳 false
     * When: 執行 execute()
     * Then: 拋出 IllegalStateException
     */
    @Test
    @DisplayName("分散式鎖衝突 - 無法取得鎖時應拋出 IllegalStateException")
    void shouldThrowExceptionWhenLockAcquisitionFails() {
        // Given
        byte[] content = "{}".getBytes(StandardCharsets.UTF_8);
        UploadScanReportCommand command = new UploadScanReportCommand(
                "proj-1", "main", "commit-101", "Trivy", content, "trivy.sarif"
        );

        when(distributedLockPort.tryLock(anyString(), anyLong())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("請勿重複提交");
    }
}
