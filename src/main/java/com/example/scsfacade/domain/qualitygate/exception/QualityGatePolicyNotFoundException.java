package com.example.scsfacade.domain.qualitygate.exception;

/**
 * 找不到品質門禁策略之領域例外 (QualityGatePolicyNotFoundException)。
 */
public class QualityGatePolicyNotFoundException extends RuntimeException {

    private final String projectId;

    public QualityGatePolicyNotFoundException(String projectId) {
        super(String.format("找不到專案之門禁策略: %s", projectId));
        this.projectId = projectId;
    }

    public String getProjectId() {
        return projectId;
    }
}
