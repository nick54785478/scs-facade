package com.example.scsfacade.application.port.in;

import com.example.scsfacade.application.command.IngestSonarQubeCommand;
import com.example.scsfacade.application.dto.FindingGottenResult;

import java.util.List;

/**
 * 匯入 SonarQube 掃描結果使用案例 (IngestSonarQubeUseCase)。
 * <p>
 * 遵循 ... + UseCase 命名規範。應用層對外開放的 Inbound Port 業務合約。
 * 負責協調 SonarQube 外部拉取、指紋去重、弱點生命週期狀態更新與儲存。
 * </p>
 */
public interface IngestSonarQubeUseCase {

    /**
     * 執行 SonarQube 報告匯入流程。
     *
     * @param command 匯入命令
     * @return 匯入或更新後的標準弱點結果清單
     */
    List<FindingGottenResult> execute(IngestSonarQubeCommand command);
}
