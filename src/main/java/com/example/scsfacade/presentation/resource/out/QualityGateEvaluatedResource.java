package com.example.scsfacade.presentation.resource.out;

import java.time.Instant;

/**
 * 品質門禁已評估回應載體 (QualityGateEvaluatedResource)。
 * <p>
 * 遵循 N + Ved + Resource 命名規範。
 * 供 CI/CD 流程判定通過與否之純資料載體。
 * </p>
 */
public record QualityGateEvaluatedResource(
        String projectId,
        String status,
        boolean passed,
        int criticalCount,
        int highCount,
        String message,
        Instant evaluatedAt
) {
}
