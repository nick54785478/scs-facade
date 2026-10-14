package com.example.scsfacade.application.service;

import com.example.scsfacade.application.assembler.QualityGateDtoAssembler;
import com.example.scsfacade.application.command.EvaluateQualityGateCommand;
import com.example.scsfacade.application.dto.QualityGateEvaluatedResult;
import com.example.scsfacade.application.port.in.EvaluateQualityGateUseCase;
import com.example.scsfacade.application.port.out.FindingEventPublisherPort;
import com.example.scsfacade.application.port.out.FindingRepositoryPort;
import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import com.example.scsfacade.domain.qualitygate.aggregate.root.QualityGatePolicy;
import com.example.scsfacade.domain.qualitygate.aggregate.vo.QualityGateStatus;
import com.example.scsfacade.domain.qualitygate.aggregate.vo.QualityGateThreshold;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 評估品質門禁命令服務 (EvaluateQualityGateCommandService)。
 * <p>
 * 遵循 ... + CommandService 命名規範。實作 Inbound Port (EvaluateQualityGateUseCase)。
 * 職責：
 * 1. 從 FindingRepositoryPort 取得目標專案目前所有活躍中的弱點。
 * 2. 建立或載入專案的 QualityGatePolicy 聚合根。
 * 3. 委託聚合根執行門禁評估並發布領域事件。
 * 4. 委託 QualityGateDtoAssembler 組裝評估結果 DTO。
 * </p>
 */
@Service
public class EvaluateQualityGateCommandService implements EvaluateQualityGateUseCase {

    private final FindingRepositoryPort findingRepositoryPort;
    private final FindingEventPublisherPort findingEventPublisherPort;
    private final QualityGateDtoAssembler qualityGateDtoAssembler;

    public EvaluateQualityGateCommandService(FindingRepositoryPort findingRepositoryPort,
                                            FindingEventPublisherPort findingEventPublisherPort,
                                            QualityGateDtoAssembler qualityGateDtoAssembler) {
        this.findingRepositoryPort = findingRepositoryPort;
        this.findingEventPublisherPort = findingEventPublisherPort;
        this.qualityGateDtoAssembler = qualityGateDtoAssembler;
    }

    @Override
    @Transactional
    public QualityGateEvaluatedResult execute(EvaluateQualityGateCommand command) {
        // 1. 取得專案當前未修復弱點
        List<Finding> activeFindings = findingRepositoryPort.findActiveByProjectId(command.projectId());

        // 2. 依命令構建門檻規則（若命令未指定則採用預設嚴格標準）
        int maxCritical = command.maxCritical() != null ? command.maxCritical() : 0;
        int maxHigh = command.maxHigh() != null ? command.maxHigh() : 2;
        boolean blockOnSecrets = !Boolean.FALSE.equals(command.blockOnSecrets());

        QualityGateThreshold threshold = new QualityGateThreshold(maxCritical, maxHigh, blockOnSecrets);
        QualityGatePolicy policy = QualityGatePolicy.create(command.projectId(), threshold);

        // 3. 聚合根執行核心業務評估
        QualityGateStatus status = policy.evaluate(activeFindings);

        // 4. 發布門禁評估事件
        findingEventPublisherPort.publishAll(policy.pullDomainEvents());

        // 5. 計算統計數量
        int criticalCount = 0;
        int highCount = 0;
        for (Finding finding : activeFindings) {
            if (finding.getSeverity() == Severity.CRITICAL) {
                criticalCount++;
            } else if (finding.getSeverity() == Severity.HIGH) {
                highCount++;
            }
        }

        // 6. 透過組裝器回傳 DTO
        return qualityGateDtoAssembler.toResult(command.projectId(), status, criticalCount, highCount);
    }
}
