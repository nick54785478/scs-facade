package com.example.scsfacade.infrastructure.adapter;

import com.example.scsfacade.application.port.out.ScanReportParserPort;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingCategory;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingLocation;
import com.example.scsfacade.domain.finding.aggregate.vo.RuleInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SARIF 2.1.0 掃描報告解析出站適配器 (SarifReportParserAdapter)。
 * <p>
 * 遵循 ... + Adapter 命名規範。實作 ScanReportParserPort 介面。
 * 負責將符合 OASIS SARIF 2.1.0 標準之掃描結果（如 Trivy, Gitleaks, Semgrep, CodeQL）解析為標準化領域物件。
 * </p>
 */
@Component
public class SarifReportParserAdapter implements ScanReportParserPort {

    private static final Logger log = LoggerFactory.getLogger(SarifReportParserAdapter.class);
    private final ObjectMapper objectMapper;

    public SarifReportParserAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public SarifReportParserAdapter() {
        this(new ObjectMapper());
    }

    @Override
    public boolean supports(String filename, byte[] reportContent) {
        if (filename != null && (filename.toLowerCase().endsWith(".sarif") || filename.toLowerCase().endsWith(".json"))) {
            return true;
        }
        if (reportContent != null && reportContent.length > 0) {
            String snippet = new String(reportContent, 0, Math.min(reportContent.length, 512), StandardCharsets.UTF_8);
            return snippet.contains("\"runs\"") || snippet.contains("sarif");
        }
        return false;
    }

    @Override
    public ParsedScanReport parse(byte[] reportContent, String defaultToolName) {
        if (reportContent == null || reportContent.length == 0) {
            throw new IllegalArgumentException("掃描報告內容為空，無法解析");
        }

        try {
            JsonNode root = objectMapper.readTree(reportContent);
            JsonNode runs = root.path("runs");
            if (!runs.isArray() || runs.isEmpty()) {
                ScannerToolInfo emptyTool = resolveToolInfo(null, defaultToolName);
                return new ParsedScanReport(emptyTool, Collections.emptyList());
            }

            // 支援解析多個 run，累積所有弱點項目
            List<ParsedFindingItem> allItems = new ArrayList<>();
            ScannerToolInfo primaryTool = null;

            for (JsonNode run : runs) {
                JsonNode toolNode = run.path("tool").path("driver");
                ScannerToolInfo toolInfo = resolveToolInfo(toolNode, defaultToolName);
                if (primaryTool == null) {
                    primaryTool = toolInfo;
                }

                // 建立該 run 的 rules 字典庫索引
                JsonNode rulesNode = (toolNode != null && !toolNode.isMissingNode()) ? toolNode.path("rules") : null;
                Map<String, JsonNode> rulesMap = buildRulesMap(rulesNode);

                JsonNode results = run.path("results");
                if (results.isArray()) {
                    for (JsonNode result : results) {
                        ParsedFindingItem item = parseSingleResult(result, toolInfo, rulesMap);
                        if (item != null) {
                            allItems.add(item);
                        }
                    }
                }
            }

            if (primaryTool == null) {
                primaryTool = resolveToolInfo(null, defaultToolName);
            }

            log.info("成功解析 SARIF 報告: 工具={}, 總計弱點數量={}", primaryTool.getName(), allItems.size());
            return new ParsedScanReport(primaryTool, allItems);

        } catch (IOException e) {
            log.error("解析 SARIF JSON 報告失敗: {}", e.getMessage(), e);
            throw new IllegalArgumentException("無效的 SARIF 報告格式: " + e.getMessage(), e);
        }
    }

    private Map<String, JsonNode> buildRulesMap(JsonNode rulesNode) {
        Map<String, JsonNode> map = new HashMap<>();
        if (rulesNode != null && rulesNode.isArray()) {
            for (JsonNode rule : rulesNode) {
                String id = rule.path("id").asText("");
                if (!id.isEmpty()) {
                    map.put(id, rule);
                }
            }
        }
        return map;
    }

    private ParsedFindingItem parseSingleResult(JsonNode result,
                                                ScannerToolInfo toolInfo,
                                                Map<String, JsonNode> rulesMap) {
        String ruleId = result.path("ruleId").asText("UNKNOWN_RULE");
        JsonNode ruleNode = rulesMap != null ? rulesMap.get(ruleId) : null;

        String message = result.path("message").path("text").asText("");
        String level = result.path("level").asText("");
        String toolName = toolInfo != null ? toolInfo.getName() : "";

        // 解析 Severity
        Severity severity = resolveSeverity(result, ruleNode, level, toolName);

        // 解析 Category
        FindingCategory category = resolveCategory(ruleId, message, toolName, ruleNode);

        // 解析 Location
        FindingLocation location = resolveLocation(result, message);

        // 解析 RuleInfo (含 CWE 與標籤)
        RuleInfo ruleInfo = resolveRuleInfo(ruleId, ruleNode, message);

        return new ParsedFindingItem(ruleInfo, category, severity, location, result.toString());
    }

    private Severity resolveSeverity(JsonNode result, JsonNode ruleNode, String level, String toolName) {
        // 優先讀取 security-severity (CVSS 分數，常見於 CodeQL 與 Trivy)
        double cvss = extractCvssScore(result, ruleNode);
        if (cvss >= 9.0) {
            return Severity.CRITICAL;
        } else if (cvss >= 7.0) {
            return Severity.HIGH;
        } else if (cvss >= 4.0) {
            return Severity.MEDIUM;
        } else if (cvss > 0.0) {
            return Severity.LOW;
        }

        // 若無 CVSS 分數，依據 level 判定
        String normalizedLevel = level != null ? level.toLowerCase().trim() : "";
        if ("error".equals(normalizedLevel)) {
            // Gitleaks 洩漏的密鑰皆視為 CRITICAL
            if (toolName != null && toolName.toLowerCase().contains("gitleaks")) {
                return Severity.CRITICAL;
            }
            return Severity.HIGH;
        } else if ("warning".equals(normalizedLevel)) {
            return Severity.MEDIUM;
        } else if ("note".equals(normalizedLevel)) {
            return Severity.LOW;
        } else if ("none".equals(normalizedLevel)) {
            return Severity.INFO;
        }

        // 預設評級
        return Severity.MEDIUM;
    }

    private double extractCvssScore(JsonNode result, JsonNode ruleNode) {
        JsonNode scoreNode = result.path("properties").path("security-severity");
        if (scoreNode.isMissingNode() && ruleNode != null) {
            scoreNode = ruleNode.path("properties").path("security-severity");
        }
        if (!scoreNode.isMissingNode()) {
            try {
                return scoreNode.asDouble();
            } catch (Exception ignored) {
                // ignore parsing failure
            }
        }
        return -1.0;
    }

    private FindingCategory resolveCategory(String ruleId, String message, String toolName, JsonNode ruleNode) {
        String combined = (ruleId + " " + message + " " + toolName).toLowerCase();

        // 1. 密鑰洩漏
        if (combined.contains("gitleaks") || combined.contains("secret") || combined.contains("private-key")
                || combined.contains("password") || combined.contains("credential") || combined.contains("api_key")
                || combined.contains("api-key") || combined.contains("token")) {
            return FindingCategory.SECRET_LEAK;
        }

        // 2. 套件依賴弱點 (SCA)
        if (combined.contains("trivy") || ruleId.toUpperCase().startsWith("CVE-") || ruleId.toUpperCase().startsWith("GHSA-")) {
            return FindingCategory.SCA;
        }

        // 3. 一般程式碼漏洞
        if (combined.contains("vulnerability") || combined.contains("injection") || combined.contains("xss") || combined.contains("rce")) {
            return FindingCategory.VULNERABILITY;
        }

        // 4. 代碼錯誤
        if (combined.contains("bug") || combined.contains("nullpointer") || combined.contains("overflow")) {
            return FindingCategory.BUG;
        }

        // 預設為壞味道
        return FindingCategory.CODE_SMELL;
    }

    private FindingLocation resolveLocation(JsonNode result, String defaultSnippet) {
        JsonNode physicalLocation = result.path("locations").path(0).path("physicalLocation");
        String uri = physicalLocation.path("artifactLocation").path("uri").asText("unknown-file");
        if (uri.startsWith("file://")) {
            uri = uri.substring(7);
        }

        JsonNode region = physicalLocation.path("region");
        int startLine = region.path("startLine").asInt(1);
        if (startLine <= 0) {
            startLine = 1;
        }
        int endLine = region.path("endLine").asInt(startLine);
        if (endLine < startLine) {
            endLine = startLine;
        }

        String snippet = region.path("snippet").path("text").asText("");
        if (snippet.isEmpty()) {
            snippet = defaultSnippet != null && !defaultSnippet.isEmpty() ? defaultSnippet : "N/A";
        }

        return new FindingLocation(uri, startLine, endLine, snippet);
    }

    private RuleInfo resolveRuleInfo(String ruleId, JsonNode ruleNode, String defaultName) {
        String name = defaultName;
        List<String> cwes = new ArrayList<>();
        String cve = null;
        List<String> tags = new ArrayList<>();

        if (ruleNode != null) {
            String ruleName = ruleNode.path("name").asText("");
            if (ruleName.isEmpty()) {
                ruleName = ruleNode.path("shortDescription").path("text").asText("");
            }
            if (!ruleName.isEmpty()) {
                name = ruleName;
            }

            JsonNode tagsNode = ruleNode.path("properties").path("tags");
            if (tagsNode.isArray()) {
                for (JsonNode t : tagsNode) {
                    String tagText = t.asText("");
                    if (!tagText.isEmpty()) {
                        tags.add(tagText);
                        if (tagText.toUpperCase().startsWith("CWE-")) {
                            cwes.add(tagText.toUpperCase());
                        }
                    }
                }
            }
        }

        if (ruleId.toUpperCase().startsWith("CVE-")) {
            cve = ruleId.toUpperCase();
        }

        return new RuleInfo(ruleId, name != null && !name.isEmpty() ? name : ruleId, cwes, cve, tags);
    }

    private ScannerToolInfo resolveToolInfo(JsonNode driverNode, String defaultToolName) {
        String name = defaultToolName;
        String version = "1.0.0";
        if (driverNode != null) {
            String parsedName = driverNode.path("name").asText("");
            if (!parsedName.isEmpty()) {
                name = parsedName;
            }
            String parsedVersion = driverNode.path("semanticVersion").asText("");
            if (parsedVersion.isEmpty()) {
                parsedVersion = driverNode.path("version").asText("");
            }
            if (!parsedVersion.isEmpty()) {
                version = parsedVersion;
            }
        }

        if (name == null || name.trim().isEmpty()) {
            name = "SARIF Scanner";
        }

        String category = "STATIC_ANALYSIS";
        String lowerName = name.toLowerCase();
        if (lowerName.contains("trivy")) {
            category = "SCA";
        } else if (lowerName.contains("gitleaks")) {
            category = "SECRET";
        } else if (lowerName.contains("semgrep") || lowerName.contains("codeql")) {
            category = "SAST";
        }

        return new ScannerToolInfo(name, version, category);
    }
}
