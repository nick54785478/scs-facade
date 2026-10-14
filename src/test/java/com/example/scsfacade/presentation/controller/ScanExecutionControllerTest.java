package com.example.scsfacade.presentation.controller;

import com.example.scsfacade.application.dto.FindingGottenResult;
import com.example.scsfacade.application.dto.ScanExecutionGottenResult;
import com.example.scsfacade.application.port.in.GetScanExecutionUseCase;
import com.example.scsfacade.application.port.in.ListScanExecutionsUseCase;
import com.example.scsfacade.application.query.GetScanExecutionQuery;
import com.example.scsfacade.application.query.ListScanExecutionsQuery;
import com.example.scsfacade.presentation.assembler.FindingResourceAssembler;
import com.example.scsfacade.presentation.assembler.ScanExecutionResourceAssembler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 掃描執行歷程控制器單元測試 (ScanExecutionControllerTest)。
 * <p>
 * 遵循 BDD / Gherkin 規範，驗證 ScanExecutionController REST API 端點行為。
 * </p>
 */
@WebMvcTest(ScanExecutionController.class)
@Import({ScanExecutionResourceAssembler.class, FindingResourceAssembler.class})
class ScanExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetScanExecutionUseCase getScanExecutionUseCase;

    @MockBean
    private ListScanExecutionsUseCase listScanExecutionsUseCase;

    /**
     * Feature: 依識別碼查詢單次掃描歷程記錄
     * Scenario: 呼叫 GET /api/v1/scan-executions/{id} 正確回傳 200 OK 與掃描記錄詳細資訊
     * Given: 應用層 UseCase 回傳一筆掃描結果 DTO
     * When: 發送 GET 請求至 /api/v1/scan-executions/exec-101
     * Then: HTTP 狀態碼為 200 OK，JSON 欄位與 DTO 吻合
     */
    @Test
    @DisplayName("查詢單筆掃描執行 - 成功時應回傳 200 OK 與掃描資料")
    void shouldReturnScanExecutionWhenFound() throws Exception {
        // Given
        ScanExecutionGottenResult result = new ScanExecutionGottenResult(
                "exec-101",
                "scs-facade",
                "main",
                "commit-abc",
                "SonarQube",
                "10.0",
                "WEBHOOK",
                "SUCCESS",
                5,
                2,
                3,
                1,
                0,
                2,
                true,
                Instant.now(),
                Instant.now(),
                null,
                Collections.emptyList()
        );
        when(getScanExecutionUseCase.execute(any(GetScanExecutionQuery.class))).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/api/v1/scan-executions/exec-101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value("exec-101"))
                .andExpect(jsonPath("$.projectId").value("scs-facade"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.passedQualityGate").value(true));

        verify(getScanExecutionUseCase).execute(any(GetScanExecutionQuery.class));
    }

    /**
     * Feature: 查詢專案歷史掃描歷程清單
     * Scenario: 呼叫 GET /api/v1/scan-executions?projectId=scs-facade&branch=main
     * Given: 應用層 UseCase 回傳兩筆掃描歷史 DTO
     * When: 發送帶有 projectId 與 branch 查詢參數之 GET 請求
     * Then: HTTP 狀態碼為 200 OK，回傳清單長度為 2
     */
    @Test
    @DisplayName("查詢歷史掃描清單 - 成功時應回傳 200 OK 與掃描歷程陣列")
    void shouldReturnScanExecutionListWhenQueried() throws Exception {
        // Given
        ScanExecutionGottenResult item1 = new ScanExecutionGottenResult(
                "exec-1", "scs-facade", "main", "c1", "SonarQube", "10.0",
                "WEBHOOK", "SUCCESS", 2, 1, 1, 0, 0, 1, true,
                Instant.now(), Instant.now(), null, Collections.emptyList()
        );
        ScanExecutionGottenResult item2 = new ScanExecutionGottenResult(
                "exec-2", "scs-facade", "main", "c2", "SonarQube", "10.0",
                "WEBHOOK", "SUCCESS", 1, 0, 1, 1, 0, 0, true,
                Instant.now(), Instant.now(), null, Collections.emptyList()
        );
        when(listScanExecutionsUseCase.execute(any(ListScanExecutionsQuery.class)))
                .thenReturn(List.of(item1, item2));

        // When & Then
        mockMvc.perform(get("/api/v1/scan-executions")
                        .param("projectId", "scs-facade")
                        .param("branch", "main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].executionId").value("exec-1"))
                .andExpect(jsonPath("$[1].executionId").value("exec-2"));

        verify(listScanExecutionsUseCase).execute(any(ListScanExecutionsQuery.class));
    }
}
