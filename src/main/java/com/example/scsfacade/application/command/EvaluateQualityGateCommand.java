package com.example.scsfacade.application.command;

/**
 * 評估品質門禁命令 (EvaluateQualityGateCommand)。
 * <p>
 * 遵循 V + N + Command 命名規範。攜帶 CI/CD Pipeline 請求門禁審查之參數。
 * </p>
 */
public record EvaluateQualityGateCommand(
        String projectId,
        Integer maxCritical,
        Integer maxHigh,
        Boolean blockOnSecrets
) {
    public EvaluateQualityGateCommand {
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("專案代碼不可為空");
        }
    }
}
