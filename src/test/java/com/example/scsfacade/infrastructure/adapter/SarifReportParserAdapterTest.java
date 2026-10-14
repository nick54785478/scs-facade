package com.example.scsfacade.infrastructure.adapter;

import com.example.scsfacade.application.port.out.ScanReportParserPort.ParsedFindingItem;
import com.example.scsfacade.application.port.out.ScanReportParserPort.ParsedScanReport;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingCategory;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SARIF 報告解析出站適配器單元測試 (SarifReportParserAdapterTest)。
 */
class SarifReportParserAdapterTest {

    private SarifReportParserAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SarifReportParserAdapter(new ObjectMapper());
    }

    /**
     * Feature: 檔案格式支援性判定
     * Scenario: 驗證 supports 方法對 sarif, json 延伸檔名與內文關鍵字之辨識
     * Given: 包含 .sarif, .json 及一般檔名
     * When: 呼叫 supports()
     * Then: 正確判定是否支援
     */
    @Test
    @DisplayName("格式支援判定 - 應正確識別 sarif 與 json 檔案")
    void shouldCheckSupportsCorrectly() {
        assertThat(adapter.supports("trivy.sarif", new byte[0])).isTrue();
        assertThat(adapter.supports("report.JSON", new byte[0])).isTrue();
        assertThat(adapter.supports("report.xml", "{\"runs\": []}".getBytes(StandardCharsets.UTF_8))).isTrue();
        assertThat(adapter.supports("report.txt", "hello".getBytes(StandardCharsets.UTF_8))).isFalse();
    }

    /**
     * Feature: Trivy SCA 弱點報告解析
     * Scenario: 解析包含 CVE 與套件漏洞之 Trivy SARIF
     * Given: 標準 Trivy SARIF JSON
     * When: 呼叫 parse()
     * Then: 成功提取工具名稱 Trivy, Category 為 SCA, Severity 與 Location 正確
     */
    @Test
    @DisplayName("Trivy 解析 - 應正確解析 SCA 漏洞、CVE 與嚴重等級")
    void shouldParseTrivySarifReport() {
        // Given
        String trivySarif = """
                {
                  "version": "2.1.0",
                  "$schema": "https://json.schemastore.org/sarif-2.1.0.json",
                  "runs": [
                    {
                      "tool": {
                        "driver": {
                          "name": "Trivy",
                          "version": "0.49.1",
                          "rules": [
                            {
                              "id": "CVE-2023-44487",
                              "name": "HTTP/2 Rapid Reset Attack",
                              "properties": {
                                "tags": ["CWE-400", "security"],
                                "security-severity": "7.5"
                              }
                            }
                          ]
                        }
                      },
                      "results": [
                        {
                          "ruleId": "CVE-2023-44487",
                          "level": "error",
                          "message": { "text": "HTTP/2 Rapid Reset vulnerability in netty-codec-http2" },
                          "locations": [
                            {
                              "physicalLocation": {
                                "artifactLocation": { "uri": "pom.xml" },
                                "region": {
                                  "startLine": 45,
                                  "endLine": 48,
                                  "snippet": { "text": "<artifactId>netty-codec-http2</artifactId>" }
                                }
                              }
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        // When
        ParsedScanReport report = adapter.parse(trivySarif.getBytes(StandardCharsets.UTF_8), "Trivy");

        // Then
        assertThat(report.toolInfo().getName()).isEqualTo("Trivy");
        assertThat(report.toolInfo().getVersion()).isEqualTo("0.49.1");
        assertThat(report.toolInfo().getCategory()).isEqualTo("SCA");

        assertThat(report.items()).hasSize(1);
        ParsedFindingItem item = report.items().get(0);
        assertThat(item.ruleInfo().getRuleId()).isEqualTo("CVE-2023-44487");
        assertThat(item.ruleInfo().getCve()).isEqualTo("CVE-2023-44487");
        assertThat(item.ruleInfo().getCweList()).contains("CWE-400");
        assertThat(item.category()).isEqualTo(FindingCategory.SCA);
        assertThat(item.severity()).isEqualTo(Severity.HIGH); // 7.5 -> HIGH
        assertThat(item.location().getFilePath()).isEqualTo("pom.xml");
        assertThat(item.location().getStartLine()).isEqualTo(45);
        assertThat(item.location().getSnippet()).contains("netty-codec-http2");
    }

    /**
     * Feature: Gitleaks 憑證洩漏報告解析
     * Scenario: 解析包含 API 金鑰之 Gitleaks SARIF 報告
     * Given: Gitleaks SARIF JSON
     * When: 呼叫 parse()
     * Then: 工具名稱為 Gitleaks, Category 為 SECRET_LEAK, 嚴重度為 CRITICAL
     */
    @Test
    @DisplayName("Gitleaks 解析 - 應正確分類為 SECRET_LEAK 且評級為 CRITICAL")
    void shouldParseGitleaksSarifReport() {
        // Given
        String gitleaksSarif = """
                {
                  "version": "2.1.0",
                  "runs": [
                    {
                      "tool": {
                        "driver": {
                          "name": "gitleaks",
                          "version": "8.18.2",
                          "rules": [
                            {
                              "id": "generic-api-key",
                              "name": "Generic API Key Leak"
                            }
                          ]
                        }
                      },
                      "results": [
                        {
                          "ruleId": "generic-api-key",
                          "level": "error",
                          "message": { "text": "Discovered hardcoded API key in application.properties" },
                          "locations": [
                            {
                              "physicalLocation": {
                                "artifactLocation": { "uri": "file://src/main/resources/application.properties" },
                                "region": {
                                  "startLine": 12,
                                  "snippet": { "text": "api.key=AKIAIOSFODNN7EXAMPLE" }
                                }
                              }
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        // When
        ParsedScanReport report = adapter.parse(gitleaksSarif.getBytes(StandardCharsets.UTF_8), "Gitleaks");

        // Then
        assertThat(report.toolInfo().getName()).isEqualTo("gitleaks");
        assertThat(report.toolInfo().getCategory()).isEqualTo("SECRET");

        assertThat(report.items()).hasSize(1);
        ParsedFindingItem item = report.items().get(0);
        assertThat(item.category()).isEqualTo(FindingCategory.SECRET_LEAK);
        assertThat(item.severity()).isEqualTo(Severity.CRITICAL);
        assertThat(item.location().getFilePath()).isEqualTo("src/main/resources/application.properties");
        assertThat(item.location().getStartLine()).isEqualTo(12);
    }

    /**
     * Feature: Semgrep SAST 程式碼弱點解析
     * Scenario: 解析 Semgrep CVSS >= 9.0 嚴重漏洞與警告等級
     * Given: Semgrep SARIF JSON
     * When: 呼叫 parse()
     * Then: 正確解析為 SAST 工具與對應等級
     */
    @Test
    @DisplayName("Semgrep 解析 - 應正確解析 CVSS 評分並對齊 Severity 與 Category")
    void shouldParseSemgrepSarifReport() {
        // Given
        String semgrepSarif = """
                {
                  "runs": [
                    {
                      "tool": {
                        "driver": {
                          "name": "semgrep",
                          "version": "1.60.0",
                          "rules": [
                            {
                              "id": "java.lang.security.audit.rce",
                              "properties": {
                                "security-severity": "9.5"
                              }
                            },
                            {
                              "id": "java.style.unused-import"
                            }
                          ]
                        }
                      },
                      "results": [
                        {
                          "ruleId": "java.lang.security.audit.rce",
                          "level": "error",
                          "message": { "text": "Potential remote code execution via Runtime.exec" },
                          "locations": [
                            {
                              "physicalLocation": {
                                "artifactLocation": { "uri": "src/Runner.java" },
                                "region": { "startLine": 20 }
                              }
                            }
                          ]
                        },
                        {
                          "ruleId": "java.style.unused-import",
                          "level": "note",
                          "message": { "text": "Unused import detected" },
                          "locations": []
                        }
                      ]
                    }
                  ]
                }
                """;

        // When
        ParsedScanReport report = adapter.parse(semgrepSarif.getBytes(StandardCharsets.UTF_8), "Semgrep");

        // Then
        assertThat(report.toolInfo().getCategory()).isEqualTo("SAST");
        assertThat(report.items()).hasSize(2);

        ParsedFindingItem rceItem = report.items().get(0);
        assertThat(rceItem.severity()).isEqualTo(Severity.CRITICAL); // 9.5 -> CRITICAL
        assertThat(rceItem.category()).isEqualTo(FindingCategory.VULNERABILITY);

        ParsedFindingItem styleItem = report.items().get(1);
        assertThat(styleItem.severity()).isEqualTo(Severity.LOW); // note -> LOW
        assertThat(styleItem.category()).isEqualTo(FindingCategory.CODE_SMELL);
    }

    /**
     * Feature: 空報告與異常防護
     * Scenario: 傳入空 runs 或無效 JSON
     * Given: 空資料或毀損 JSON
     * When: 呼叫 parse()
     * Then: 正確處理空清單或拋出 IllegalArgumentException
     */
    @Test
    @DisplayName("異常防護 - 空報告或無效 JSON 應妥善處理")
    void shouldHandleEmptyOrInvalidSarif() {
        // Empty content
        assertThatThrownBy(() -> adapter.parse(new byte[0], "Tool"))
                .isInstanceOf(IllegalArgumentException.class);

        // Invalid JSON
        assertThatThrownBy(() -> adapter.parse("not a json".getBytes(StandardCharsets.UTF_8), "Tool"))
                .isInstanceOf(IllegalArgumentException.class);

        // Empty runs
        ParsedScanReport emptyReport = adapter.parse("{\"runs\": []}".getBytes(StandardCharsets.UTF_8), "MyTool");
        assertThat(emptyReport.items()).isEmpty();
        assertThat(emptyReport.toolInfo().getName()).isEqualTo("MyTool");
    }
}
