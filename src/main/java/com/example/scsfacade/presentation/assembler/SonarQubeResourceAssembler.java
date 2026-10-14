package com.example.scsfacade.presentation.assembler;

import com.example.scsfacade.application.command.IngestSonarQubeCommand;
import com.example.scsfacade.application.dto.ScanExecutionGottenResult;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanTriggerType;
import com.example.scsfacade.presentation.resource.in.IngestSonarQubeResource;
import com.example.scsfacade.presentation.resource.in.ReceiveSonarQubeWebhookResource;
import com.example.scsfacade.presentation.resource.out.FindingGottenResource;
import com.example.scsfacade.presentation.resource.out.SonarQubeIngestedResource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * SonarQube 表現層組裝器 (SonarQubeResourceAssembler)。
 * <p>
 * 專門負責表現層 Request/Response 與應用層 Command/DTO 之間的互轉防腐。
 * </p>
 */
@Component
public class SonarQubeResourceAssembler {

    private final FindingResourceAssembler findingResourceAssembler;

    public SonarQubeResourceAssembler(FindingResourceAssembler findingResourceAssembler) {
        this.findingResourceAssembler = findingResourceAssembler;
    }

    /**
     * 將 IngestSonarQubeResource 請求載體轉為 IngestSonarQubeCommand 應用層命令。
     *
     * @param resource 表現層請求
     * @return 應用層命令
     */
    public IngestSonarQubeCommand toCommand(IngestSonarQubeResource resource) {
        if (resource == null) {
            return null;
        }
        return new IngestSonarQubeCommand(
                resource.projectId(),
                resource.branch(),
                resource.commitHash(),
                ScanTriggerType.MANUAL,
                resource.idempotencyKey()
        );
    }

    /**
     * 將 SonarQube 原生 Webhook 請求載體轉為 IngestSonarQubeCommand 應用層命令。
     *
     * @param resource 原生 Webhook 請求
     * @return 應用層命令
     */
    public IngestSonarQubeCommand toCommand(ReceiveSonarQubeWebhookResource resource) {
        if (resource == null || resource.project() == null) {
            throw new IllegalArgumentException("SonarQube Webhook Payload 缺少專案資訊");
        }
        String projectId = resource.project().key();
        String branch = resource.branch() != null ? resource.branch().name() : "main";
        String commitHash = resource.revision() != null ? resource.revision() : "";
        String externalTaskId = resource.taskId();

        return new IngestSonarQubeCommand(projectId, branch, commitHash, ScanTriggerType.WEBHOOK, externalTaskId);
    }

    /**
     * 將應用層掃描執行結果轉為 SonarQubeIngestedResource 表現層回應。
     *
     * @param result 掃描執行結果 DTO
     * @return 匯入回應載體
     */
    public SonarQubeIngestedResource toResource(ScanExecutionGottenResult result) {
        if (result == null) {
            return null;
        }
        List<FindingGottenResource> findingResources = result.findings() != null
                ? result.findings().stream().map(findingResourceAssembler::toResource).toList()
                : Collections.emptyList();

        return new SonarQubeIngestedResource(
                result.executionId(),
                result.projectId(),
                result.branch(),
                result.commitHash(),
                result.status(),
                result.passedQualityGate(),
                result.totalActiveCount(),
                result.newCount(),
                result.existingCount(),
                result.resolvedCount(),
                result.criticalCount(),
                result.highCount(),
                findingResources
        );
    }
}
