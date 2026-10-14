package com.example.scsfacade.presentation.resource.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 上傳掃描報告請求中繼載體 (UploadScanReportResource)。
 * <p>
 * 遵循 V + N + Resource 命名規範。
 * 嚴格作為純資料載體 (Pure Data Carrier)，不包含任何業務邏輯或轉換方法。
 * </p>
 */
@Schema(description = "上傳掃描報告 (SARIF/JSON) 中繼資料載體")
public record UploadScanReportResource(

        @Schema(description = "專案代碼 (Project Key)", example = "payment-service", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "專案代碼 (projectId) 為必填")
        String projectId,

        @Schema(description = "目標掃描分支名稱（預設為 main）", example = "main", defaultValue = "main")
        String branch,

        @Schema(description = "對應之 Git Commit 雜湊碼", example = "a1b2c3d4e5f67890123456789abcdef012345678")
        String commitHash,

        @Schema(description = "掃描工具名稱（若未填則自動自 SARIF driver 提取）", example = "Trivy")
        String toolName,

        @Schema(description = "自訂外部任務或 Pipeline 識別碼 (用於等冪防重放)", example = "ci-pipeline-58")
        String externalTaskId
) {
    public UploadScanReportResource(String projectId, String branch, String commitHash, String toolName) {
        this(projectId, branch, commitHash, toolName, null);
    }

    public UploadScanReportResource(String projectId, String branch, String commitHash) {
        this(projectId, branch, commitHash, null, null);
    }
}
