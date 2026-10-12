package com.example.scsfacade.presentation.assembler;

import com.example.scsfacade.application.command.EvaluateQualityGateCommand;
import com.example.scsfacade.application.dto.QualityGateEvaluatedResult;
import com.example.scsfacade.presentation.resource.in.EvaluateQualityGateResource;
import com.example.scsfacade.presentation.resource.out.QualityGateEvaluatedResource;
import org.springframework.stereotype.Component;

/**
 * 品質門禁表現層組裝器 (QualityGateResourceAssembler)。
 * <p>
 * 專門負責門禁 Request/Response 與應用層 Command/DTO 互轉。
 * </p>
 */
@Component
public class QualityGateResourceAssembler {

    /**
     * 將 EvaluateQualityGateResource 轉為 EvaluateQualityGateCommand。
     *
     * @param resource 門禁評估請求
     * @return 應用層命令
     */
    public EvaluateQualityGateCommand toCommand(EvaluateQualityGateResource resource) {
        if (resource == null) {
            return null;
        }
        return new EvaluateQualityGateCommand(
                resource.projectId(),
                resource.maxCritical(),
                resource.maxHigh(),
                resource.blockOnSecrets()
        );
    }

    /**
     * 將應用層門禁評估結果轉為對外 Response Resource。
     *
     * @param result 應用層 DTO
     * @return 門禁回應載體
     */
    public QualityGateEvaluatedResource toResource(QualityGateEvaluatedResult result) {
        if (result == null) {
            return null;
        }
        return new QualityGateEvaluatedResource(
                result.projectId(),
                result.status(),
                result.passed(),
                result.criticalCount(),
                result.highCount(),
                result.message(),
                result.evaluatedAt()
        );
    }
}
