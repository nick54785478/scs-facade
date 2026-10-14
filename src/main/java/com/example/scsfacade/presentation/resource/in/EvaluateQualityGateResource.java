package com.example.scsfacade.presentation.resource.in;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 評估品質門禁請求載體 (EvaluateQualityGateResource)。
 * <p>
 * 遵循 V + N + Resource 命名規範。
 * 嚴格作為純資料載體，不包含任何業務或轉換邏輯。
 * </p>
 */
@Schema(description = "品質門禁評估請求載體")
public record EvaluateQualityGateResource(

        @Schema(description = "專案唯一識別代碼", example = "scs-backend", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "專案代碼 (projectId) 為必填")
        String projectId,

        @Schema(description = "允許之嚴重等級 (Critical) 弱點上限（預設 0）", example = "0", defaultValue = "0")
        Integer maxCritical,

        @Schema(description = "允許之高危險等級 (High) 弱點上限（預設 2）", example = "2", defaultValue = "2")
        Integer maxHigh,

        @Schema(description = "是否在偵測到機敏資訊外洩時強制阻擋（預設 true）", example = "true", defaultValue = "true")
        Boolean blockOnSecrets
) {
}
