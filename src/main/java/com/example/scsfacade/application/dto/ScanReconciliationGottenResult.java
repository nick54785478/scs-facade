package com.example.scsfacade.application.dto;

import java.time.Instant;

/**
 * 掃描對帳已取得結果 DTO (ScanReconciliationGottenResult)。
 * <p>
 * 遵循 N + Gotten + Result 命名規範。
 * 嚴格作為純資料載體，不包含任何業務邏輯或轉換方法。
 * </p>
 */
public record ScanReconciliationGottenResult(
        String projectId,
        String branch,
        String toolName,
        int remoteActiveCount,
        int localActiveCount,
        int reconciledResolvedCount,
        int reconciledAppendedCount,
        int severityAdjustedCount,
        boolean passedQualityGate,
        Instant executedAt
) {
}
