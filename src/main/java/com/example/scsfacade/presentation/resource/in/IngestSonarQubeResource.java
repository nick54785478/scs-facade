package com.example.scsfacade.presentation.resource.in;

import jakarta.validation.constraints.NotBlank;

/**
 * 匯入 SonarQube 掃描請求載體 (IngestSonarQubeResource)。
 * <p>
 * 遵循 V + N + Resource 命名規範。
 * 依據規範，此物件嚴格作為純資料載體 (Pure Data Carrier)，嚴禁在內部實作 toCommand 等任何轉換邏輯。
 * 轉換工作交由 presentation/assembler/ 套件的 Assembler 獨立負責。
 * </p>
 */
public record IngestSonarQubeResource(
        @NotBlank(message = "專案代碼 (projectId) 為必填")
        String projectId,
        String branch,
        String commitHash
) {
}
