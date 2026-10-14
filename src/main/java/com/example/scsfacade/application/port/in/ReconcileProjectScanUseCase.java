package com.example.scsfacade.application.port.in;

import com.example.scsfacade.application.command.ReconcileProjectScanCommand;
import com.example.scsfacade.application.dto.ScanReconciliationGottenResult;

/**
 * 專案掃描狀態低頻對帳用例 (ReconcileProjectScanUseCase)。
 * <p>
 * 遵循 ... + UseCase 命名規範。Inbound Port 合約。
 * 負責比對外部掃描器伺服器與本地 Facade 之活躍弱點差異，補償修復與遺漏狀態，達成最終一致性。
 * </p>
 */
public interface ReconcileProjectScanUseCase {

    /**
     * 執行指定專案之掃描狀態低頻對帳。
     *
     * @param command 對帳命令
     * @return 對帳結算結果 DTO
     */
    ScanReconciliationGottenResult execute(ReconcileProjectScanCommand command);
}
