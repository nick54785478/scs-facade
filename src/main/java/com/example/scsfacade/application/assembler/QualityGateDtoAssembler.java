package com.example.scsfacade.application.assembler;

import com.example.scsfacade.application.dto.QualityGateEvaluatedResult;
import com.example.scsfacade.domain.qualitygate.aggregate.vo.QualityGateStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 品質門禁應用層組裝器 (QualityGateDtoAssembler)。
 * <p>
 * 專門負責將領域層評估數據組裝為 QualityGateEvaluatedResult DTO。
 * </p>
 */
@Component
public class QualityGateDtoAssembler {

    /**
     * 組裝門禁評估結果 DTO。
     *
     * @param projectId     專案識別碼
     * @param status        門禁狀態 (PASSED/FAILED)
     * @param criticalCount Critical 等級弱點數量
     * @param highCount     High 等級弱點數量
     * @return 門禁評估結果 DTO
     */
    public QualityGateEvaluatedResult toResult(String projectId, QualityGateStatus status, int criticalCount, int highCount) {
        boolean passed = status == QualityGateStatus.PASSED;
        String message = passed
                ? String.format("專案 [%s] 通過品質門禁檢查 (Critical: %d, High: %d)", projectId, criticalCount, highCount)
                : String.format("專案 [%s] 未通過品質門禁檢查！超出允許之弱點門檻 (Critical: %d, High: %d)", projectId, criticalCount, highCount);

        return new QualityGateEvaluatedResult(
                projectId,
                status.name(),
                passed,
                criticalCount,
                highCount,
                message,
                Instant.now()
        );
    }
}
