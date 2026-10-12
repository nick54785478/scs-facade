package com.example.scsfacade.presentation.resource.out;

import java.util.List;

/**
 * SonarQube 已匯入結果回應載體 (SonarQubeIngestedResource)。
 * <p>
 * 遵循 N + Ved + Resource 命名規範。
 * 依據規範，此物件嚴格作為純資料載體，不包含任何轉換邏輯。
 * </p>
 */
public record SonarQubeIngestedResource(
        String projectId,
        int totalIngested,
        List<FindingGottenResource> findings
) {
}
