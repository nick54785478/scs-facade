package com.example.scsfacade.application.service;

import com.example.scsfacade.application.command.ReconcileProjectScanCommand;
import com.example.scsfacade.application.dto.ScanReconciliationGottenResult;
import com.example.scsfacade.application.port.out.DistributedLockPort;
import com.example.scsfacade.application.port.out.FindingEventPublisherPort;
import com.example.scsfacade.application.port.out.FindingRepositoryPort;
import com.example.scsfacade.application.port.out.SonarQubeScannerPort;
import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingCategory;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingLocation;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingStatus;
import com.example.scsfacade.domain.finding.aggregate.vo.RuleInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 專案低頻對帳命令服務單元測試 (ReconcileProjectScanCommandServiceTest)。
 */
@ExtendWith(MockitoExtension.class)
class ReconcileProjectScanCommandServiceTest {

    @Mock
    private SonarQubeScannerPort sonarQubeScannerPort;

    @Mock
    private FindingRepositoryPort findingRepositoryPort;

    @Mock
    private FindingEventPublisherPort findingEventPublisherPort;

    @Mock
    private DistributedLockPort distributedLockPort;

    private ReconcileProjectScanCommandService service;

    @BeforeEach
    void setUp() {
        service = new ReconcileProjectScanCommandService(
                sonarQubeScannerPort,
                findingRepositoryPort,
                findingEventPublisherPort,
                distributedLockPort
        );
    }

    private Finding createFinding(String projectId, String branch, String toolName, Severity severity, String ruleId, String path, int line) {
        return Finding.createNew(
                projectId,
                branch,
                "commit-123",
                new ScannerToolInfo(toolName, "1.0", "SAST"),
                FindingCategory.VULNERABILITY,
                new RuleInfo(ruleId, "Rule Description", List.of("CWE-89"), null, List.of("security")),
                severity,
                new FindingLocation(path, line, line, "Sample snippet"),
                "{}"
        );
    }

    /**
     * Feature: 專案低頻對帳 - 補償修復狀態 (情境 A)
     * Scenario: 本地存在活躍 SonarQube 弱點，但遠端已無此弱點
     * Given: 本地有 1 筆 OPEN 弱點，遠端 Issues 回傳空清單
     * When: 執行 ReconcileProjectScanCommand 對帳命令
     * Then: 本地弱點被標記為 RESOLVED，reconciledResolvedCount 為 1，發布狀態變更領域事件
     */
    @Test
    @DisplayName("情境 A - 本地存在但遠端已消失之弱點應補償標記為 RESOLVED 並發布事件")
    void shouldResolveDisappearedFindings() {
        // Given
        String projectId = "order-service";
        String branch = "main";
        ReconcileProjectScanCommand command = new ReconcileProjectScanCommand(projectId, branch);

        Finding localFinding = createFinding(projectId, branch, "SonarQube", Severity.HIGH, "rule-1", "src/Order.java", 10);

        when(distributedLockPort.tryLock(anyString(), anyLong())).thenReturn(true);
        when(sonarQubeScannerPort.fetchProjectIssues(projectId, branch, "RECONCILED")).thenReturn(Collections.emptyList());
        when(findingRepositoryPort.findActiveByProjectAndBranch(projectId, branch)).thenReturn(List.of(localFinding));

        // When
        ScanReconciliationGottenResult result = service.execute(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.projectId()).isEqualTo(projectId);
        assertThat(result.branch()).isEqualTo(branch);
        assertThat(result.remoteActiveCount()).isZero();
        assertThat(result.localActiveCount()).isEqualTo(1);
        assertThat(result.reconciledResolvedCount()).isEqualTo(1);
        assertThat(result.reconciledAppendedCount()).isZero();
        assertThat(result.severityAdjustedCount()).isZero();
        assertThat(result.passedQualityGate()).isTrue();
        assertThat(localFinding.getStatus()).isEqualTo(FindingStatus.RESOLVED);

        verify(findingRepositoryPort).saveAll(any());
        verify(findingEventPublisherPort).publishAll(any());
        verify(distributedLockPort).releaseLock("lock:scan:reconcile:order-service:main");
    }

    /**
     * Feature: 專案低頻對帳 - 補錄遺漏弱點 (情境 B)
     * Scenario: 遠端存在弱點，但本地資料庫從無此指紋 (先前 Webhook 傳輸失敗漏接)
     * Given: 遠端有 1 筆 Critical 弱點，本地為空清單
     * When: 執行 ReconcileProjectScanCommand 對帳命令
     * Then: 補入庫，reconciledAppendedCount 為 1，發布弱點匯入事件，且 Quality Gate 判定不合格
     */
    @Test
    @DisplayName("情境 B - 遠端存在但本地從無之弱點應補錄入庫並觸發門禁審查")
    void shouldAppendMissedRemoteFindings() {
        // Given
        String projectId = "payment-service";
        String branch = "main";
        ReconcileProjectScanCommand command = new ReconcileProjectScanCommand(projectId, branch);

        Finding remoteFinding = createFinding(projectId, branch, "SonarQube", Severity.CRITICAL, "rule-crit", "src/Pay.java", 20);

        when(distributedLockPort.tryLock(anyString(), anyLong())).thenReturn(true);
        when(sonarQubeScannerPort.fetchProjectIssues(projectId, branch, "RECONCILED")).thenReturn(List.of(remoteFinding));
        when(findingRepositoryPort.findActiveByProjectAndBranch(projectId, branch)).thenReturn(Collections.emptyList());

        // When
        ScanReconciliationGottenResult result = service.execute(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.remoteActiveCount()).isEqualTo(1);
        assertThat(result.localActiveCount()).isZero();
        assertThat(result.reconciledAppendedCount()).isEqualTo(1);
        assertThat(result.reconciledResolvedCount()).isZero();
        assertThat(result.passedQualityGate()).isFalse(); // 1 筆 Critical 應判定門禁不合格

        verify(findingRepositoryPort).saveAll(any());
        verify(findingEventPublisherPort).publishAll(any());
        verify(distributedLockPort).releaseLock("lock:scan:reconcile:payment-service:main");
    }

    /**
     * Feature: 專案低頻對帳 - 同步嚴重等級異動 (情境 C)
     * Scenario: 雙方皆存在該弱點，但遠端 Severity 在管理介面被調整過
     * Given: 本地弱點為 LOW，遠端同指紋弱點為 HIGH
     * When: 執行 ReconcileProjectScanCommand 對帳命令
     * Then: 本地弱點等級被調整為 HIGH，severityAdjustedCount 為 1
     */
    @Test
    @DisplayName("情境 C - 雙方皆存在但等級不同時應同步調整嚴重性等級")
    void shouldAdjustModifiedSeverity() {
        // Given
        String projectId = "auth-service";
        String branch = "main";
        ReconcileProjectScanCommand command = new ReconcileProjectScanCommand(projectId, branch);

        Finding localFinding = createFinding(projectId, branch, "SonarQube", Severity.LOW, "rule-auth", "src/Token.java", 35);
        Finding remoteFinding = createFinding(projectId, branch, "SonarQube", Severity.HIGH, "rule-auth", "src/Token.java", 35);

        when(distributedLockPort.tryLock(anyString(), anyLong())).thenReturn(true);
        when(sonarQubeScannerPort.fetchProjectIssues(projectId, branch, "RECONCILED")).thenReturn(List.of(remoteFinding));
        when(findingRepositoryPort.findActiveByProjectAndBranch(projectId, branch)).thenReturn(List.of(localFinding));

        // When
        ScanReconciliationGottenResult result = service.execute(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.severityAdjustedCount()).isEqualTo(1);
        assertThat(localFinding.getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(result.passedQualityGate()).isTrue(); // 1 High <= 2, 0 Critical -> 合格

        verify(findingRepositoryPort).saveAll(any());
        verify(distributedLockPort).releaseLock("lock:scan:reconcile:auth-service:main");
    }

    /**
     * Feature: 專案低頻對帳 - 狀態完全吻合 (情境 D)
     * Scenario: 雙方皆存在該弱點且等級一致
     * Given: 本地為 MEDIUM，遠端同指紋亦為 MEDIUM
     * When: 執行對帳
     * Then: 僅更新 touchScan，各差異計數皆為 0
     */
    @Test
    @DisplayName("情境 D - 狀態與等級完全吻合時不產生任何狀態異動")
    void shouldKeepUntouchedWhenBothMatch() {
        // Given
        String projectId = "user-service";
        String branch = "main";
        ReconcileProjectScanCommand command = new ReconcileProjectScanCommand(projectId, branch);

        Finding localFinding = createFinding(projectId, branch, "SonarQube", Severity.MEDIUM, "rule-m", "src/User.java", 5);
        Finding remoteFinding = createFinding(projectId, branch, "SonarQube", Severity.MEDIUM, "rule-m", "src/User.java", 5);

        when(distributedLockPort.tryLock(anyString(), anyLong())).thenReturn(true);
        when(sonarQubeScannerPort.fetchProjectIssues(projectId, branch, "RECONCILED")).thenReturn(List.of(remoteFinding));
        when(findingRepositoryPort.findActiveByProjectAndBranch(projectId, branch)).thenReturn(List.of(localFinding));

        // When
        ScanReconciliationGottenResult result = service.execute(command);

        // Then
        assertThat(result.reconciledResolvedCount()).isZero();
        assertThat(result.reconciledAppendedCount()).isZero();
        assertThat(result.severityAdjustedCount()).isZero();
        assertThat(result.passedQualityGate()).isTrue();
    }

    /**
     * Feature: 專案低頻對帳 - 多工具隔離保護
     * Scenario: 本地有 Trivy 與 SonarQube 弱點，遠端 SonarQube 為空
     * Given: 本地有一筆 Trivy 弱點與一筆 SonarQube 弱點
     * When: 執行對帳
     * Then: 僅 SonarQube 弱點被標記為 RESOLVED，Trivy 弱點不受任何影響
     */
    @Test
    @DisplayName("工具隔離保護 - 僅比對同為 SonarQube 之弱點，絕不誤傷 Trivy 等其他工具資料")
    void shouldIsolateOtherToolsFromReconciliation() {
        // Given
        String projectId = "core-service";
        String branch = "main";
        ReconcileProjectScanCommand command = new ReconcileProjectScanCommand(projectId, branch);

        Finding trivyFinding = createFinding(projectId, branch, "Trivy", Severity.HIGH, "CVE-2023-1111", "pom.xml", 1);
        Finding sonarFinding = createFinding(projectId, branch, "SonarQube", Severity.MEDIUM, "java:S1186", "src/Core.java", 15);

        when(distributedLockPort.tryLock(anyString(), anyLong())).thenReturn(true);
        when(sonarQubeScannerPort.fetchProjectIssues(projectId, branch, "RECONCILED")).thenReturn(Collections.emptyList());
        when(findingRepositoryPort.findActiveByProjectAndBranch(projectId, branch)).thenReturn(List.of(trivyFinding, sonarFinding));

        // When
        ScanReconciliationGottenResult result = service.execute(command);

        // Then
        assertThat(result.localActiveCount()).isEqualTo(1); // 僅計算 SonarQube 的 1 筆
        assertThat(result.reconciledResolvedCount()).isEqualTo(1);
        assertThat(sonarFinding.getStatus()).isEqualTo(FindingStatus.RESOLVED);
        assertThat(trivyFinding.getStatus()).isEqualTo(FindingStatus.OPEN); // Trivy 維持 OPEN
    }

    /**
     * Feature: 專案低頻對帳 - 併發防護
     * Scenario: 分散式鎖爭搶失敗 (其他節點正在對帳)
     * Given: distributedLockPort.tryLock(...) 回傳 false
     * When: 執行對帳
     * Then: 拋出 IllegalStateException，且不呼叫 ScannerPort 與 RepositoryPort
     */
    @Test
    @DisplayName("併發防護 - 分散式鎖被佔用時應拋出 IllegalStateException 防止重複執行")
    void shouldFailWhenLockCannotBeAcquired() {
        // Given
        String projectId = "busy-service";
        ReconcileProjectScanCommand command = new ReconcileProjectScanCommand(projectId);

        when(distributedLockPort.tryLock(anyString(), anyLong())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("該專案對帳作業正在執行中或並發衝突");

        verify(sonarQubeScannerPort, never()).fetchProjectIssues(any(), any(), any());
        verify(findingRepositoryPort, never()).findActiveByProjectAndBranch(any(), any());
    }
}
