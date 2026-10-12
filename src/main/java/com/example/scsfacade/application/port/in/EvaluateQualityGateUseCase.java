package com.example.scsfacade.application.port.in;

import com.example.scsfacade.application.command.EvaluateQualityGateCommand;
import com.example.scsfacade.application.dto.QualityGateEvaluatedResult;

/**
 * 評估品質門禁使用案例 (EvaluateQualityGateUseCase)。
 * <p>
 * 遵循 ... + UseCase 命名規範。Inbound Port 合約。
 * 負責根據專案與門檻原則，審查當前所有未修復之弱點，判定是否放行 CI/CD。
 * </p>
 */
public interface EvaluateQualityGateUseCase {

    /**
     * 執行品質門禁評估。
     *
     * @param command 門禁評估命令
     * @return 評估結果 DTO
     */
    QualityGateEvaluatedResult execute(EvaluateQualityGateCommand command);
}
