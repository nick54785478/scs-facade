package com.example.scsfacade.application.command;

import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanTriggerType;

/**
 * 匯入 SonarQube 掃描結果命令 (IngestSonarQubeCommand)。
 * <p>
 * 遵循 V + N + Command 命名規範。攜帶主動拉取或事件觸發 SonarQube 掃描匯入所需之參數。
 * </p>
 */
public record IngestSonarQubeCommand(
        String projectId,
        String branch,
        String commitHash,
        ScanTriggerType triggerType,
        String externalTaskId
) {
    public IngestSonarQubeCommand {
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("專案代碼不可為空");
        }
        if (triggerType == null) {
            triggerType = ScanTriggerType.WEBHOOK;
        }
        if (externalTaskId != null && externalTaskId.trim().isEmpty()) {
            externalTaskId = null;
        }
    }

    public IngestSonarQubeCommand(String projectId, String branch, String commitHash, ScanTriggerType triggerType) {
        this(projectId, branch, commitHash, triggerType, null);
    }

    public IngestSonarQubeCommand(String projectId, String branch, String commitHash) {
        this(projectId, branch, commitHash, ScanTriggerType.WEBHOOK, null);
    }
}
