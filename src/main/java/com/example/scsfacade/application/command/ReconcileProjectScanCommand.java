package com.example.scsfacade.application.command;

/**
 * 專案掃描對帳命令 (ReconcileProjectScanCommand)。
 * <p>
 * 遵循 V + N + Command 命名規範。攜帶欲進行低頻最終一致性對帳之專案、分支與強制同步旗標。
 * </p>
 */
public record ReconcileProjectScanCommand(
        String projectId,
        String branch,
        boolean forceSync
) {

    public ReconcileProjectScanCommand {
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("專案代碼 (projectId) 不可為空");
        }
        if (branch == null || branch.trim().isEmpty()) {
            branch = "main";
        }
    }

    public ReconcileProjectScanCommand(String projectId, String branch) {
        this(projectId, branch, false);
    }

    public ReconcileProjectScanCommand(String projectId) {
        this(projectId, "main", false);
    }
}
