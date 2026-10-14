package com.example.scsfacade.presentation.controller;

import com.example.scsfacade.application.command.EvaluateQualityGateCommand;
import com.example.scsfacade.application.dto.QualityGateEvaluatedResult;
import com.example.scsfacade.application.port.in.EvaluateQualityGateUseCase;
import com.example.scsfacade.presentation.assembler.QualityGateResourceAssembler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 品質門禁控制器單元測試 (QualityGateControllerTest)。
 * <p>
 * 遵循 BDD / Gherkin 規範，驗證 QualityGateController REST API。
 * </p>
 */
@WebMvcTest(QualityGateController.class)
@Import(QualityGateResourceAssembler.class)
class QualityGateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EvaluateQualityGateUseCase evaluateQualityGateUseCase;

    /**
     * Feature: 評估專案是否通過品質門禁
     * Scenario: 發送 POST /api/v1/quality-gate/evaluate 成功完成評估並回傳 200 OK
     */
    @Test
    @DisplayName("門禁評估 - 成功時應回傳 200 OK 與評估結果")
    void shouldEvaluateQualityGateSuccessfully() throws Exception {
        // Given
        QualityGateEvaluatedResult result = new QualityGateEvaluatedResult(
                "scs-facade", "PASSED", true, 0, 1, "門禁審查合格", Instant.now()
        );
        when(evaluateQualityGateUseCase.execute(any(EvaluateQualityGateCommand.class))).thenReturn(result);

        String requestJson = """
                {
                    "projectId": "scs-facade",
                    "maxCritical": 0,
                    "maxHigh": 2,
                    "blockOnSecrets": true
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/quality-gate/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value("scs-facade"))
                .andExpect(jsonPath("$.status").value("PASSED"))
                .andExpect(jsonPath("$.passed").value(true));

        verify(evaluateQualityGateUseCase).execute(any(EvaluateQualityGateCommand.class));
    }
}
