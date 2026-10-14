package com.example.scsfacade.presentation.resource.out;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 掃描對帳結算完成回應載體 (ScanReconciledResource)。
 * <p>
 * 遵循 N + Ved + Resource 命名規範。
 * 嚴格作為純資料載體，不包含任何業務邏輯或轉換方法。
 * </p>
 */
@Schema(description = "掃描對帳結算完成回應載體")
public record ScanReconciledResource(

        @Schema(description = "專案代碼", example = "payment-service")
        String projectId,

        @Schema(description = "掃描分支", example = "main")
        String branch,

        @Schema(description = "比對之掃描工具名稱", example = "SonarQube")
        String toolName,

        @Schema(description = "外部伺服器最新未解決弱點數", example = "15")
        int remoteActiveCount,

        @Schema(description = "本地原本活躍之弱點數", example = "16")
        int localActiveCount,

        @Schema(description = "本次補償標記為已修復之弱點數 (遠端已關閉但本地仍開)", example = "2")
        int reconciledResolvedCount,

        @Schema(description = "本次補錄遺漏之弱點數 (遠端存在但本地漏接)", example = "1")
        int reconciledAppendedCount,

        @Schema(description = "本次同步調整嚴重性等級之弱點數", example = "0")
        int severityAdjustedCount,

        @Schema(description = "品質門禁判定是否合格", example = "true")
        boolean passedQualityGate,

        @Schema(description = "對帳執行完成時間戳記", example = "2026-10-15T02:00:15Z")
        Instant executedAt
) {
}
