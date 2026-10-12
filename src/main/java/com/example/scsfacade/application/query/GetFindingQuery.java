package com.example.scsfacade.application.query;

/**
 * 取得單一弱點查詢 (GetFindingQuery)。
 * <p>
 * 遵循 V + N + Query 命名規範。
 * </p>
 */
public record GetFindingQuery(
        String findingId
) {
    public GetFindingQuery {
        if (findingId == null || findingId.trim().isEmpty()) {
            throw new IllegalArgumentException("Finding ID 不可為空");
        }
    }
}
