package com.example.scsfacade.application.service;

import com.example.scsfacade.application.assembler.FindingDtoAssembler;
import com.example.scsfacade.application.command.IngestSonarQubeCommand;
import com.example.scsfacade.application.dto.FindingGottenResult;
import com.example.scsfacade.application.port.in.IngestSonarQubeUseCase;
import com.example.scsfacade.application.port.out.FindingEventPublisherPort;
import com.example.scsfacade.application.port.out.FindingRepositoryPort;
import com.example.scsfacade.application.port.out.SonarQubeScannerPort;
import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 匯入 SonarQube 掃描結果命令服務 (IngestSonarQubeCommandService)。
 * <p>
 * 遵循 ... + CommandService 命名規範。實作 Inbound Port (IngestSonarQubeUseCase)。
 * 職責：
 * 1. 協調 SonarQubeScannerPort 抓取遠端弱點資料。
 * 2. 依指紋進行去重與生命週期狀態比對 (Deduplication & State Update)。
 * 3. 透過 FindingRepositoryPort 持久化，並將領域事件經由 FindingEventPublisherPort 發布。
 * 4. 透過 FindingDtoAssembler 組裝並回傳 DTO。
 * </p>
 */
@Service
public class IngestSonarQubeCommandService implements IngestSonarQubeUseCase {

    private final SonarQubeScannerPort sonarQubeScannerPort;
    private final FindingRepositoryPort findingRepositoryPort;
    private final FindingEventPublisherPort findingEventPublisherPort;
    private final FindingDtoAssembler findingDtoAssembler;

    public IngestSonarQubeCommandService(SonarQubeScannerPort sonarQubeScannerPort,
                                        FindingRepositoryPort findingRepositoryPort,
                                        FindingEventPublisherPort findingEventPublisherPort,
                                        FindingDtoAssembler findingDtoAssembler) {
        this.sonarQubeScannerPort = sonarQubeScannerPort;
        this.findingRepositoryPort = findingRepositoryPort;
        this.findingEventPublisherPort = findingEventPublisherPort;
        this.findingDtoAssembler = findingDtoAssembler;
    }

    @Override
    @Transactional
    public List<FindingGottenResult> execute(IngestSonarQubeCommand command) {
        // 1. 呼叫外向埠拉取 SonarQube issues
        List<Finding> scannedFindings = sonarQubeScannerPort.fetchProjectIssues(
                command.projectId(),
                command.branch(),
                command.commitHash()
        );

        List<Finding> persistedFindings = new ArrayList<>();
        List<Object> allDomainEvents = new ArrayList<>();
        Instant scanTimestamp = Instant.now();

        // 2. 進行去重比對與狀態維護
        for (Finding incoming : scannedFindings) {
            Optional<Finding> existingOpt = findingRepositoryPort.findByFingerprint(incoming.getFingerprint());

            Finding target;
            if (existingOpt.isPresent()) {
                // 既有弱點：刷新最後出現時間與嚴重程度
                target = existingOpt.get();
                target.touchScan(scanTimestamp);
                target.adjustSeverity(incoming.getSeverity());
            } else {
                // 新弱點：直接納入儲存
                target = incoming;
            }

            Finding saved = findingRepositoryPort.save(target);
            persistedFindings.add(saved);

            // 提取領域事件
            allDomainEvents.addAll(target.pullDomainEvents());
        }

        // 3. 發布累積的領域事件
        findingEventPublisherPort.publishAll(allDomainEvents);

        // 4. 使用 Assembler 轉換為 DTO 回傳
        return persistedFindings.stream()
                .map(findingDtoAssembler::toResult)
                .toList();
    }
}
