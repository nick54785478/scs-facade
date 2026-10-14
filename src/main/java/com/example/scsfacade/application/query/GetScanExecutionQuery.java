package com.example.scsfacade.application.query;

/**
 * 查詢單筆掃描執行紀錄查詢物件 (GetScanExecutionQuery)。
 * <p>
 * 遵循 V + N + Query 命名規範。
 * </p>
 */
public record GetScanExecutionQuery(
        String executionId
) {
    public GetScanExecutionQuery {
        if (executionId == null || executionId.trim().isEmpty()) {
            throw new IllegalArgumentException("掃描識別碼不可為空");
        }
    }
}
