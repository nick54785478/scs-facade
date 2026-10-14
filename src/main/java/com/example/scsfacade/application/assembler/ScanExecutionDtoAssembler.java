package com.example.scsfacade.application.assembler;

import com.example.scsfacade.application.dto.FindingGottenResult;
import com.example.scsfacade.application.dto.ScanExecutionGottenResult;
import com.example.scsfacade.domain.scanexecution.aggregate.root.ScanExecution;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 掃描執行應用層組裝器 (ScanExecutionDtoAssembler)。
 * <p>
 * 專門負責領域聚合根 (ScanExecution) 與 DTO (ScanExecutionGottenResult) 之間的轉換。
 * </p>
 */
@Component
public class ScanExecutionDtoAssembler {

    /**
     * 將 ScanExecution 聚合根轉換為 ScanExecutionGottenResult DTO。
     *
     * @param scanExecution 掃描執行聚合根
     * @param findings      弱點 DTO 清單 (可為空)
     * @return 轉換後的 ScanExecutionGottenResult
     */
    public ScanExecutionGottenResult toResult(ScanExecution scanExecution, List<FindingGottenResult> findings) {
        if (scanExecution == null) {
            return null;
        }
        return new ScanExecutionGottenResult(
                scanExecution.getId().getValue(),
                scanExecution.getProjectId(),
                scanExecution.getBranch(),
                scanExecution.getCommitHash(),
                scanExecution.getToolInfo().getName(),
                scanExecution.getToolInfo().getVersion(),
                scanExecution.getTriggerType().name(),
                scanExecution.getStatus().name(),
                scanExecution.getSummary().getTotalActiveCount(),
                scanExecution.getSummary().getNewCount(),
                scanExecution.getSummary().getExistingCount(),
                scanExecution.getSummary().getResolvedCount(),
                scanExecution.getSummary().getCriticalCount(),
                scanExecution.getSummary().getHighCount(),
                scanExecution.getSummary().isPassedQualityGate(),
                scanExecution.getStartedAt(),
                scanExecution.getCompletedAt(),
                scanExecution.getErrorMessage(),
                findings != null ? findings : Collections.emptyList(),
                scanExecution.getExternalTaskId()
        );
    }

    public ScanExecutionGottenResult toResult(ScanExecution scanExecution) {
        return toResult(scanExecution, Collections.emptyList());
    }
}
