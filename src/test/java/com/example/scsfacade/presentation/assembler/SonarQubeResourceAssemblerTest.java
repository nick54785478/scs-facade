package com.example.scsfacade.presentation.assembler;

import com.example.scsfacade.application.command.IngestSonarQubeCommand;
import com.example.scsfacade.application.dto.FindingGottenResult;
import com.example.scsfacade.presentation.resource.in.IngestSonarQubeResource;
import com.example.scsfacade.presentation.resource.in.ReceiveSonarQubeWebhookResource;
import com.example.scsfacade.presentation.resource.in.ReceiveSonarQubeWebhookResource.BranchNode;
import com.example.scsfacade.presentation.resource.in.ReceiveSonarQubeWebhookResource.ProjectNode;
import com.example.scsfacade.presentation.resource.out.FindingGottenResource;
import com.example.scsfacade.presentation.resource.out.SonarQubeIngestedResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SonarQube 表現層組裝器測試 (SonarQubeResourceAssemblerTest)。
 * <p>
 * 驗證 Request/Response Resource 與 Command/DTO 之間的互轉防腐邏輯。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class SonarQubeResourceAssemblerTest {

    @Mock
    private FindingResourceAssembler findingResourceAssembler;

    private SonarQubeResourceAssembler sonarQubeResourceAssembler;

    @BeforeEach
    void setUp() {
        sonarQubeResourceAssembler = new SonarQubeResourceAssembler(findingResourceAssembler);
    }

    /**
     * Feature: 手動觸發匯入請求轉命令
     * Scenario: 成功將 IngestSonarQubeResource 轉為 IngestSonarQubeCommand
     * Given: 包含專案代碼、分支與提交雜湊的請求載體
     * When: 執行 toCommand(IngestSonarQubeResource)
     * Then: 產出欄位完全對應之 IngestSonarQubeCommand
     */
    @Test
    @DisplayName("手動觸發請求轉換 - 應正確產出應用層命令")
    void shouldConvertIngestResourceToCommand() {
        // Given
        IngestSonarQubeResource resource = new IngestSonarQubeResource(
                "scs-facade",
                "dev",
                "commit-abc"
        );

        // When
        IngestSonarQubeCommand command = sonarQubeResourceAssembler.toCommand(resource);

        // Then
        assertThat(command).isNotNull();
        assertThat(command.projectId()).isEqualTo("scs-facade");
        assertThat(command.branch()).isEqualTo("dev");
        assertThat(command.commitHash()).isEqualTo("commit-abc");
    }

    /**
     * Feature: 手動觸發匯入空值檢查
     * Scenario: 傳入 null 時應回傳 null
     * Given: null 請求載體
     * When: 執行 toCommand(IngestSonarQubeResource)
     * Then: 回傳 null
     */
    @Test
    @DisplayName("手動觸發請求傳入 null - 應安全回傳 null")
    void shouldReturnNullWhenIngestResourceIsNull() {
        // When
        IngestSonarQubeCommand command = sonarQubeResourceAssembler.toCommand((IngestSonarQubeResource) null);

        // Then
        assertThat(command).isNull();
    }

    /**
     * Feature: 原生 Webhook 請求轉命令
     * Scenario: 成功解析完整 SonarQube 原生 Webhook 酬載
     * Given: 包含 project、branch、revision 的 Webhook 請求
     * When: 執行 toCommand(ReceiveSonarQubeWebhookResource)
     * Then: 正確解析出 project key、branch name 與 revision commitHash
     */
    @Test
    @DisplayName("原生 Webhook 請求轉換 - 包含完整分支與版本時應正確轉換")
    void shouldConvertWebhookResourceToCommandWithFullPayload() {
        // Given
        ProjectNode project = new ProjectNode("scs-facade", "SCS Facade", "http://localhost:9000/dashboard?id=scs-facade");
        BranchNode branch = new BranchNode("feature/login", "BRANCH", true, "http://localhost:9000/branch");
        ReceiveSonarQubeWebhookResource.QualityGateNode qualityGate =
                new ReceiveSonarQubeWebhookResource.QualityGateNode("Sonar way", "OK");
        ReceiveSonarQubeWebhookResource resource = new ReceiveSonarQubeWebhookResource(
                "http://localhost:9000",
                "12345",
                "SUCCESS",
                "2026-10-13T00:00:00+0000",
                "commit-rev-999",
                project,
                branch,
                qualityGate
        );

        // When
        IngestSonarQubeCommand command = sonarQubeResourceAssembler.toCommand(resource);

        // Then
        assertThat(command).isNotNull();
        assertThat(command.projectId()).isEqualTo("scs-facade");
        assertThat(command.branch()).isEqualTo("feature/login");
        assertThat(command.commitHash()).isEqualTo("commit-rev-999");
    }

    /**
     * Feature: 原生 Webhook 請求缺少可選欄位時的預設值保護
     * Scenario: 缺少 branch 與 revision 時應安全預設為 main 與空字串
     * Given: branch 與 revision 為 null 的 Webhook 請求
     * When: 執行 toCommand(ReceiveSonarQubeWebhookResource)
     * Then: branch 預設為 main，commitHash 預設為空字串
     */
    @Test
    @DisplayName("原生 Webhook 請求轉換 - 缺少分支與版本時應使用安全預設值")
    void shouldConvertWebhookResourceWithDefaultValuesWhenBranchAndRevisionMissing() {
        // Given
        ProjectNode project = new ProjectNode("scs-facade", "SCS Facade", "http://localhost:9000/dashboard?id=scs-facade");
        ReceiveSonarQubeWebhookResource resource = new ReceiveSonarQubeWebhookResource(
                "http://localhost:9000",
                "12345",
                "SUCCESS",
                "2026-10-13T00:00:00+0000",
                null,
                project,
                null,
                null
        );

        // When
        IngestSonarQubeCommand command = sonarQubeResourceAssembler.toCommand(resource);

        // Then
        assertThat(command).isNotNull();
        assertThat(command.projectId()).isEqualTo("scs-facade");
        assertThat(command.branch()).isEqualTo("main");
        assertThat(command.commitHash()).isEmpty();
    }

    /**
     * Feature: 原生 Webhook 請求驗證
     * Scenario: 傳入 null 或缺少 project 資訊時拋出例外
     * Given: null 載體或 project 為 null
     * When: 執行 toCommand(ReceiveSonarQubeWebhookResource)
     * Then: 拋出 IllegalArgumentException 阻止非法資料處理
     */
    @Test
    @DisplayName("原生 Webhook 請求驗證 - 缺少專案資訊時應拋出 IllegalArgumentException")
    void shouldThrowExceptionWhenWebhookResourceOrProjectIsNull() {
        // Given null resource
        assertThatThrownBy(() -> sonarQubeResourceAssembler.toCommand((ReceiveSonarQubeWebhookResource) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("缺少專案資訊");

        // Given resource without project
        ReceiveSonarQubeWebhookResource invalidResource = new ReceiveSonarQubeWebhookResource(
                "http://localhost:9000",
                "12345",
                "SUCCESS",
                "2026-10-13T00:00:00+0000",
                "rev1",
                null,
                null,
                null
        );
        assertThatThrownBy(() -> sonarQubeResourceAssembler.toCommand(invalidResource))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("缺少專案資訊");
    }

    /**
     * Feature: 弱點 DTO 轉回應載體
     * Scenario: 成功將 FindingGottenResult 清單組裝為 SonarQubeIngestedResource
     * Given: 專案代碼與弱點 DTO 清單
     * When: 執行 toResource(projectId, results)
     * Then: 回傳對應弱點數量與內容的 SonarQubeIngestedResource
     */
    @Test
    @DisplayName("弱點結果轉回應 - 應正確組裝弱點數量與清單")
    void shouldConvertFindingResultsToIngestedResource() {
        // Given
        FindingGottenResult mockDto = mock(FindingGottenResult.class);
        FindingGottenResource mockResource = mock(FindingGottenResource.class);
        when(findingResourceAssembler.toResource(mockDto)).thenReturn(mockResource);

        // When
        SonarQubeIngestedResource response = sonarQubeResourceAssembler.toResource("scs-facade", List.of(mockDto));

        // Then
        assertThat(response).isNotNull();
        assertThat(response.projectId()).isEqualTo("scs-facade");
        assertThat(response.totalIngested()).isEqualTo(1);
        assertThat(response.findings()).containsExactly(mockResource);
    }

    /**
     * Feature: 弱點結果清單為空時防護
     * Scenario: 傳入 null 清單時應回傳計數為 0 且空清單之回應
     * Given: results 為 null
     * When: 執行 toResource(projectId, null)
     * Then: 回傳 totalIngested 為 0
     */
    @Test
    @DisplayName("弱點結果為空時防護 - 應回傳安全空清單")
    void shouldHandleNullFindingResultsSafely() {
        // When
        SonarQubeIngestedResource response = sonarQubeResourceAssembler.toResource("scs-facade", null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.projectId()).isEqualTo("scs-facade");
        assertThat(response.totalIngested()).isZero();
        assertThat(response.findings()).isEmpty();
    }
}
