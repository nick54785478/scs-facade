package com.example.scsfacade.application.service;

import com.example.scsfacade.application.command.ReconcileProjectScanCommand;
import com.example.scsfacade.application.dto.ScanReconciliationGottenResult;
import com.example.scsfacade.application.port.in.ReconcileProjectScanUseCase;
import com.example.scsfacade.application.port.out.DistributedLockPort;
import com.example.scsfacade.application.port.out.FindingEventPublisherPort;
import com.example.scsfacade.application.port.out.FindingRepositoryPort;
import com.example.scsfacade.application.port.out.SonarQubeScannerPort;
import com.example.scsfacade.domain.event.DomainEvent;
import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingFingerprint;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 專案掃描低頻對帳命令服務 (ReconcileProjectScanCommandService)。
 * <p>
 * 遵循 ... + CommandService 命名規範。實作 ReconcileProjectScanUseCase Inbound Port。
 * 負責比對 SonarQube 最新未修復弱點與 Facade 資料庫之差異，自動補償修復狀態、補錄漏報弱點並同步嚴重等級。
 * </p>
 */
@Service
public class ReconcileProjectScanCommandService implements ReconcileProjectScanUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReconcileProjectScanCommandService.class);

    private final SonarQubeScannerPort sonarQubeScannerPort;
    private final FindingRepositoryPort findingRepositoryPort;
    private final FindingEventPublisherPort findingEventPublisherPort;
    private final DistributedLockPort distributedLockPort;

    public ReconcileProjectScanCommandService(SonarQubeScannerPort sonarQubeScannerPort,
                                             FindingRepositoryPort findingRepositoryPort,
                                             FindingEventPublisherPort findingEventPublisherPort,
                                             DistributedLockPort distributedLockPort) {
        this.sonarQubeScannerPort = sonarQubeScannerPort;
        this.findingRepositoryPort = findingRepositoryPort;
        this.findingEventPublisherPort = findingEventPublisherPort;
        this.distributedLockPort = distributedLockPort;
    }

    @Override
    @Transactional
    public ScanReconciliationGottenResult execute(ReconcileProjectScanCommand command) {
        String branch = command.branch() != null && !command.branch().trim().isEmpty() ? command.branch().trim() : "main";
        String lockKey = "lock:scan:reconcile:" + command.projectId() + ":" + branch;

        boolean locked = distributedLockPort.tryLock(lockKey, 30);
        if (!locked) {
            throw new IllegalStateException("該專案對帳作業正在執行中或並發衝突，請勿重複發起: " + lockKey);
        }

        try {
            log.info("開始執行專案掃描低頻對帳: project={}, branch={}", command.projectId(), branch);

            // 1. 呼叫 SonarQube 抓取最新未修復之遠端弱點
            List<Finding> remoteFindings = sonarQubeScannerPort.fetchProjectIssues(command.projectId(), branch, "RECONCILED");

            // 2. 撈取本地資料庫中處於活躍狀態之弱點清單 (僅比對同為 SonarQube 工具之資料，避免誤傷其他工具)
            List<Finding> localAllActive = findingRepositoryPort.findActiveByProjectAndBranch(command.projectId(), branch);
            List<Finding> localSonarActive = localAllActive.stream()
                    .filter(f -> f.getToolInfo() != null && "SonarQube".equalsIgnoreCase(f.getToolInfo().getName()))
                    .toList();

            Map<FindingFingerprint, Finding> remoteMap = remoteFindings.stream()
                    .collect(Collectors.toMap(Finding::getFingerprint, f -> f, (e1, e2) -> e1));

            Map<FindingFingerprint, Finding> localMap = localSonarActive.stream()
                    .collect(Collectors.toMap(Finding::getFingerprint, f -> f, (e1, e2) -> e1));

            List<Finding> findingsToSave = new ArrayList<>();
            List<DomainEvent> eventsToPublish = new ArrayList<>();

            Instant now = Instant.now();
            int reconciledResolvedCount = 0;
            int reconciledAppendedCount = 0;
            int severityAdjustedCount = 0;
            int criticalCount = 0;
            int highCount = 0;

            // 3. Diff 比對情境 A 與 C：以本地活躍弱點為基準
            for (Finding local : localSonarActive) {
                Finding remote = remoteMap.get(local.getFingerprint());
                if (remote != null) {
                    // 雙方皆存在：比對 Severity 是否在遠端手動調整過
                    if (local.getSeverity() != remote.getSeverity()) {
                        local.adjustSeverity(remote.getSeverity());
                        severityAdjustedCount++;
                    }
                    local.touchScan("RECONCILED", now);
                    findingsToSave.add(local);

                    if (local.getSeverity() == Severity.CRITICAL) {
                        criticalCount++;
                    } else if (local.getSeverity() == Severity.HIGH) {
                        highCount++;
                    }
                } else {
                    // 本地存在但遠端已無此弱點 (情境 A：遠端手動設為 False Positive 或已修復)
                    local.markAsResolved("RECONCILED", now);
                    eventsToPublish.addAll(local.pullDomainEvents());
                    findingsToSave.add(local);
                    reconciledResolvedCount++;
                }
            }

            // 4. Diff 比對情境 B：以遠端弱點為基準，找出本地從未收錄之遺漏弱點
            for (Finding remote : remoteFindings) {
                if (!localMap.containsKey(remote.getFingerprint())) {
                    eventsToPublish.addAll(remote.pullDomainEvents());
                    findingsToSave.add(remote);
                    reconciledAppendedCount++;

                    if (remote.getSeverity() == Severity.CRITICAL) {
                        criticalCount++;
                    } else if (remote.getSeverity() == Severity.HIGH) {
                        highCount++;
                    }
                }
            }

            // 5. 品質門禁判定
            boolean passedQualityGate = (criticalCount == 0 && highCount <= 2);

            // 6. 批次保存與事件發布
            if (!findingsToSave.isEmpty()) {
                findingRepositoryPort.saveAll(findingsToSave);
            }
            if (!eventsToPublish.isEmpty()) {
                findingEventPublisherPort.publishAll(eventsToPublish);
            }

            log.info("專案掃描對帳完成: project={}, 遠端弱點={}, 本地原本弱點={}, 補償修復={}, 補錄遺漏={}, 等級同步={}, 門禁合格={}",
                    command.projectId(), remoteFindings.size(), localSonarActive.size(),
                    reconciledResolvedCount, reconciledAppendedCount, severityAdjustedCount, passedQualityGate);

            return new ScanReconciliationGottenResult(
                    command.projectId(),
                    branch,
                    "SonarQube",
                    remoteFindings.size(),
                    localSonarActive.size(),
                    reconciledResolvedCount,
                    reconciledAppendedCount,
                    severityAdjustedCount,
                    passedQualityGate,
                    now
            );

        } finally {
            distributedLockPort.releaseLock(lockKey);
        }
    }
}
