package com.example.scsfacade.presentation.assembler;

import com.example.scsfacade.application.command.ReconcileProjectScanCommand;
import com.example.scsfacade.application.dto.ScanReconciliationGottenResult;
import com.example.scsfacade.presentation.resource.in.ReconcileProjectScanResource;
import com.example.scsfacade.presentation.resource.out.ScanReconciledResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 掃描對帳表現層組裝器單元測試 (ScanReconciliationResourceAssemblerTest)。
 */
class ScanReconciliationResourceAssemblerTest {

    private ScanReconciliationResourceAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new ScanReconciliationResourceAssembler();
    }

    /**
     * Feature: 請求資源轉命令
     * Scenario: 將合法之 ReconcileProjectScanResource 轉換為 ReconcileProjectScanCommand
     * Given: 包含 projectId, branch, forceSync 之請求資源
     * When: 執行 toCommand
     * Then: 產出欄位完全一致之命令物件
     */
    @Test
    @DisplayName("資源轉命令 - 應正確將請求資源對齊至應用層命令")
    void shouldConvertResourceToCommand() {
        // Given
        ReconcileProjectScanResource resource = new ReconcileProjectScanResource(
                "order-service", "feature/reconcile", true
        );

        // When
        ReconcileProjectScanCommand command = assembler.toCommand(resource);

        // Then
        assertThat(command).isNotNull();
        assertThat(command.projectId()).isEqualTo("order-service");
        assertThat(command.branch()).isEqualTo("feature/reconcile");
        assertThat(command.forceSync()).isTrue();
    }

    /**
     * Feature: 資源轉命令空值防護
     * Scenario: 傳入 null 資源物件
     * Given: resource = null
     * When: 執行 toCommand
     * Then: 拋出 IllegalArgumentException
     */
    @Test
    @DisplayName("資源轉命令空值防護 - 傳入 null 應拋出 IllegalArgumentException")
    void shouldThrowExceptionWhenResourceIsNull() {
        // When & Then
        assertThatThrownBy(() -> assembler.toCommand(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ReconcileProjectScanResource 不可為空");
    }

    /**
     * Feature: 結果 DTO 轉回應資源
     * Scenario: 將 ScanReconciliationGottenResult 轉換為 ScanReconciledResource
     * Given: 完整之對帳結果 DTO
     * When: 執行 toResource
     * Then: 各統計數值與門禁旗標精準對齊
     */
    @Test
    @DisplayName("結果轉資源 - 應正確將 DTO 轉換為表現層回應資源")
    void shouldConvertResultToResource() {
        // Given
        Instant now = Instant.now();
        ScanReconciliationGottenResult result = new ScanReconciliationGottenResult(
                "pay-service", "main", "SonarQube", 10, 8, 2, 4, 1, true, now
        );

        // When
        ScanReconciledResource resource = assembler.toResource(result);

        // Then
        assertThat(resource).isNotNull();
        assertThat(resource.projectId()).isEqualTo("pay-service");
        assertThat(resource.branch()).isEqualTo("main");
        assertThat(resource.toolName()).isEqualTo("SonarQube");
        assertThat(resource.remoteActiveCount()).isEqualTo(10);
        assertThat(resource.localActiveCount()).isEqualTo(8);
        assertThat(resource.reconciledResolvedCount()).isEqualTo(2);
        assertThat(resource.reconciledAppendedCount()).isEqualTo(4);
        assertThat(resource.severityAdjustedCount()).isEqualTo(1);
        assertThat(resource.passedQualityGate()).isTrue();
        assertThat(resource.executedAt()).isEqualTo(now);

        // Null 轉換防護
        assertThat(assembler.toResource(null)).isNull();
    }

    /**
     * Feature: 清單批次轉換
     * Scenario: 將結果清單批次轉換為資源清單
     * Given: 包含 2 筆結果之清單
     * When: 執行 toResourceList
     * Then: 回傳長度為 2 之資源清單；若傳入 null/空清單則回傳空清單
     */
    @Test
    @DisplayName("清單批次轉換 - 應正確支援批次轉換與空清單容錯")
    void shouldConvertResourceList() {
        // Given
        ScanReconciliationGottenResult r1 = new ScanReconciliationGottenResult(
                "p1", "main", "SonarQube", 1, 1, 0, 0, 0, true, Instant.now()
        );
        ScanReconciliationGottenResult r2 = new ScanReconciliationGottenResult(
                "p2", "main", "SonarQube", 2, 2, 0, 0, 0, true, Instant.now()
        );

        // When
        List<ScanReconciledResource> list = assembler.toResourceList(List.of(r1, r2));

        // Then
        assertThat(list).hasSize(2);
        assertThat(list.get(0).projectId()).isEqualTo("p1");
        assertThat(list.get(1).projectId()).isEqualTo("p2");

        assertThat(assembler.toResourceList(null)).isEmpty();
        assertThat(assembler.toResourceList(Collections.emptyList())).isEmpty();
    }
}
