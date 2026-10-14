package com.example.scsfacade.presentation.assembler;

import com.example.scsfacade.application.dto.ScanExecutionGottenResult;
import com.example.scsfacade.presentation.resource.out.FindingGottenResource;
import com.example.scsfacade.presentation.resource.out.ScanExecutionGottenResource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 掃描執行表現層組裝器 (ScanExecutionResourceAssembler)。
 * <p>
 * 專門負責應用層 DTO (ScanExecutionGottenResult) 與表現層回應載體 (ScanExecutionGottenResource) 之間的轉換。
 * </p>
 */
@Component
public class ScanExecutionResourceAssembler {

    private final FindingResourceAssembler findingResourceAssembler;

    public ScanExecutionResourceAssembler(FindingResourceAssembler findingResourceAssembler) {
        this.findingResourceAssembler = findingResourceAssembler;
    }

    public ScanExecutionGottenResource toResource(ScanExecutionGottenResult result) {
        if (result == null) {
            return null;
        }

        List<FindingGottenResource> findingResources = result.findings() != null
                ? result.findings().stream().map(findingResourceAssembler::toResource).toList()
                : Collections.emptyList();

        return new ScanExecutionGottenResource(
                result.executionId(),
                result.projectId(),
                result.branch(),
                result.commitHash(),
                result.toolName(),
                result.toolVersion(),
                result.triggerType(),
                result.status(),
                result.totalActiveCount(),
                result.newCount(),
                result.existingCount(),
                result.resolvedCount(),
                result.criticalCount(),
                result.highCount(),
                result.passedQualityGate(),
                result.startedAt(),
                result.completedAt(),
                result.errorMessage(),
                findingResources
        );
    }
}
