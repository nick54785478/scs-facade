package com.example.scsfacade.infrastructure.adapter;

import com.example.scsfacade.application.port.out.SonarQubeScannerPort;
import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingLocation;
import com.example.scsfacade.domain.finding.aggregate.vo.RuleInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import com.example.scsfacade.infrastructure.client.sonarqube.SonarQubeRestClient;
import com.example.scsfacade.infrastructure.client.sonarqube.dto.SonarQubeIssueDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SonarQube 掃描器外向適配器 (SonarQubeScannerAdapter)。
 * <p>
 * 遵循 ... + Adapter 命名規範。實作 SonarQubeScannerPort。
 * 職責：
 * 1. 呼叫 SonarQubeRestClient 抓取遠端原始 Issues。
 * 2. 執行嚴重性等級標準化 (Severity Normalization)。
 * 3. 轉換封裝為 Finding 領域聚合根。
 * </p>
 */
@Component
public class SonarQubeScannerAdapter implements SonarQubeScannerPort {

    private final SonarQubeRestClient restClient;

    public SonarQubeScannerAdapter(SonarQubeRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<Finding> fetchProjectIssues(String projectId, String branch, String commitHash) {
        List<SonarQubeIssueDto> issues = restClient.searchIssues(projectId, branch);
        if (issues == null || issues.isEmpty()) {
            return Collections.emptyList();
        }

        ScannerToolInfo toolInfo = new ScannerToolInfo("SonarQube", "10.x", "SAST");
        List<Finding> findings = new ArrayList<>();

        for (SonarQubeIssueDto issue : issues) {
            Severity canonicalSeverity = mapSeverity(issue.severity());
            RuleInfo ruleInfo = new RuleInfo(
                    issue.rule() != null ? issue.rule() : "unknown-rule",
                    issue.message() != null ? issue.message() : "",
                    issue.tags(),
                    null
            );

            String filePath = extractFilePath(issue.component());
            FindingLocation location = new FindingLocation(
                    filePath,
                    issue.line(),
                    issue.line(),
                    issue.message()
            );

            Finding finding = Finding.createNew(
                    projectId,
                    branch != null ? branch : "main",
                    commitHash != null ? commitHash : "",
                    toolInfo,
                    ruleInfo,
                    canonicalSeverity,
                    location,
                    issue.key() != null ? String.format("{\"sonarIssueKey\":\"%s\"}", issue.key()) : "{}"
            );

            findings.add(finding);
        }

        return findings;
    }

    /**
     * 標準化嚴重程度等級映射矩陣。
     *
     * @param sonarSeverity SonarQube 原生嚴重等級
     * @return 領域標準 Severity
     */
    public Severity mapSeverity(String sonarSeverity) {
        if (sonarSeverity == null) {
            return Severity.INFO;
        }
        return switch (sonarSeverity.trim().toUpperCase()) {
            case "BLOCKER" -> Severity.CRITICAL;
            case "CRITICAL" -> Severity.HIGH;
            case "MAJOR" -> Severity.MEDIUM;
            case "MINOR" -> Severity.LOW;
            default -> Severity.INFO;
        };
    }

    private String extractFilePath(String component) {
        if (component == null) {
            return "unknown-file";
        }
        int colonIdx = component.indexOf(':');
        return colonIdx >= 0 ? component.substring(colonIdx + 1) : component;
    }
}
