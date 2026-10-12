package com.example.scsfacade.application.query;

/**
 * 依專案列出弱點查詢 (ListFindingQuery)。
 * <p>
 * 遵循 V + N + Query 命名規範。
 * </p>
 */
public record ListFindingQuery(
        String projectId,
        String status
) {
    public ListFindingQuery {
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("專案代碼不可為空");
        }
    }
}
