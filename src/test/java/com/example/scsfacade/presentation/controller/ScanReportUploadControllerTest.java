package com.example.scsfacade.presentation.controller;

import com.example.scsfacade.application.dto.ScanReportUploadedResult;
import com.example.scsfacade.application.port.in.UploadScanReportUseCase;
import com.example.scsfacade.presentation.assembler.ScanReportResourceAssembler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 掃描報告上傳控制器單元測試 (ScanReportUploadControllerTest)。
 */
@WebMvcTest(ScanReportUploadController.class)
@Import(ScanReportResourceAssembler.class)
class ScanReportUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UploadScanReportUseCase uploadScanReportUseCase;

    /**
     * Feature: 上傳 SARIF 報告端點
     * Scenario: 呼叫 POST /api/v1/scans/upload 上傳報告並取得 200 OK
     * Given: 合法之 MultipartFile 與請求參數
     * When: 發送 multipart 請求
     * Then: HTTP 狀態為 200 OK，回傳結算資訊
     */
    @Test
    @DisplayName("上傳報告 - 成功上傳 SARIF 檔案應回傳 200 OK 與結算結果")
    void shouldUploadScanReportSuccessfully() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "trivy.sarif",
                MediaType.APPLICATION_JSON_VALUE,
                "{\"runs\": []}".getBytes(StandardCharsets.UTF_8)
        );

        ScanReportUploadedResult result = new ScanReportUploadedResult(
                "exec-101", "payment-service", "main", "commit-aaa", "Trivy", "SUCCESS",
                true, 5, 1, 4, 0, 0, 1, Instant.now()
        );
        when(uploadScanReportUseCase.execute(any())).thenReturn(result);

        // When & Then
        mockMvc.perform(multipart("/api/v1/scans/upload")
                        .file(file)
                        .param("projectId", "payment-service")
                        .param("branch", "main")
                        .param("commitHash", "commit-aaa")
                        .param("toolName", "Trivy")
                        .param("externalTaskId", "task-101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scanExecutionId").value("exec-101"))
                .andExpect(jsonPath("$.projectId").value("payment-service"))
                .andExpect(jsonPath("$.toolName").value("Trivy"))
                .andExpect(jsonPath("$.passedQualityGate").value(true))
                .andExpect(jsonPath("$.totalActiveCount").value(5))
                .andExpect(jsonPath("$.newCount").value(1));

        verify(uploadScanReportUseCase).execute(any());
    }

    /**
     * Feature: 上傳報告檔案為空防護
     * Scenario: 上傳 0 byte 內容之空檔案
     * Given: 空的 MockMultipartFile
     * When: 發送 POST /api/v1/scans/upload
     * Then: 拋出異常並回傳客戶端錯誤 (4xx)
     */
    @Test
    @DisplayName("檔案為空防護 - 傳入 0 byte 檔案應拋出錯誤")
    void shouldRejectEmptyFile() throws Exception {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.sarif", MediaType.APPLICATION_JSON_VALUE, new byte[0]
        );

        // When & Then
        mockMvc.perform(multipart("/api/v1/scans/upload")
                        .file(emptyFile)
                        .param("projectId", "payment-service"))
                .andExpect(status().is4xxClientError());
    }
}
