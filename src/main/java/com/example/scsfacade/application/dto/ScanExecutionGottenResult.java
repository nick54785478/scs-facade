package com.example.scsfacade.application.dto;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * 掃描執行查詢結果資料傳遞物件 (ScanExecutionGottenResult)。
 * <p>
 * 遵循 N + Gotten + Result 命名規範。
 * 嚴格作為純資料載體 (Pure Data Carrier)，內部嚴禁實作任何轉換邏輯。
 * </p>
 */
public record ScanExecutionGottenResult(
        String executionId,
        String projectId,
        String branch,
        String commitHash,
        String toolName,
        String toolVersion,
        String triggerType,
        String status,
        int totalActiveCount,
        int newCount,
        int existingCount,
        int resolvedCount,
        int criticalCount,
        int highCount,
        boolean passedQualityGate,
        Instant startedAt,
        Instant completedAt,
        String errorMessage,
        List<FindingGottenResult> findings,
        String externalTaskId
) {
    public ScanExecutionGottenResult {
        findings = findings != null ? List.copyOf(findings) : Collections.emptyList();
    }

    public ScanExecutionGottenResult(
            String executionId,
            String projectId,
            String branch,
            String commitHash,
            String toolName,
            String toolVersion,
            String triggerType,
            String status,
            int totalActiveCount,
            int newCount,
            int existingCount,
            int resolvedCount,
            int criticalCount,
            int highCount,
            boolean passedQualityGate,
            Instant startedAt,
            Instant completedAt,
            String errorMessage,
            List<FindingGottenResult> findings
    ) {
        this(
                executionId, projectId, branch, commitHash, toolName, toolVersion, triggerType, status,
                totalActiveCount, newCount, existingCount, resolvedCount, criticalCount, highCount,
                passedQualityGate, startedAt, completedAt, errorMessage, findings, null
        );
    }
}
