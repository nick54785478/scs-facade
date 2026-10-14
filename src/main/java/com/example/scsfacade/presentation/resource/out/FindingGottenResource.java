package com.example.scsfacade.presentation.resource.out;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * 弱點已取得結果回應載體 (FindingGottenResource)。
 * <p>
 * 遵循 N + Gotten/Searched + Resource 命名規範。
 * 嚴格作為純資料載體，不包含任何轉換邏輯。
 * </p>
 */
@Schema(description = "弱點/掃描發現項目詳細資料回應載體")
public record FindingGottenResource(
        @Schema(description = "弱點唯一識別碼 (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String findingId,

        @Schema(description = "專案識別碼", example = "my-backend-service")
        String projectId,

        @Schema(description = "分支名稱", example = "main")
        String branch,

        @Schema(description = "最新關聯 Git Commit SHA", example = "a1b2c3d4e5f67890123456789abcdef012345678")
        String commitHash,

        @Schema(description = "首次發現之 Git Commit SHA", example = "a1b2c3d4e5f67890123456789abcdef012345678")
        String firstSeenCommit,

        @Schema(description = "最後觀察到之 Git Commit SHA", example = "b2c3d4e5f67890123456789abcdef012345678a")
        String lastSeenCommit,

        @Schema(description = "修復確認之 Git Commit SHA (若已修復)", example = "c3d4e5f67890123456789abcdef012345678ab")
        String resolvedInCommit,

        @Schema(description = "跨 Commit 追蹤之程式碼指紋 (Fingerprint)", example = "c2b3d4e5f6...")
        String fingerprint,

        @Schema(description = "掃描工具名稱", example = "SonarQube")
        String toolName,

        @Schema(description = "工具分類 (SAST, SCA, DAST, SECRET)", example = "SAST")
        String toolCategory,

        @Schema(description = "弱點類別 (SECURITY_VULNERABILITY, CODE_SMELL, BUG)", example = "SECURITY_VULNERABILITY")
        String category,

        @Schema(description = "規則識別碼", example = "java:S2077")
        String ruleId,

        @Schema(description = "規則名稱/描述", example = "SQL queries should not be vulnerable to injection attacks")
        String ruleName,

        @Schema(description = "關聯 CWE 清單", example = "[\"CWE-89\"]")
        List<String> cweList,

        @Schema(description = "關聯 CVE 編號 (若為已知漏洞)", example = "CVE-2023-12345")
        String cve,

        @Schema(description = "標籤清單", example = "[\"security\", \"sql-injection\"]")
        List<String> tags,

        @Schema(description = "嚴重度層級 (CRITICAL, HIGH, MEDIUM, LOW, INFO)", example = "CRITICAL")
        String severity,

        @Schema(description = "弱點原始碼檔案路徑", example = "src/main/java/com/example/UserService.java")
        String filePath,

        @Schema(description = "弱點起始行號", example = "42")
        Integer startLine,

        @Schema(description = "弱點結束行號", example = "45")
        Integer endLine,

        @Schema(description = "問題程式碼片段", example = "String query = \"SELECT * FROM users WHERE name = '\" + input + \"'\";")
        String snippet,

        @Schema(description = "生命週期狀態 (NEW, EXISTING, RESOLVED, SUPPRESSED, FALSE_POSITIVE)", example = "NEW")
        String status,

        @Schema(description = "首次發現時間戳記 (ISO-8601)", example = "2026-10-14T00:00:00Z")
        Instant firstSeenAt,

        @Schema(description = "最後觀察時間戳記 (ISO-8601)", example = "2026-10-14T01:00:00Z")
        Instant lastSeenAt,

        @Schema(description = "修復時間戳記 (若已修復，ISO-8601)", example = "2026-10-14T02:00:00Z")
        Instant resolvedAt
) {
}
