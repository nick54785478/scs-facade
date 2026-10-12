package com.example.scsfacade.application.service;

import com.example.scsfacade.application.assembler.QualityGateDtoAssembler;
import com.example.scsfacade.application.command.EvaluateQualityGateCommand;
import com.example.scsfacade.application.dto.QualityGateEvaluatedResult;
import com.example.scsfacade.application.port.out.FindingEventPublisherPort;
import com.example.scsfacade.application.port.out.FindingRepositoryPort;
import com.example.scsfacade.domain.finding.aggregate.root.Finding;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 評估品質門禁使用案例測試 (EvaluateQualityGateCommandServiceTest)。
 */
@ExtendWith(MockitoExtension.class)
class EvaluateQualityGateCommandServiceTest {

    @Mock
    private FindingRepositoryPort findingRepositoryPort;

    @Mock
    private FindingEventPublisherPort findingEventPublisherPort;

    private QualityGateDtoAssembler qualityGateDtoAssembler;
    private EvaluateQualityGateCommandService evaluateQualityGateCommandService;

    @BeforeEach
    void setUp() {
        qualityGateDtoAssembler = new QualityGateDtoAssembler();
        evaluateQualityGateCommandService = new EvaluateQualityGateCommandService(
                findingRepositoryPort,
                findingEventPublisherPort,
                qualityGateDtoAssembler
        );
    }

    /**
     * Feature: 品質門禁審查評估
     * Scenario: 當專案中無任何未修復之弱點時門禁評估通過
     * Given: 專案目前活躍弱點為空清單
     * When: 執行 EvaluateQualityGateCommand 門禁評估命令
     * Then: 回傳門禁通過狀態 (PASSED)，passed 為 true
     */
    @Test
    @DisplayName("門禁評估 - 無弱點時應判定為通過 (PASSED)")
    void shouldPassQualityGateWhenNoActiveFindings() {
        // Given
        String projectId = "clean-project";
        EvaluateQualityGateCommand command = new EvaluateQualityGateCommand(projectId, 0, 2, true);

        when(findingRepositoryPort.findActiveByProjectId(projectId)).thenReturn(List.of());

        // When
        QualityGateEvaluatedResult result = evaluateQualityGateCommandService.execute(command);

        // Then
        assertThat(result.passed()).isTrue();
        assertThat(result.status()).isEqualTo("PASSED");
        assertThat(result.criticalCount()).isZero();
        assertThat(result.highCount()).isZero();

        verify(findingEventPublisherPort).publishAll(any());
    }

    /**
     * Feature: 品質門禁審查評估
     * Scenario: 當專案存在 Critical 等級弱點且門檻為 0 時門禁未通過
     * Given: 專案存在 1 筆 Critical 等級之活躍弱點
     * When: 執行門禁評估命令 (maxCritical = 0)
     * Then: 回傳門禁未通過狀態 (FAILED)，passed 為 false
     */
    @Test
    @DisplayName("門禁評估 - 存在 Critical 弱點且門檻為 0 應判定為失敗 (FAILED)")
    void shouldFailQualityGateWhenCriticalExceedsThreshold() {
        // Given
        String projectId = "vulnerable-project";
        EvaluateQualityGateCommand command = new EvaluateQualityGateCommand(projectId, 0, 2, true);

        Finding criticalFinding = Finding.createNew(
                projectId,
                "main",
                "commit-1",
                new ScannerToolInfo("SonarQube", "10.0", "SAST"),
                new RuleInfo("java:S2077", "SQL Injection", List.of("CWE-89"), null),
                Severity.CRITICAL,
                new FindingLocation("OrderService.java", 10, 12, "sql"),
                "{}"
        );

        when(findingRepositoryPort.findActiveByProjectId(projectId)).thenReturn(List.of(criticalFinding));

        // When
        QualityGateEvaluatedResult result = evaluateQualityGateCommandService.execute(command);

        // Then
        assertThat(result.passed()).isFalse();
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.criticalCount()).isEqualTo(1);

        verify(findingEventPublisherPort).publishAll(any());
    }
}
