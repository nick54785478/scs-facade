package com.example.scsfacade.application.service;

import com.example.scsfacade.application.assembler.FindingDtoAssembler;
import com.example.scsfacade.application.dto.FindingGottenResult;
import com.example.scsfacade.application.query.GetFindingQuery;
import com.example.scsfacade.application.query.ListFindingQuery;
import com.example.scsfacade.application.view.FindingGottenView;
import com.example.scsfacade.application.port.out.FindingRepositoryPort;
import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingId;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingLocation;
import com.example.scsfacade.domain.finding.aggregate.vo.RuleInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import com.example.scsfacade.domain.finding.exception.FindingNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 弱點資料查詢使用案例測試 (FindingQueryServiceTest)。
 */
@ExtendWith(MockitoExtension.class)
class FindingQueryServiceTest {

    @Mock
    private FindingRepositoryPort findingRepositoryPort;

    private FindingDtoAssembler findingDtoAssembler;
    private FindingQueryService findingQueryService;

    @BeforeEach
    void setUp() {
        findingDtoAssembler = new FindingDtoAssembler();
        findingQueryService = new FindingQueryService(findingRepositoryPort, findingDtoAssembler);
    }

    /**
     * Feature: 弱點資料查詢
     * Scenario: 依據 ID 成功查詢弱點詳細資訊
     * Given: 儲存庫存在指定 ID 之弱點
     * When: 執行 GetFindingQuery 查詢
     * Then: 回傳包含正確資訊之 FindingGottenResult
     */
    @Test
    @DisplayName("依 ID 查詢 - 弱點存在時應正確回傳結果")
    void shouldReturnFindingWhenFound() {
        // Given
        Finding finding = Finding.createNew(
                "proj-1",
                "main",
                "commit-1",
                new ScannerToolInfo("SonarQube", "10.0", "SAST"),
                new RuleInfo("rule-1", "Rule Name", List.of(), null),
                Severity.HIGH,
                new FindingLocation("App.java", 1, 5, "code"),
                "{}"
        );
        FindingId id = finding.getId();
        GetFindingQuery query = new GetFindingQuery(id.getValue());

        when(findingRepositoryPort.findById(id)).thenReturn(Optional.of(finding));

        // When
        FindingGottenResult result = findingQueryService.getById(query);

        // Then
        assertThat(result.findingId()).isEqualTo(id.getValue());
        assertThat(result.severity()).isEqualTo("HIGH");
    }

    /**
     * Feature: 弱點資料查詢
     * Scenario: 查詢不存在之 ID 時拋出領域例外
     * Given: 儲存庫不存在指定 ID 之弱點
     * When: 執行 GetFindingQuery 查詢
     * Then: 拋出 FindingNotFoundException 領域例外
     */
    @Test
    @DisplayName("依 ID 查詢 - 弱點不存在時應拋出 FindingNotFoundException")
    void shouldThrowExceptionWhenNotFound() {
        // Given
        FindingId nonExistentId = FindingId.generate();
        GetFindingQuery query = new GetFindingQuery(nonExistentId.getValue());

        when(findingRepositoryPort.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> findingQueryService.getById(query))
                .isInstanceOf(FindingNotFoundException.class);
    }

    /**
     * Feature: 弱點資料查詢
     * Scenario: 依據專案與狀態篩選弱點列表
     * Given: 專案下有一筆 OPEN 弱點
     * When: 執行 ListFindingQuery (status = "OPEN")
     * Then: 正確回傳 1 筆 FindingGottenView 投影資料
     */
    @Test
    @DisplayName("依專案列表查詢 - 應依狀態過濾並回傳 Projection View")
    void shouldFilterFindingsByProjectAndStatus() {
        // Given
        String projectId = "proj-1";
        Finding finding = Finding.createNew(
                projectId,
                "main",
                "c1",
                new ScannerToolInfo("SonarQube", "10.0", "SAST"),
                new RuleInfo("r1", "rule", List.of(), null),
                Severity.LOW,
                new FindingLocation("File.java", 1, 1, ""),
                "{}"
        );
        ListFindingQuery query = new ListFindingQuery(projectId, "OPEN");

        when(findingRepositoryPort.findByProjectId(projectId)).thenReturn(List.of(finding));

        // When
        List<FindingGottenView> views = findingQueryService.listByProject(query);

        // Then
        assertThat(views).hasSize(1);
        assertThat(views.get(0).status()).isEqualTo("OPEN");
    }
}
