package com.example.scsfacade.application.port.in;

import com.example.scsfacade.application.command.ReconcileAllProjectsCommand;
import com.example.scsfacade.application.dto.ScanReconciliationGottenResult;

import java.util.List;

/**
 * 全專案掃描狀態低頻批次對帳用例 (ReconcileAllProjectsUseCase)。
 * <p>
 * 遵循 ... + UseCase 命名規範。Inbound Port 合約。
 * 負責探索所有納管專案清單，逐一進行外部掃描器狀態與本地 Facade 弱點對帳，以達成最終一致性。
 * </p>
 */
public interface ReconcileAllProjectsUseCase {

    /**
     * 執行全納管專案之掃描狀態低頻批次對帳。
     *
     * @param command 批次對帳命令
     * @return 納管專案之對帳結果清單
     */
    List<ScanReconciliationGottenResult> execute(ReconcileAllProjectsCommand command);
}
