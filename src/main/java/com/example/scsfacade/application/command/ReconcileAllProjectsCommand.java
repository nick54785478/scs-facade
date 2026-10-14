package com.example.scsfacade.application.command;

/**
 * 全專案掃描批次對帳命令 (ReconcileAllProjectsCommand)。
 * <p>
 * 遵循 V + N + Command 命名規範。攜帶批次觸發低頻最終一致性對帳之強制同步旗標。
 * </p>
 */
public record ReconcileAllProjectsCommand(
        boolean forceSync
) {
    public ReconcileAllProjectsCommand() {
        this(false);
    }
}
