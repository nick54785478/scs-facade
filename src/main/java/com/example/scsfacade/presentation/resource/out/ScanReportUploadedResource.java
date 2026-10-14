package com.example.scsfacade.presentation.resource.out;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 掃描報告已上傳結算結果回應載體 (ScanReportUploadedResource)。
 * <p>
 * 遵循 N + Ved + Resource 命名規範。
 * 嚴格作為純資料載體，不包含任何業務邏輯或轉換方法。
 * </p>
 */
@Schema(description = "掃描報告已上傳結算結果回應載體")
public record ScanReportUploadedResource(

        @Schema(description = "掃描批次執行識別碼 (UUID)", example = "4ba96075-6828-4673-c40d-3d074077bfb7")
        String scanExecutionId,

        @Schema(description = "專案代碼", example = "payment-service")
        String projectId,

        @Schema(description = "掃描分支", example = "main")
        String branch,

        @Schema(description = "對應之 Git Commit 雜湊", example = "a1b2c3d4e5f67890123456789abcdef012345678")
        String commitHash,

        @Schema(description = "掃描工具名稱", example = "Trivy")
        String toolName,

        @Schema(description = "執行狀態 (SUCCESS, FAILED 等)", example = "SUCCESS")
        String status,

        @Schema(description = "品質門禁判定是否合格", example = "true")
        boolean passedQualityGate,

        @Schema(description = "總現存活躍弱點數", example = "10")
        int totalActiveCount,

        @Schema(description = "本次新增弱點數", example = "2")
        int newCount,

        @Schema(description = "既有未解決弱點數", example = "8")
        int existingCount,

        @Schema(description = "本次已修復弱點數", example = "1")
        int resolvedCount,

        @Schema(description = "嚴重等級 (CRITICAL) 弱點數", example = "0")
        int criticalCount,

        @Schema(description = "高危險等級 (HIGH) 弱點數", example = "1")
        int highCount,

        @Schema(description = "結算完成時間戳記", example = "2026-10-15T00:00:00Z")
        Instant completedAt
) {
}
