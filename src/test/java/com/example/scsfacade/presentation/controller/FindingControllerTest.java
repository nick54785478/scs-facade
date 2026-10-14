package com.example.scsfacade.presentation.controller;

import com.example.scsfacade.application.dto.FindingGottenResult;
import com.example.scsfacade.application.port.in.GetFindingUseCase;
import com.example.scsfacade.application.query.GetFindingQuery;
import com.example.scsfacade.application.query.ListFindingQuery;
import com.example.scsfacade.application.view.FindingGottenView;
import com.example.scsfacade.presentation.assembler.FindingResourceAssembler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 弱點查詢控制器單元測試 (FindingControllerTest)。
 * <p>
 * 遵循 BDD / Gherkin 規範，驗證 FindingController REST API。
 * </p>
 */
@WebMvcTest(FindingController.class)
@Import(FindingResourceAssembler.class)
class FindingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetFindingUseCase getFindingUseCase;

    /**
     * Feature: 依弱點 ID 查詢詳細資訊
     * Scenario: 呼叫 GET /api/v1/findings/{id} 回傳 200 OK
     */
    @Test
    @DisplayName("依 ID 查詢弱點 - 成功時應回傳 200 OK 與詳細資訊")
    void shouldReturnFindingById() throws Exception {
        // Given
        Instant now = Instant.now();
        FindingGottenResult dto = new FindingGottenResult(
                "finding-101", "scs-facade", "main", "c1", "c1", "c1", null,
                "fp101", "SonarQube", "10.0", "SAST", "VULNERABILITY",
                "java:S2077", "SQL Injection", List.of("CWE-89"), null, List.of("sql"),
                "HIGH", "OrderService.java", 42, 45, "SELECT *", "OPEN",
                now, now, null, "{}"
        );
        when(getFindingUseCase.getById(any(GetFindingQuery.class))).thenReturn(dto);

        // When & Then
        mockMvc.perform(get("/api/v1/findings/finding-101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findingId").value("finding-101"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.ruleId").value("java:S2077"));

        verify(getFindingUseCase).getById(any(GetFindingQuery.class));
    }

    /**
     * Feature: 依專案查詢弱點清單投影
     * Scenario: 呼叫 GET /api/v1/findings?projectId=scs-facade&status=OPEN
     */
    @Test
    @DisplayName("依專案查詢弱點視圖 - 應回傳 200 OK 與投影清單")
    void shouldReturnFindingViewsByProject() throws Exception {
        // Given
        FindingGottenView view = new FindingGottenView(
                "finding-101", "scs-facade", "java:S2077",
                "HIGH", "OrderService.java", 42, "OPEN"
        );
        when(getFindingUseCase.listByProject(any(ListFindingQuery.class))).thenReturn(List.of(view));

        // When & Then
        mockMvc.perform(get("/api/v1/findings")
                        .param("projectId", "scs-facade")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].findingId").value("finding-101"))
                .andExpect(jsonPath("$[0].severity").value("HIGH"));

        verify(getFindingUseCase).listByProject(any(ListFindingQuery.class));
    }
}
