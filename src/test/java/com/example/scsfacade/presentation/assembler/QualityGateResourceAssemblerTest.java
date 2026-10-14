package com.example.scsfacade.presentation.assembler;

import com.example.scsfacade.application.command.EvaluateQualityGateCommand;
import com.example.scsfacade.application.dto.QualityGateEvaluatedResult;
import com.example.scsfacade.presentation.resource.in.EvaluateQualityGateResource;
import com.example.scsfacade.presentation.resource.out.QualityGateEvaluatedResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 品質門禁表現層組裝器單元測試 (QualityGateResourceAssemblerTest)。
 * <p>
 * 驗證 QualityGateResourceAssembler 在 Resource 與 Command/DTO 之間的雙向轉換。
 * </p>
 */
class QualityGateResourceAssemblerTest {

    private final QualityGateResourceAssembler assembler = new QualityGateResourceAssembler();

    /**
     * Feature: Request Resource 轉換為 Application Command
     * Scenario: 傳入 EvaluateQualityGateResource 正確映射為 EvaluateQualityGateCommand
     */
    @Test
    @DisplayName("Resource 轉 Command - 應完整映射門檻參數")
    void shouldMapResourceToCommand() {
        // Given
        EvaluateQualityGateResource resource = new EvaluateQualityGateResource(
                "scs-facade", 0, 3, true
        );

        // When
        EvaluateQualityGateCommand command = assembler.toCommand(resource);

        // Then
        assertThat(command).isNotNull();
        assertThat(command.projectId()).isEqualTo("scs-facade");
        assertThat(command.maxCritical()).isZero();
        assertThat(command.maxHigh()).isEqualTo(3);
        assertThat(command.blockOnSecrets()).isTrue();
    }

    /**
     * Feature: DTO 轉換為 Response Resource
     * Scenario: 傳入 QualityGateEvaluatedResult 正確映射為 QualityGateEvaluatedResource
     */
    @Test
    @DisplayName("Result 轉 Resource - 應完整映射評估結果")
    void shouldMapResultToResource() {
        // Given
        QualityGateEvaluatedResult result = new QualityGateEvaluatedResult(
                "scs-facade", "PASSED", true, 0, 1, "Passed", Instant.now()
        );

        // When
        QualityGateEvaluatedResource resource = assembler.toResource(result);

        // Then
        assertThat(resource).isNotNull();
        assertThat(resource.projectId()).isEqualTo("scs-facade");
        assertThat(resource.status()).isEqualTo("PASSED");
        assertThat(resource.passed()).isTrue();
    }

    /**
     * Feature: 容錯處理
     * Scenario: 傳入 null 時安全回傳 null
     */
    @Test
    @DisplayName("空值防禦 - 傳入 null 應回傳 null")
    void shouldReturnNullWhenGivenNull() {
        assertThat(assembler.toCommand(null)).isNull();
        assertThat(assembler.toResource(null)).isNull();
    }
}
