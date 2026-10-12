package com.example.scsfacade.application.dto;

import java.time.Instant;
import java.util.List;

/**
 * 弱點查詢結果資料傳遞物件 (FindingGottenResult)。
 * <p>
 * 遵循 N + Gotten/Searched + Result 命名規範。
 * 依據規範，此物件嚴格作為純資料載體 (Pure Data Carrier)，禁止於內部撰寫任何領域模型轉換邏輯。
 * 轉換職責由 assembler/ 套件中的 Assembler 獨立負責。
 * </p>
 */
public record FindingGottenResult(
        String findingId,
        String projectId,
        String branch,
        String commitHash,
        String fingerprint,
        String toolName,
        String toolVersion,
        String toolCategory,
        String ruleId,
        String ruleName,
        List<String> cweList,
        String cve,
        String severity,
        String filePath,
        Integer startLine,
        Integer endLine,
        String snippet,
        String status,
        Instant firstSeenAt,
        Instant lastSeenAt,
        String rawDetails
) {
}
