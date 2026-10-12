package com.example.scsfacade.presentation.resource.out;

import java.time.Instant;
import java.util.List;

/**
 * 弱點已取得結果回應載體 (FindingGottenResource)。
 * <p>
 * 遵循 N + Gotten/Searched + Resource 命名規範。
 * 嚴格作為純資料載體，不包含任何轉換邏輯。
 * </p>
 */
public record FindingGottenResource(
        String findingId,
        String projectId,
        String branch,
        String commitHash,
        String fingerprint,
        String toolName,
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
        Instant lastSeenAt
) {
}
