package com.example.scsfacade.presentation.controller;

import com.example.scsfacade.application.command.ReconcileAllProjectsCommand;
import com.example.scsfacade.application.command.ReconcileProjectScanCommand;
import com.example.scsfacade.application.dto.ScanReconciliationGottenResult;
import com.example.scsfacade.application.port.in.ReconcileAllProjectsUseCase;
import com.example.scsfacade.application.port.in.ReconcileProjectScanUseCase;
import com.example.scsfacade.presentation.assembler.ScanReconciliationResourceAssembler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 掃描對帳控制器單元測試 (ScanReconciliationControllerTest)。
 */
@WebMvcTest(ScanReconciliationController.class)
@Import(ScanReconciliationResourceAssembler.class)
class ScanReconciliationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReconcileProjectScanUseCase reconcileProjectScanUseCase;

    @MockBean
    private ReconcileAllProjectsUseCase reconcileAllProjectsUseCase;

    /**
     * Feature: 手動觸發單一專案對帳端點
     * Scenario: 傳入合法專案對帳參數，回傳 200 OK 與對帳結果
     * Given: 包含 projectId 與 branch 之 JSON Payload
     * When: 發送 POST /api/v1/scans/reconcile
     * Then: HTTP 狀態為 200 OK，包含結算之統計與品質門禁資訊
     */
    @Test
    @DisplayName("單專案對帳端點 - 成功發起對帳應回傳 200 OK 與對帳統計資訊")
    void shouldReconcileSingleProjectSuccessfully() throws Exception {
        // Given
        ScanReconciliationGottenResult result = new ScanReconciliationGottenResult(
                "order-service", "main", "SonarQube", 15, 16, 2, 1, 0, true, Instant.now()
        );
        when(reconcileProjectScanUseCase.execute(any(ReconcileProjectScanCommand.class))).thenReturn(result);

        String requestBody = """
                {
                    "projectId": "order-service",
                    "branch": "main",
                    "forceSync": true
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/scans/reconcile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value("order-service"))
                .andExpect(jsonPath("$.branch").value("main"))
                .andExpect(jsonPath("$.toolName").value("SonarQube"))
                .andExpect(jsonPath("$.remoteActiveCount").value(15))
                .andExpect(jsonPath("$.localActiveCount").value(16))
                .andExpect(jsonPath("$.reconciledResolvedCount").value(2))
                .andExpect(jsonPath("$.reconciledAppendedCount").value(1))
                .andExpect(jsonPath("$.passedQualityGate").value(true));

        verify(reconcileProjectScanUseCase).execute(any(ReconcileProjectScanCommand.class));
    }

    /**
     * Feature: 專案對帳參數驗證
     * Scenario: 傳入空白之 projectId
     * Given: projectId 為空字串
     * When: 發送 POST /api/v1/scans/reconcile
     * Then: 觸發 JSR-380 驗證並回傳 400 Bad Request
     */
    @Test
    @DisplayName("參數校驗 - projectId 為空應回傳 400 Bad Request")
    void shouldRejectBlankProjectId() throws Exception {
        // Given
        String requestBody = """
                {
                    "projectId": "   ",
                    "branch": "main"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/scans/reconcile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    /**
     * Feature: 手動觸發全專案批次對帳端點
     * Scenario: 發起全專案批次對帳請求
     * Given: 批次對帳用例回傳 2 筆專案對帳結果
     * When: 發送 POST /api/v1/scans/reconcile-all
     * Then: HTTP 狀態為 200 OK，回傳對帳結果清單陣列
     */
    @Test
    @DisplayName("批次對帳端點 - 成功發起全專案批次對帳應回傳 200 OK 與陣列清單")
    void shouldReconcileAllProjectsSuccessfully() throws Exception {
        // Given
        ScanReconciliationGottenResult r1 = new ScanReconciliationGottenResult(
                "p1", "main", "SonarQube", 5, 5, 0, 0, 0, true, Instant.now()
        );
        ScanReconciliationGottenResult r2 = new ScanReconciliationGottenResult(
                "p2", "main", "SonarQube", 8, 8, 0, 0, 0, true, Instant.now()
        );
        when(reconcileAllProjectsUseCase.execute(any(ReconcileAllProjectsCommand.class)))
                .thenReturn(List.of(r1, r2));

        // When & Then
        mockMvc.perform(post("/api/v1/scans/reconcile-all")
                        .param("forceSync", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].projectId").value("p1"))
                .andExpect(jsonPath("$[1].projectId").value("p2"));

        verify(reconcileAllProjectsUseCase).execute(any(ReconcileAllProjectsCommand.class));
    }
}
