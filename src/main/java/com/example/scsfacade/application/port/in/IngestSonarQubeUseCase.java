package com.example.scsfacade.application.port.in;

import com.example.scsfacade.application.command.IngestSonarQubeCommand;
import com.example.scsfacade.application.dto.ScanExecutionGottenResult;

/**
 * 匯入 SonarQube 掃描結果用例 (IngestSonarQubeUseCase)。
 * <p>
 * 遵循 ... + UseCase 命名規範。定義表現層要求執行 SonarQube 拉取、弱點生命週期比對與門禁結算之合約。
 * </p>
 */
public interface IngestSonarQubeUseCase {

    /**
     * 執行 SonarQube 匯入與生命週期對齊。
     *
     * @param command 匯入命令
     * @return 掃描執行完成結果 DTO
     */
    ScanExecutionGottenResult execute(IngestSonarQubeCommand command);
}
