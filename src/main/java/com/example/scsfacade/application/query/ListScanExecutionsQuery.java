package com.example.scsfacade.application.query;

/**
 * 查詢專案掃描執行紀錄清單查詢物件 (ListScanExecutionsQuery)。
 * <p>
 * 遵循 V + N + Query 命名規範。
 * </p>
 */
public record ListScanExecutionsQuery(
        String projectId,
        String branch
) {
    public ListScanExecutionsQuery {
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("專案代碼不可為空");
        }
    }
}
