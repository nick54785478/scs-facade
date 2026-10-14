package com.example.scsfacade.presentation.controller;

import com.example.scsfacade.application.command.IngestSonarQubeCommand;
import com.example.scsfacade.application.dto.ScanExecutionGottenResult;
import com.example.scsfacade.application.port.in.IngestSonarQubeUseCase;
import com.example.scsfacade.presentation.assembler.SonarQubeResourceAssembler;
import com.example.scsfacade.presentation.resource.in.IngestSonarQubeResource;
import com.example.scsfacade.presentation.resource.in.ReceiveSonarQubeWebhookResource;
import com.example.scsfacade.presentation.resource.in.ReceiveSonarQubeWebhookResource.BranchNode;
import com.example.scsfacade.presentation.resource.in.ReceiveSonarQubeWebhookResource.ProjectNode;
import com.example.scsfacade.presentation.resource.out.SonarQubeIngestedResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SonarQube Webhook 控制器測試 (SonarQubeWebhookControllerTest)。
 * <p>
 * 驗證表現層 HTTP 端點路由、參數組裝與調用 UseCase 流程。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class SonarQubeWebhookControllerTest {

    private MockMvc mockMvc;

    @Mock
    private IngestSonarQubeUseCase ingestSonarQubeUseCase;

    @Mock
    private SonarQubeResourceAssembler sonarQubeResourceAssembler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        SonarQubeWebhookController controller =
                new SonarQubeWebhookController(ingestSonarQubeUseCase, sonarQubeResourceAssembler);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * Feature: 手動觸發 SonarQube 匯入 API
     * Scenario: 接收合法觸發請求並成功回傳匯入結果
     * Given: 包含 projectId 的 IngestSonarQubeResource 請求 JSON
     * When: POST /api/v1/scans/sonarqube/trigger
     * Then: HTTP 狀態碼為 200 OK，回傳預期之 SonarQubeIngestedResource JSON
     */
    @Test
    @DisplayName("手動觸發匯入 - 應成功調用 UseCase 並回傳 200 OK")
    void shouldSuccessfullyTriggerIngestion() throws Exception {
        // Given
        IngestSonarQubeResource resource = new IngestSonarQubeResource("scs-facade", "main", "sha123");
        IngestSonarQubeCommand command = new IngestSonarQubeCommand("scs-facade", "main", "sha123");
        ScanExecutionGottenResult result = new ScanExecutionGottenResult(
                "exec-123", "scs-facade", "main", "sha123",
                "SonarQube", "10.0", "MANUAL", "SUCCESS",
                0, 0, 0, 0, 0, 0, true,
                Instant.now(), Instant.now(), null,
                Collections.emptyList()
        );
        SonarQubeIngestedResource expectedResponse =
                new SonarQubeIngestedResource("exec-123", "scs-facade", "main", "sha123", "SUCCESS", true, 0, 0, 0, 0, 0, 0, Collections.emptyList());

        when(sonarQubeResourceAssembler.toCommand(any(IngestSonarQubeResource.class))).thenReturn(command);
        when(ingestSonarQubeUseCase.execute(command)).thenReturn(result);
        when(sonarQubeResourceAssembler.toResource(result)).thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/scans/sonarqube/trigger")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resource)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value("scs-facade"))
                .andExpect(jsonPath("$.executionId").value("exec-123"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(ingestSonarQubeUseCase).execute(command);
    }

    /**
     * Feature: 接收 SonarQube 原生 Webhook API
     * Scenario: 接收原生 Webhook 酬載並自動調用 UseCase 執行匯入
     * Given: 包含專案資訊與分支的 ReceiveSonarQubeWebhookResource 請求 JSON
     * When: POST /api/v1/scans/sonarqube/webhook
     * Then: HTTP 狀態碼為 200 OK，成功執行同步並回傳匯入結果
     */
    @Test
    @DisplayName("接收原生 Webhook - 應成功解析酬載並調用 UseCase 回傳 200 OK")
    void shouldSuccessfullyHandleNativeWebhook() throws Exception {
        // Given
        ProjectNode project = new ProjectNode("scs-facade", "SCS Facade", "http://localhost:9000/dashboard");
        BranchNode branch = new BranchNode("dev", "BRANCH", true, "http://localhost:9000/branch");
        ReceiveSonarQubeWebhookResource webhookResource = new ReceiveSonarQubeWebhookResource(
                "http://localhost:9000",
                "task-123",
                "SUCCESS",
                "2026-10-13T01:00:00+0000",
                "commit-rev-456",
                project,
                branch,
                null
        );
        IngestSonarQubeCommand command = new IngestSonarQubeCommand("scs-facade", "dev", "commit-rev-456");
        ScanExecutionGottenResult result = new ScanExecutionGottenResult(
                "exec-456", "scs-facade", "dev", "commit-rev-456",
                "SonarQube", "10.0", "WEBHOOK", "SUCCESS",
                0, 0, 0, 0, 0, 0, true,
                Instant.now(), Instant.now(), null,
                Collections.emptyList()
        );
        SonarQubeIngestedResource expectedResponse =
                new SonarQubeIngestedResource("exec-456", "scs-facade", "dev", "commit-rev-456", "SUCCESS", true, 0, 0, 0, 0, 0, 0, Collections.emptyList());

        when(sonarQubeResourceAssembler.toCommand(any(ReceiveSonarQubeWebhookResource.class))).thenReturn(command);
        when(ingestSonarQubeUseCase.execute(command)).thenReturn(result);
        when(sonarQubeResourceAssembler.toResource(result)).thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/scans/sonarqube/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookResource)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value("scs-facade"))
                .andExpect(jsonPath("$.executionId").value("exec-456"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(ingestSonarQubeUseCase).execute(command);
    }
}
