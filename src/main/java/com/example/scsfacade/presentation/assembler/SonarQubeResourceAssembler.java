package com.example.scsfacade.presentation.assembler;

import com.example.scsfacade.application.command.IngestSonarQubeCommand;
import com.example.scsfacade.application.dto.FindingGottenResult;
import com.example.scsfacade.presentation.resource.in.IngestSonarQubeResource;
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
                resource.commitHash()
        );
    }

    /**
     * 將應用層弱點結果清單轉為 SonarQubeIngestedResource 表現層回應。
     *
     * @param projectId 專案代碼
     * @param results   弱點 DTO 清單
     * @return 匯入回應載體
     */
    public SonarQubeIngestedResource toResource(String projectId, List<FindingGottenResult> results) {
        List<FindingGottenResource> findingResources = results != null
                ? results.stream().map(findingResourceAssembler::toResource).toList()
                : Collections.emptyList();

        return new SonarQubeIngestedResource(
                projectId,
                findingResources.size(),
                findingResources
        );
    }
}
