package com.example.scsfacade.application.assembler;

import com.example.scsfacade.application.dto.FindingGottenResult;
import com.example.scsfacade.application.dto.ScanExecutionGottenResult;
import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingCategory;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingLocation;
import com.example.scsfacade.domain.finding.aggregate.vo.RuleInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import com.example.scsfacade.domain.scanexecution.aggregate.root.ScanExecution;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanTriggerType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 應用層 DTO 組裝器單元測試 (DtoAssemblerTest)。
 * <p>
 * 驗證 FindingDtoAssembler 與 ScanExecutionDtoAssembler 之模型轉 DTO 行為。
 * </p>
 */
class DtoAssemblerTest {

    private final FindingDtoAssembler findingDtoAssembler = new FindingDtoAssembler();
    private final ScanExecutionDtoAssembler scanExecutionDtoAssembler = new ScanExecutionDtoAssembler();

    /**
     * Feature: 弱點聚合根轉換為應用層 DTO
     * Scenario: 傳入 Finding 聚合根應正確轉換為 FindingGottenResult
     */
    @Test
    @DisplayName("Finding 轉換為 DTO - 應正確映射各項領域屬性")
    void shouldMapFindingToDto() {
        // Given
        Finding finding = Finding.createNew(
                "proj-1", "main", "commit-1",
                new ScannerToolInfo("SonarQube", "10.0", "SAST"),
                FindingCategory.VULNERABILITY,
                new RuleInfo("java:S2077", "SQL Injection", List.of("CWE-89"), null, List.of("sql")),
                Severity.HIGH,
                new FindingLocation("OrderService.java", 10, 15, "SELECT *"),
                "{}"
        );

        // When
        FindingGottenResult dto = findingDtoAssembler.toResult(finding);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.projectId()).isEqualTo("proj-1");
        assertThat(dto.ruleId()).isEqualTo("java:S2077");
        assertThat(dto.severity()).isEqualTo("HIGH");
    }

    /**
     * Feature: 掃描執行聚合根轉換為應用層 DTO
     * Scenario: 傳入 ScanExecution 聚合根應正確轉換為 ScanExecutionGottenResult
     */
    @Test
    @DisplayName("ScanExecution 轉換為 DTO - 應正確映射並組裝結果")
    void shouldMapScanExecutionToDto() {
        // Given
        ScanExecution scanExecution = ScanExecution.start(
                "proj-1", "main", "commit-1",
                new ScannerToolInfo("SonarQube", "10.0", "SAST"),
                ScanTriggerType.WEBHOOK,
                "task-123"
        );
        scanExecution.complete(new com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanSummary(
                10, 2, 8, 0, 1, 3, false
        ));

        // When
        ScanExecutionGottenResult dto = scanExecutionDtoAssembler.toResult(scanExecution, Collections.emptyList());

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.projectId()).isEqualTo("proj-1");
        assertThat(dto.status()).isEqualTo("SUCCESS");
        assertThat(dto.totalActiveCount()).isEqualTo(10);
        assertThat(dto.passedQualityGate()).isFalse();
    }
}
