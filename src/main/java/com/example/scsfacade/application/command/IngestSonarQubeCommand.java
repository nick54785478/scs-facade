package com.example.scsfacade.application.command;

/**
 * 匯入 SonarQube 掃描結果命令 (IngestSonarQubeCommand)。
 * <p>
 * 遵循 V + N + Command 命名規範。攜帶主動拉取或事件觸發 SonarQube 掃描匯入所需之參數。
 * </p>
 */
public record IngestSonarQubeCommand(
        String projectId,
        String branch,
        String commitHash
) {
    public IngestSonarQubeCommand {
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("專案代碼不可為空");
        }
    }
}
