package com.example.scsfacade.application.port.in;

import com.example.scsfacade.application.dto.ScanExecutionGottenResult;
import com.example.scsfacade.application.query.GetScanExecutionQuery;

/**
 * 查詢單筆掃描執行紀錄用例 (GetScanExecutionUseCase)。
 * <p>
 * 遵循 ... + UseCase 命名規範。定義表現層呼叫應用層之業務合約。
 * </p>
 */
public interface GetScanExecutionUseCase {

    /**
     * 執行查詢掃描紀錄。
     *
     * @param query 查詢物件
     * @return 掃描執行結果 DTO
     */
    ScanExecutionGottenResult execute(GetScanExecutionQuery query);
}
