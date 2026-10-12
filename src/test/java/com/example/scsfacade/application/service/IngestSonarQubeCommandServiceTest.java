package com.example.scsfacade.application.service;

import com.example.scsfacade.application.assembler.FindingDtoAssembler;
import com.example.scsfacade.application.command.IngestSonarQubeCommand;
import com.example.scsfacade.application.dto.FindingGottenResult;
import com.example.scsfacade.application.port.out.FindingEventPublisherPort;
import com.example.scsfacade.application.port.out.FindingRepositoryPort;
import com.example.scsfacade.application.port.out.SonarQubeScannerPort;
import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingFingerprint;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingLocation;
import com.example.scsfacade.domain.finding.aggregate.vo.RuleInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    private FindingEventPublisherPort findingEventPublisherPort;

    private FindingDtoAssembler findingDtoAssembler;
    private IngestSonarQubeCommandService ingestSonarQubeCommandService;

    @BeforeEach
    void setUp() {
        findingDtoAssembler = new FindingDtoAssembler();
        ingestSonarQubeCommandService = new IngestSonarQubeCommandService(
                sonarQubeScannerPort,
                findingRepositoryPort,
                findingEventPublisherPort,
                findingDtoAssembler
        );
    }

    /**
     * Feature: SonarQube 弱點報告匯入
     * Scenario: 成功匯入全新弱點並發布事件
     * Given: SonarQube 回傳一筆全新的 SQL Injection 弱點 (Critical 等級)
     * When: 執行 IngestSonarQubeCommand 匯入命令
     * Then: 成功調用儲存庫持久化，並發布領域事件，回傳對應之 DTO
     */
    @Test
    @DisplayName("匯入新弱點 - 應正確儲存並發布領域事件")
    void shouldSuccessfullyIngestNewFinding() {
        // Given
        String projectId = "test-project";
        IngestSonarQubeCommand command = new IngestSonarQubeCommand(projectId, "main", "commit-123");

        Finding newFinding = Finding.createNew(
                projectId,
                "main",
                "commit-123",
                new ScannerToolInfo("SonarQube", "10.0", "SAST"),
                new RuleInfo("java:S2077", "SQL Injection", List.of("CWE-89"), null),
                Severity.CRITICAL,
                new FindingLocation("src/OrderService.java", 42, 45, "SELECT * FROM orders"),
                "{}"
        );

        when(sonarQubeScannerPort.fetchProjectIssues(projectId, "main", "commit-123"))
                .thenReturn(List.of(newFinding));
        when(findingRepositoryPort.findByFingerprint(newFinding.getFingerprint()))
                .thenReturn(Optional.empty());
        when(findingRepositoryPort.save(any(Finding.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<FindingGottenResult> results = ingestSonarQubeCommandService.execute(command);

        // Then
        assertThat(results).hasSize(1);
        FindingGottenResult result = results.get(0);
        assertThat(result.projectId()).isEqualTo(projectId);
        assertThat(result.severity()).isEqualTo("CRITICAL");
        assertThat(result.ruleId()).isEqualTo("java:S2077");

        verify(findingRepositoryPort).save(any(Finding.class));
        verify(findingEventPublisherPort).publishAll(any());
    }

    /**
     * Feature: SonarQube 弱點報告匯入
     * Scenario: 匯入已存在之弱點時能依指紋正確去重並更新最後掃描時間
     * Given: 儲存庫中已存在相同指紋之弱點記錄
     * When: 執行匯入命令
     * Then: 更新既有弱點之掃描時間戳記，不重複新增
     */
    @Test
    @DisplayName("匯入既有弱點 - 應依指紋去重並刷新狀態")
    void shouldDeduplicateAndTouchExistingFinding() {
        // Given
        String projectId = "test-project";
        IngestSonarQubeCommand command = new IngestSonarQubeCommand(projectId, "main", "commit-456");

        Finding existingFinding = Finding.createNew(
                projectId,
                "main",
                "commit-old",
                new ScannerToolInfo("SonarQube", "10.0", "SAST"),
                new RuleInfo("java:S2077", "SQL Injection", List.of("CWE-89"), null),
                Severity.HIGH,
                new FindingLocation("src/OrderService.java", 42, 45, "SELECT * FROM orders"),
                "{}"
        );
        Instant firstSeen = existingFinding.getFirstSeenAt();

        when(sonarQubeScannerPort.fetchProjectIssues(projectId, "main", "commit-456"))
                .thenReturn(List.of(existingFinding));
        when(findingRepositoryPort.findByFingerprint(any(FindingFingerprint.class)))
                .thenReturn(Optional.of(existingFinding));
        when(findingRepositoryPort.save(any(Finding.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<FindingGottenResult> results = ingestSonarQubeCommandService.execute(command);

        // Then
        assertThat(results).hasSize(1);
        assertThat(existingFinding.getLastSeenAt()).isAfterOrEqualTo(firstSeen);
        verify(findingRepositoryPort).save(existingFinding);
    }
}
