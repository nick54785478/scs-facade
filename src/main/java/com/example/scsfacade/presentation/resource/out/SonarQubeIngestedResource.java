package com.example.scsfacade.presentation.resource.out;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * SonarQube 已匯入結果回應載體 (SonarQubeIngestedResource)。
 * <p>
 * 遵循 N + Ved + Resource 命名規範。
 * 依據規範，此物件嚴格作為純資料載體，不包含任何轉換邏輯。
 * </p>
 */
@Schema(description = "SonarQube 掃描結果匯入成功回應載體")
public record SonarQubeIngestedResource(
        @Schema(description = "掃描執行紀錄識別碼 (UUID)", example = "4ba96075-6828-4673-c40d-3d074077bfb7")
        String executionId,

        @Schema(description = "專案識別碼", example = "my-backend-service")
        String projectId,

        @Schema(description = "分支名稱", example = "main")
        String branch,

        @Schema(description = "Git Commit SHA", example = "a1b2c3d4e5f67890123456789abcdef012345678")
        String commitHash,

        @Schema(description = "執行狀態", example = "SUCCESS")
        String status,

        @Schema(description = "是否通過品質門禁", example = "true")
        boolean passedQualityGate,

        @Schema(description = "總現存未解決弱點數", example = "10")
        int totalActiveCount,

        @Schema(description = "本次新增弱點數", example = "2")
        int newCount,

        @Schema(description = "既有未解決弱點數", example = "8")
        int existingCount,

        @Schema(description = "本次修復弱點數", example = "1")
        int resolvedCount,

        @Schema(description = "Critical 嚴重等級弱點數量", example = "0")
        int criticalCount,

        @Schema(description = "High 嚴重等級弱點數量", example = "1")
        int highCount,

        @Schema(description = "關聯之弱點清單")
        List<FindingGottenResource> findings
) {
}
