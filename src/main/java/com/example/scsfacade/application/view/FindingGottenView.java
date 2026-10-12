package com.example.scsfacade.application.view;

/**
 * 弱點摘要投影視圖 (FindingGottenView)。
 * <p>
 * 遵循 N + Gotten/Searched + View 命名規範。
 * 供清單查詢或儀表板展示用之純資料載體。
 * </p>
 */
public record FindingGottenView(
        String findingId,
        String projectId,
        String ruleId,
        String severity,
        String filePath,
        Integer startLine,
        String status
) {
}
