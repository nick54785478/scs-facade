package com.example.scsfacade.application.port.in;

import com.example.scsfacade.application.dto.ScanExecutionGottenResult;
import com.example.scsfacade.application.query.ListScanExecutionsQuery;

import java.util.List;

/**
 * 查詢專案掃描執行紀錄清單用例 (ListScanExecutionsUseCase)。
 * <p>
 * 遵循 ... + UseCase 命名規範。
 * </p>
 */
public interface ListScanExecutionsUseCase {

    /**
     * 執行查詢掃描紀錄清單。
     *
     * @param query 查詢物件
     * @return 掃描執行結果 DTO 清單
     */
    List<ScanExecutionGottenResult> execute(ListScanExecutionsQuery query);
}
