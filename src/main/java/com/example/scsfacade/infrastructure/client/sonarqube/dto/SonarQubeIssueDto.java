package com.example.scsfacade.infrastructure.client.sonarqube.dto;

import java.util.List;

/**
 * SonarQube Issue 原始資料傳遞物件。
 * <p>
 * Mapping SonarQube Web API /api/issues/search 回傳之 JSON 節點。
 * </p>
 */
public record SonarQubeIssueDto(
        String key,
        String rule,
        String severity,
        String component,
        String project,
        Integer line,
        String message,
        String status,
        List<String> tags,
        String creationDate
) {
}
