package com.example.scsfacade.presentation.resource.in;

import jakarta.validation.constraints.NotBlank;

/**
 * 評估品質門禁請求載體 (EvaluateQualityGateResource)。
 * <p>
 * 遵循 V + N + Resource 命名規範。
 * 嚴格作為純資料載體，不包含任何業務或轉換邏輯。
 * </p>
 */
public record EvaluateQualityGateResource(
        @NotBlank(message = "專案代碼 (projectId) 為必填")
        String projectId,
        Integer maxCritical,
        Integer maxHigh,
        Boolean blockOnSecrets
) {
}
