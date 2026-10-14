package com.example.scsfacade.presentation.resource.out;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * 掃描執行已取得結果回應載體 (ScanExecutionGottenResource)。
 * <p>
 * 遵循 N + Gotten + Resource 命名規範。
 * </p>
 */
@Schema(description = "掃描執行紀錄已取得結果詳細回應載體")
public record ScanExecutionGottenResource(
        @Schema(description = "掃描執行紀錄識別碼 (UUID)", example = "4ba96075-6828-4673-c40d-3d074077bfb7")
        String executionId,

        @Schema(description = "專案識別碼", example = "my-backend-service")
        String projectId,

        @Schema(description = "掃描分支名稱", example = "main")
        String branch,

        @Schema(description = "掃描之 Git Commit SHA", example = "a1b2c3d4e5f67890123456789abcdef012345678")
        String commitHash,

        @Schema(description = "掃描工具名稱", example = "SonarQube")
        String toolName,

        @Schema(description = "工具版本", example = "10.4")
        String toolVersion,

        @Schema(description = "觸發方式 (WEBHOOK, MANUAL, SCHEDULED, PIPELINE)", example = "WEBHOOK")
        String triggerType,

        @Schema(description = "執行狀態 (STARTED, SUCCESS, FAILED, TIMEOUT, CANCELLED)", example = "SUCCESS")
        String status,

        @Schema(description = "總現存未解決弱點數", example = "12")
        int totalActiveCount,

        @Schema(description = "本次新增弱點數", example = "3")
        int newCount,

        @Schema(description = "既有未解決弱點數", example = "9")
        int existingCount,

        @Schema(description = "本次已修復弱點數", example = "2")
        int resolvedCount,

        @Schema(description = "Critical 嚴重等級弱點數量", example = "0")
        int criticalCount,

        @Schema(description = "High 嚴重等級弱點數量", example = "2")
        int highCount,

        @Schema(description = "是否通過品質門禁 (true: 通過 / false: 未通過)", example = "true")
        boolean passedQualityGate,

        @Schema(description = "掃描開始時間戳記 (ISO-8601)", example = "2026-10-14T01:00:00Z")
        Instant startedAt,

        @Schema(description = "掃描完成時間戳記 (ISO-8601)", example = "2026-10-14T01:05:00Z")
        Instant completedAt,

        @Schema(description = "錯誤訊息 (若失敗)", example = "null")
        String errorMessage,

        @Schema(description = "本次掃描發現的弱點清單")
        List<FindingGottenResource> findings,

        @Schema(description = "外部任務識別碼或冪等鍵 (如 SonarQube taskId)", example = "AW70wU7P-uK7q_qGZ8Pq")
        String externalTaskId
) {
    public ScanExecutionGottenResource(
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
            List<FindingGottenResource> findings
    ) {
        this(
                executionId, projectId, branch, commitHash, toolName, toolVersion, triggerType, status,
                totalActiveCount, newCount, existingCount, resolvedCount, criticalCount, highCount,
                passedQualityGate, startedAt, completedAt, errorMessage, findings, null
        );
    }
}
