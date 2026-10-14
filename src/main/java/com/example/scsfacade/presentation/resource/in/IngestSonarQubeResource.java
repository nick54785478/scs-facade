package com.example.scsfacade.presentation.resource.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 匯入 SonarQube 掃描請求載體 (IngestSonarQubeResource)。
 * <p>
 * 遵循 V + N + Resource 命名規範。
 * 依據規範，此物件嚴格作為純資料載體 (Pure Data Carrier)，嚴禁在內部實作 toCommand 等任何轉換邏輯。
 * 轉換工作交由 presentation/assembler/ 套件的 Assembler 獨立負責。
 * </p>
 */
@Schema(description = "主動匯入 SonarQube 掃描結果請求載體")
public record IngestSonarQubeResource(

        @Schema(description = "SonarQube 專案代碼 (Project Key)", example = "scs-facade", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "專案代碼 (projectId) 為必填")
        String projectId,

        @Schema(description = "目標掃描分支名稱（預設為 main）", example = "main", defaultValue = "main")
        String branch,

        @Schema(description = "對應之 Git Commit 雜湊碼", example = "a1b2c3d4e5f67890123456789abcdef012345678")
        String commitHash,

        @Schema(description = "自訂冪等性或外部任務識別碼 (Idempotency Key)", example = "idemp-20261014-001")
        String idempotencyKey
) {
    public IngestSonarQubeResource(String projectId, String branch, String commitHash) {
        this(projectId, branch, commitHash, null);
    }
}
