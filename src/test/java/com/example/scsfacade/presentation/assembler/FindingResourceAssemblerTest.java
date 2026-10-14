package com.example.scsfacade.presentation.assembler;

import com.example.scsfacade.application.dto.FindingGottenResult;
import com.example.scsfacade.presentation.resource.out.FindingGottenResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 弱點表現層組裝器單元測試 (FindingResourceAssemblerTest)。
 * <p>
 * 遵循 BDD / Gherkin 規範，驗證 FindingResourceAssembler 的 DTO 轉 Resource 行為。
 * </p>
 */
class FindingResourceAssemblerTest {

    private final FindingResourceAssembler assembler = new FindingResourceAssembler();

    /**
     * Feature: 應用層 DTO 轉換為對外 Resource
     * Scenario: 傳入完整之 FindingGottenResult 應正確轉換為 FindingGottenResource
     * Given: 包含弱點完整屬性之 DTO
     * When: 調用 assembler.toResource
     * Then: 回傳之 Resource 欄位值與 DTO 嚴格一致
     */
    @Test
    @DisplayName("DTO 轉換為 Resource - 正常情境應完整映射所有欄位")
    void shouldMapDtoToResourceSuccessfully() {
        // Given
        Instant now = Instant.now();
        FindingGottenResult dto = new FindingGottenResult(
                "f-001", "proj-1", "main", "c-100", "c-100", "c-100", null,
                "fp-abc", "SonarQube", "10.0", "SAST", "VULNERABILITY",
                "java:S2077", "SQL Injection", List.of("CWE-89"), null, List.of("sql"),
                "HIGH", "src/App.java", 10, 15, "SELECT *", "OPEN",
                now, now, null, "{}"
        );

        // When
        FindingGottenResource resource = assembler.toResource(dto);

        // Then
        assertThat(resource).isNotNull();
        assertThat(resource.findingId()).isEqualTo("f-001");
        assertThat(resource.projectId()).isEqualTo("proj-1");
        assertThat(resource.ruleId()).isEqualTo("java:S2077");
        assertThat(resource.severity()).isEqualTo("HIGH");
        assertThat(resource.status()).isEqualTo("OPEN");
    }

    /**
     * Feature: 容錯處理
     * Scenario: 傳入 null 時安全回傳 null
     */
    @Test
    @DisplayName("DTO 轉換為 Resource - 傳入 null 應回傳 null")
    void shouldReturnNullWhenInputIsNull() {
        // When
        FindingGottenResource resource = assembler.toResource(null);

        // Then
        assertThat(resource).isNull();
    }
}
