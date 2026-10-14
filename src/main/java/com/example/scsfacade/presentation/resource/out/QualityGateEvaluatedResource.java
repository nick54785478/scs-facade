package com.example.scsfacade.presentation.resource.out;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 品質門禁已評估回應載體 (QualityGateEvaluatedResource)。
 * <p>
 * 遵循 N + Ved + Resource 命名規範。
 * 供 CI/CD 流程判定通過與否之純資料載體。
 * </p>
 */
@Schema(description = "品質門禁已評估結果回應載體")
public record QualityGateEvaluatedResource(
        @Schema(description = "專案識別碼", example = "my-backend-service")
        String projectId,

        @Schema(description = "門禁判定狀態 (PASSED, FAILED, WARNING, ERROR)", example = "PASSED")
        String status,

        @Schema(description = "是否通過品質門禁 (true: 通過 / false: 未通過)", example = "true")
        boolean passed,

        @Schema(description = "Critical 嚴重等級弱點數量", example = "0")
        int criticalCount,

        @Schema(description = "High 嚴重等級弱點數量", example = "1")
        int highCount,

        @Schema(description = "門禁評估結果說明訊息", example = "Quality gate passed with 0 critical and 1 high issues.")
        String message,

        @Schema(description = "評估時間戳記 (ISO-8601)", example = "2026-10-14T01:00:00Z")
        Instant evaluatedAt
) {
}
