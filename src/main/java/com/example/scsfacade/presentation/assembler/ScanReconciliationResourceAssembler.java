package com.example.scsfacade.presentation.assembler;

import com.example.scsfacade.application.command.ReconcileProjectScanCommand;
import com.example.scsfacade.application.dto.ScanReconciliationGottenResult;
import com.example.scsfacade.presentation.resource.in.ReconcileProjectScanResource;
import com.example.scsfacade.presentation.resource.out.ScanReconciledResource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 掃描對帳表現層轉換組裝器 (ScanReconciliationResourceAssembler)。
 * <p>
 * 遵循六角形架構防腐層 (ACL) 原則，獨立負責 Presentation Resource 與 Application Command/Result 之雙向對齊轉換。
 * </p>
 */
@Component
public class ScanReconciliationResourceAssembler {

    /**
     * 將表現層對帳請求資源轉換為應用層命令物件。
     *
     * @param resource 表現層對帳請求載體
     * @return 應用層對帳命令
     */
    public ReconcileProjectScanCommand toCommand(ReconcileProjectScanResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("ReconcileProjectScanResource 不可為空");
        }
        boolean forceSync = Boolean.TRUE.equals(resource.forceSync());
        return new ReconcileProjectScanCommand(
                resource.projectId(),
                resource.branch(),
                forceSync
        );
    }

    /**
     * 將應用層對帳結算結果 DTO 轉換為表現層回應資源載體。
     *
     * @param result 應用層對帳結果 DTO
     * @return 表現層對帳回應載體
     */
    public ScanReconciledResource toResource(ScanReconciliationGottenResult result) {
        if (result == null) {
            return null;
        }
        return new ScanReconciledResource(
                result.projectId(),
                result.branch(),
                result.toolName(),
                result.remoteActiveCount(),
                result.localActiveCount(),
                result.reconciledResolvedCount(),
                result.reconciledAppendedCount(),
                result.severityAdjustedCount(),
                result.passedQualityGate(),
                result.executedAt()
        );
    }

    /**
     * 將應用層對帳結算結果清單批次轉換為表現層回應資源載體清單。
     *
     * @param results 應用層對帳結果清單
     * @return 表現層回應載體清單
     */
    public List<ScanReconciledResource> toResourceList(List<ScanReconciliationGottenResult> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }
        return results.stream()
                .map(this::toResource)
                .toList();
    }
}
