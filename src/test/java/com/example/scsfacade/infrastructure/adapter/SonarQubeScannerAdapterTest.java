package com.example.scsfacade.infrastructure.adapter;

import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingCategory;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import com.example.scsfacade.infrastructure.client.sonarqube.SonarQubeRestClient;
import com.example.scsfacade.infrastructure.client.sonarqube.dto.SonarQubeIssueDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * SonarQube 掃描外向適配器單元測試 (SonarQubeScannerAdapterTest)。
 * <p>
 * 驗證 SonarQubeScannerAdapter 抓取 Issues 後的嚴重性等級映射、CWE/標籤分離與領域聚合根轉換。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class SonarQubeScannerAdapterTest {

    @Mock
    private SonarQubeRestClient restClient;

    @InjectMocks
    private SonarQubeScannerAdapter adapter;

    /**
     * Feature: 抓取並解析 SonarQube 弱點報告
     * Scenario: 遠端回傳包含 Vulnerability、Bug 與 Code Smell，應精確映射等級與分類
     */
    @Test
    @DisplayName("抓取弱點 - 應正確標準化嚴重性與區分 CWE 標籤")
    void shouldFetchAndConvertIssuesProperly() {
        // Given
        SonarQubeIssueDto issue1 = new SonarQubeIssueDto(
                "key-1",
                "java:S2077",
                "BLOCKER",
                "scs-facade:src/main/OrderService.java",
                "scs-facade",
                42,
                "SQL Injection risk",
                "OPEN",
                List.of("cwe-89", "sql", "security"),
                "VULNERABILITY",
                "2026-10-14T00:00:00+0000"
        );
        SonarQubeIssueDto issue2 = new SonarQubeIssueDto(
                "key-2",
                "java:S2222",
                "CRITICAL",
                "scs-facade:src/main/LockAdapter.java",
                "scs-facade",
                10,
                "Lock release bug",
                "OPEN",
                List.of("bad-practice", "cwe_404"),
                "BUG",
                "2026-10-14T00:00:00+0000"
        );
        SonarQubeIssueDto issue3 = new SonarQubeIssueDto(
                "key-3",
                "java:S107",
                "MAJOR",
                "scs-facade:src/main/User.java",
                "scs-facade",
                5,
                "Too many params",
                "OPEN",
                List.of("design"),
                "CODE_SMELL",
                "2026-10-14T00:00:00+0000"
        );
        when(restClient.searchIssues("scs-facade", "main")).thenReturn(List.of(issue1, issue2, issue3));

        // When
        List<Finding> findings = adapter.fetchProjectIssues("scs-facade", "main", "commit-101");

        // Then
        assertThat(findings).hasSize(3);

        Finding f1 = findings.get(0);
        assertThat(f1.getSeverity()).isEqualTo(Severity.CRITICAL); // BLOCKER maps to CRITICAL
        assertThat(f1.getCategory()).isEqualTo(FindingCategory.VULNERABILITY);
        assertThat(f1.getRuleInfo().getCweList()).containsExactly("CWE-89");
        assertThat(f1.getRuleInfo().getTags()).containsExactly("sql", "security");

        Finding f2 = findings.get(1);
        assertThat(f2.getSeverity()).isEqualTo(Severity.HIGH); // CRITICAL maps to HIGH
        assertThat(f2.getCategory()).isEqualTo(FindingCategory.BUG);
        assertThat(f2.getRuleInfo().getCweList()).containsExactly("CWE-404");

        Finding f3 = findings.get(2);
        assertThat(f3.getSeverity()).isEqualTo(Severity.MEDIUM); // MAJOR maps to MEDIUM
        assertThat(f3.getCategory()).isEqualTo(FindingCategory.CODE_SMELL);
    }

    /**
     * Feature: 空結果處理
     * Scenario: 遠端回傳空清單或 null 時應回傳 emptyList
     */
    @Test
    @DisplayName("抓取弱點 - 空清單時應安全回傳 emptyList")
    void shouldReturnEmptyListWhenNoIssuesFound() {
        // Given
        when(restClient.searchIssues("proj-empty", "main")).thenReturn(Collections.emptyList());

        // When
        List<Finding> findings = adapter.fetchProjectIssues("proj-empty", "main", "commit-101");

        // Then
        assertThat(findings).isEmpty();
    }
}
