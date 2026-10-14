package com.example.scsfacade.application.port.in;

import com.example.scsfacade.application.command.UploadScanReportCommand;
import com.example.scsfacade.application.dto.ScanReportUploadedResult;

/**
 * 上傳並處理掃描報告用例 (UploadScanReportUseCase)。
 * <p>
 * 遵循 ... + UseCase 命名規範。Inbound Port 合約。
 * 負責接收 CI/CD 主動推送之掃描報告（如 SARIF），進行生命週期對齊、弱點指紋去重與掃描批次結算。
 * </p>
 */
public interface UploadScanReportUseCase {

    /**
     * 執行掃描報告上傳與入庫結算作業。
     *
     * @param command 上傳命令
     * @return 掃描報告上傳結算結果 DTO
     */
    ScanReportUploadedResult execute(UploadScanReportCommand command);
}
