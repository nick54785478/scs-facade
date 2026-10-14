package com.example.scsfacade.presentation.resource.in;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 接收 SonarQube 原生 Webhook 請求載體 (ReceiveSonarQubeWebhookResource)。
 * <p>
 * 遵循 V + N + Resource 命名規範。
 * Mapping SonarQube Compute Engine 分析完成後推播之原生 Webhook JSON Payload。
 * 嚴格作為純資料載體，不包含任何轉換邏輯。
 * </p>
 */
@Schema(description = "SonarQube Compute Engine 原生推播 Webhook Payload 載體")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReceiveSonarQubeWebhookResource(

        @Schema(description = "SonarQube 伺服器根 URL", example = "http://localhost:9000")
        String serverUrl,

        @Schema(description = "SonarQube Compute Engine 任務 ID", example = "AX-3k8d9QYw001")
        String taskId,

        @Schema(description = "任務狀態 (SUCCESS / FAILED)", example = "SUCCESS")
        String status,

        @Schema(description = "分析完成時間戳記 (ISO-8601)", example = "2026-10-14T00:15:00+0800")
        String analysedAt,

        @Schema(description = "源碼版本 Commit 雜湊碼", example = "a1b2c3d4e5f67890123456789abcdef012345678")
        String revision,

        @Schema(description = "SonarQube 專案基本資訊")
        ProjectNode project,

        @Schema(description = "SonarQube 分析分支資訊")
        BranchNode branch,

        @Schema(description = "品質門禁判定快照")
        QualityGateNode qualityGate
) {

    @Schema(description = "SonarQube 專案基本節點")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProjectNode(
            @Schema(description = "專案代碼 (Project Key)", example = "scs-facade")
            String key,

            @Schema(description = "專案名稱", example = "SCS Facade Service")
            String name,

            @Schema(description = "專案儀表板 URL", example = "http://localhost:9000/dashboard?id=scs-facade")
            String url
    ) {}

    @Schema(description = "SonarQube 分支節點")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BranchNode(
            @Schema(description = "分支名稱", example = "main")
            String name,

            @Schema(description = "分支類型 (BRANCH / PULL_REQUEST)", example = "BRANCH")
            String type,

            @Schema(description = "是否為預設主要分支", example = "true")
            Boolean isMain,

            @Schema(description = "該分支專屬 URL", example = "http://localhost:9000/dashboard?id=scs-facade&branch=main")
            String url
    ) {}

    @Schema(description = "品質門禁節點")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QualityGateNode(
            @Schema(description = "門禁規則名稱", example = "Sonar way")
            String name,

            @Schema(description = "門禁結果狀態 (OK / ERROR)", example = "OK")
            String status
    ) {}
}
