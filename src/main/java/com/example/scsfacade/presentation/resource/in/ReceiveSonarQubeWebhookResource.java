package com.example.scsfacade.presentation.resource.in;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 接收 SonarQube 原生 Webhook 請求載體 (ReceiveSonarQubeWebhookResource)。
 * <p>
 * 遵循 V + N + Resource 命名規範。
 * Mapping SonarQube Compute Engine 分析完成後推播之原生 Webhook JSON Payload。
 * 嚴格作為純資料載體，不包含任何轉換邏輯。
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReceiveSonarQubeWebhookResource(
        String serverUrl,
        String taskId,
        String status,
        String analysedAt,
        String revision,
        ProjectNode project,
        BranchNode branch,
        QualityGateNode qualityGate
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProjectNode(
            String key,
            String name,
            String url
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BranchNode(
            String name,
            String type,
            Boolean isMain,
            String url
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QualityGateNode(
            String name,
            String status
    ) {}
}
