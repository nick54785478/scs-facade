package com.example.scsfacade.application.dto;

import java.time.Instant;

/**
 * 品質門禁評估結果 DTO (QualityGateEvaluatedResult)。
 * <p>
 * 遵循 N + Gotten/Searched/Evaluated + Result 命名規範。
 * 嚴格作為純資料載體，不包含任何轉換方法。
 * </p>
 */
public record QualityGateEvaluatedResult(
        String projectId,
        String status,
        boolean passed,
        int criticalCount,
        int highCount,
        String message,
        Instant evaluatedAt
) {
}
