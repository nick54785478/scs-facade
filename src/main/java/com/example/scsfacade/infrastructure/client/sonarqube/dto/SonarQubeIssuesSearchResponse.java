package com.example.scsfacade.infrastructure.client.sonarqube.dto;

import java.util.List;

/**
 * SonarQube /api/issues/search 查詢回應包裝物件。
 */
public record SonarQubeIssuesSearchResponse(
        Integer total,
        Integer p,
        Integer ps,
        List<SonarQubeIssueDto> issues
) {
}
