package com.example.scsfacade.presentation.assembler;

import com.example.scsfacade.application.dto.FindingGottenResult;
import com.example.scsfacade.application.dto.ScanExecutionGottenResult;
import com.example.scsfacade.presentation.resource.out.ScanExecutionGottenResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 掃描執行表現層組裝器單元測試 (ScanExecutionResourceAssemblerTest)。
 * <p>
 * 遵循 BDD / Gherkin 規範，驗證 ScanExecutionResourceAssembler 的 DTO 轉 Resource 行為。
 * </p>
 */
class ScanExecutionResourceAssemblerTest {

    private final FindingResourceAssembler findingResourceAssembler = new FindingResourceAssembler();
    private final ScanExecutionResourceAssembler assembler = new ScanExecutionResourceAssembler(findingResourceAssembler);

    /**
     * Feature: 掃描結果 DTO 轉換為對外 Resource
     * Scenario: 傳入完整 ScanExecutionGottenResult 應正確映射並包含內嵌 Findings
     */
    @Test
    @DisplayName("掃描結果 DTO 轉換 - 正常情境應完整映射並轉換弱點子清單")
    void shouldMapScanExecutionDtoToResource() {
        // Given
        FindingGottenResult findingDto = new FindingGottenResult(
                "f-1", "p1", "main", "c1", "c1", "c1", null,
                "fp1", "Sonar", "1.0", "SAST", "CODE_SMELL", "r1", "rule1",
                List.of(), null, List.of(), "LOW", "App.java", 1, 2, "code",
                "OPEN", Instant.now(), Instant.now(), null, "{}"
        );

        ScanExecutionGottenResult dto = new ScanExecutionGottenResult(
                "exec-1", "p1", "main", "c1", "Sonar", "1.0",
                "WEBHOOK", "SUCCESS", 1, 1, 0, 0, 0, 0, true,
                Instant.now(), Instant.now(), null, List.of(findingDto)
        );

        // When
        ScanExecutionGottenResource resource = assembler.toResource(dto);

        // Then
        assertThat(resource).isNotNull();
        assertThat(resource.executionId()).isEqualTo("exec-1");
        assertThat(resource.findings()).hasSize(1);
        assertThat(resource.findings().get(0).findingId()).isEqualTo("f-1");
    }

    /**
     * Feature: 容錯處理
     * Scenario: 傳入 null 應回傳 null
     */
    @Test
    @DisplayName("掃描結果 DTO 轉換 - 傳入 null 應回傳 null")
    void shouldReturnNullWhenInputIsNull() {
        // When
        ScanExecutionGottenResource resource = assembler.toResource(null);

        // Then
        assertThat(resource).isNull();
    }
}
