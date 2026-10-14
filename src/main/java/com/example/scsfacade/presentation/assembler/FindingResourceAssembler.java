package com.example.scsfacade.presentation.assembler;

import com.example.scsfacade.application.dto.FindingGottenResult;
import com.example.scsfacade.presentation.resource.out.FindingGottenResource;
import org.springframework.stereotype.Component;

/**
 * 弱點表現層組裝器 (FindingResourceAssembler)。
 * <p>
 * 專門負責應用層 DTO (FindingGottenResult) 與表現層回應載體 (FindingGottenResource) 之間的資料轉換。
 * </p>
 */
@Component
public class FindingResourceAssembler {

    /**
     * 將應用層 DTO 轉換為對外 Response Resource。
     *
     * @param result 應用層 DTO
     * @return 表現層回應載體
     */
    public FindingGottenResource toResource(FindingGottenResult result) {
        if (result == null) {
            return null;
        }
        return new FindingGottenResource(
                result.findingId(),
                result.projectId(),
                result.branch(),
                result.commitHash(),
                result.firstSeenCommit(),
                result.lastSeenCommit(),
                result.resolvedInCommit(),
                result.fingerprint(),
                result.toolName(),
                result.toolCategory(),
                result.category(),
                result.ruleId(),
                result.ruleName(),
                result.cweList(),
                result.cve(),
                result.tags(),
                result.severity(),
                result.filePath(),
                result.startLine(),
                result.endLine(),
                result.snippet(),
                result.status(),
                result.firstSeenAt(),
                result.lastSeenAt(),
                result.resolvedAt()
        );
    }
}
