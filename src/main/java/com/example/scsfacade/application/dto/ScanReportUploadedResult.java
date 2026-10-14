package com.example.scsfacade.application.dto;

import java.time.Instant;

/**
 * 掃描報告上傳結算結果 DTO (ScanReportUploadedResult)。
 * <p>
 * 遵循 N + Ved + Result 命名規範。
 * 嚴格作為純資料載體，不包含任何業務轉換邏輯。
 * </p>
 */
public record ScanReportUploadedResult(
        String scanExecutionId,
        String projectId,
        String branch,
        String commitHash,
        String toolName,
        String status,
        boolean passedQualityGate,
        int totalActiveCount,
        int newCount,
        int existingCount,
        int resolvedCount,
        int criticalCount,
        int highCount,
        Instant completedAt
) {
}
