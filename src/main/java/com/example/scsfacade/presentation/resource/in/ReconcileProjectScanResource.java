package com.example.scsfacade.presentation.resource.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 專案掃描對帳請求中繼載體 (ReconcileProjectScanResource)。
 * <p>
 * 遵循 V + N + Resource 命名規範。
 * 嚴格作為純資料載體 (Pure Data Carrier)，不包含任何業務邏輯或轉換方法。
 * </p>
 */
@Schema(description = "專案掃描對帳請求中繼資料載體")
public record ReconcileProjectScanResource(

        @Schema(description = "專案代碼 (Project Key)", example = "payment-service", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "專案代碼 (projectId) 為必填")
        String projectId,

        @Schema(description = "目標掃描分支名稱（預設為 main）", example = "main", defaultValue = "main")
        String branch,

        @Schema(description = "是否強制重新同步", example = "false", defaultValue = "false")
        Boolean forceSync
) {
    public ReconcileProjectScanResource(String projectId, String branch) {
        this(projectId, branch, false);
    }

    public ReconcileProjectScanResource(String projectId) {
        this(projectId, "main", false);
    }
}
