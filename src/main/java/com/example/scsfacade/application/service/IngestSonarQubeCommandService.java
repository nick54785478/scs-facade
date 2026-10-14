package com.example.scsfacade.application.service;

import com.example.scsfacade.application.assembler.FindingDtoAssembler;
import com.example.scsfacade.application.assembler.ScanExecutionDtoAssembler;
import com.example.scsfacade.application.command.IngestSonarQubeCommand;
import com.example.scsfacade.application.dto.FindingGottenResult;
import com.example.scsfacade.application.dto.ScanExecutionGottenResult;
import com.example.scsfacade.application.port.in.IngestSonarQubeUseCase;
import com.example.scsfacade.application.port.out.DistributedLockPort;
import com.example.scsfacade.application.port.out.FindingEventPublisherPort;
import com.example.scsfacade.application.port.out.FindingRepositoryPort;
import com.example.scsfacade.application.port.out.ScanExecutionRepositoryPort;
import com.example.scsfacade.application.port.out.SonarQubeScannerPort;
import com.example.scsfacade.domain.event.DomainEvent;
import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingFingerprint;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import com.example.scsfacade.domain.scanexecution.aggregate.root.ScanExecution;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionStatus;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanSummary;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanTriggerType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 匯入 SonarQube 掃描結果命令服務 (IngestSonarQubeCommandService)。
 * <p>
 * 遵循 ... + CommandService 命名規範。實作 Inbound Port (IngestSonarQubeUseCase)。
 * 職責：
 * 1. 執行外部任務識別碼 (taskId) 冪等性防重校驗 (Fast Return)。
 * 2. 透過 DistributedLockPort 取得 Redis 互斥鎖，防禦並發重試衝突。
 * 3. 建立並啟動 ScanExecution 掃描執行批次。
 * 4. 協調 SonarQubeScannerPort 抓取遠端最新弱點清單。
 * 5. 執行生命週期自動對齊 (Reconciliation Engine)：
 *    - 新增弱點 (New / Introduced)：首次出現，標記 firstSeenCommit。
 *    - 既有弱點 (Existing / Persistent)：更新 lastSeenCommit 與掃描時間戳記。
 *    - 已修復弱點 (Resolved / Fixed)：在本次掃描中消失者，自動標記為 RESOLVED 並記錄 resolvedInCommit。
 * 6. 計算 Quality Gate 與各項統計指標，完成 ScanExecution。
 * 7. 統一發布領域事件，並透過組裝器回傳包含掃描歷程之 DTO。
 * </p>
 */
@Service
public class IngestSonarQubeCommandService implements IngestSonarQubeUseCase {

    private final SonarQubeScannerPort sonarQubeScannerPort;
    private final FindingRepositoryPort findingRepositoryPort;
    private final ScanExecutionRepositoryPort scanExecutionRepositoryPort;
    private final FindingEventPublisherPort findingEventPublisherPort;
    private final DistributedLockPort distributedLockPort;
    private final FindingDtoAssembler findingDtoAssembler;
    private final ScanExecutionDtoAssembler scanExecutionDtoAssembler;

    public IngestSonarQubeCommandService(SonarQubeScannerPort sonarQubeScannerPort,
                                        FindingRepositoryPort findingRepositoryPort,
                                        ScanExecutionRepositoryPort scanExecutionRepositoryPort,
                                        FindingEventPublisherPort findingEventPublisherPort,
                                        DistributedLockPort distributedLockPort,
                                        FindingDtoAssembler findingDtoAssembler,
                                        ScanExecutionDtoAssembler scanExecutionDtoAssembler) {
        this.sonarQubeScannerPort = sonarQubeScannerPort;
        this.findingRepositoryPort = findingRepositoryPort;
        this.scanExecutionRepositoryPort = scanExecutionRepositoryPort;
        this.findingEventPublisherPort = findingEventPublisherPort;
        this.distributedLockPort = distributedLockPort;
        this.findingDtoAssembler = findingDtoAssembler;
        this.scanExecutionDtoAssembler = scanExecutionDtoAssembler;
    }

    @Override
    @Transactional
    public ScanExecutionGottenResult execute(IngestSonarQubeCommand command) {
        String branch = command.branch() != null && !command.branch().trim().isEmpty() ? command.branch().trim() : "main";
        String commitHash = command.commitHash() != null ? command.commitHash().trim() : "";
        ScanTriggerType triggerType = command.triggerType() != null ? command.triggerType() : ScanTriggerType.WEBHOOK;
        String externalTaskId = command.externalTaskId() != null && !command.externalTaskId().trim().isEmpty()
                ? command.externalTaskId().trim()
                : null;

        // 1. 冪等性防重校驗 (Fast Return)：若同一外部任務已存在且已成功完成，直接組裝歷史結果回傳
        if (externalTaskId != null) {
            Optional<ScanExecution> existingExecutionOpt = scanExecutionRepositoryPort.findByExternalTaskId(externalTaskId);
            if (existingExecutionOpt.isPresent()) {
                ScanExecution existingExecution = existingExecutionOpt.get();
                if (existingExecution.getStatus() == ScanExecutionStatus.SUCCESS) {
                    List<Finding> currentActive = findingRepositoryPort.findActiveByProjectAndBranch(command.projectId(), branch);
                    List<FindingGottenResult> findingResults = currentActive.stream()
                            .map(findingDtoAssembler::toResult)
                            .toList();
                    return scanExecutionDtoAssembler.toResult(existingExecution, findingResults);
                }
            }
        }

        // 2. 獲取分散式鎖 (防止並發重試造成弱點 Unique 衝突或重複建立批次)
        String lockKey = (externalTaskId != null)
                ? "lock:scan:task:" + externalTaskId
                : "lock:scan:project:" + command.projectId() + ":" + branch;

        boolean locked = distributedLockPort.tryLock(lockKey, 60);
        if (!locked) {
            throw new IllegalStateException("相同掃描任務正在處理中或並發衝突，請勿重複提交: " + lockKey);
        }

        ScanExecution scanExecution = null;
        try {
            // 3. 初始化並啟動 ScanExecution 聚合根
            ScannerToolInfo toolInfo = new ScannerToolInfo("SonarQube", "10.x", "SAST");
            scanExecution = ScanExecution.start(command.projectId(), branch, commitHash, toolInfo, triggerType, externalTaskId);

            List<DomainEvent> allDomainEvents = new ArrayList<>(scanExecution.pullDomainEvents());

            // 4. 呼叫外向埠拉取 SonarQube 最新 issues
            List<Finding> incomingFindings = sonarQubeScannerPort.fetchProjectIssues(
                    command.projectId(),
                    branch,
                    commitHash
            );

            // 5. 取得該專案分支目前資料庫中處於活躍 (待處理) 狀態的弱點清單
            List<Finding> activeDbFindings = findingRepositoryPort.findActiveByProjectAndBranch(command.projectId(), branch);

            Map<FindingFingerprint, Finding> activeMap = activeDbFindings.stream()
                    .collect(Collectors.toMap(Finding::getFingerprint, f -> f, (existing, replace) -> existing));

            List<Finding> findingsToSave = new ArrayList<>();
            Set<FindingFingerprint> incomingFingerprints = new HashSet<>();

            Instant scanTimestamp = Instant.now();
            int newCount = 0;
            int existingCount = 0;
            int resolvedCount = 0;
            int criticalCount = 0;
            int highCount = 0;

            // 6. 比對傳入的掃描弱點
            for (Finding incoming : incomingFindings) {
                incomingFingerprints.add(incoming.getFingerprint());
                Finding existing = activeMap.get(incoming.getFingerprint());

                if (existing != null) {
                    // 既有活躍弱點：刷新最後出現 Commit 與時間戳記，同步嚴重等級
                    existing.touchScan(commitHash, scanTimestamp);
                    existing.adjustSeverity(incoming.getSeverity());
                    findingsToSave.add(existing);
                    existingCount++;

                    if (existing.getSeverity() == Severity.CRITICAL) {
                        criticalCount++;
                    } else if (existing.getSeverity() == Severity.HIGH) {
                        highCount++;
                    }
                    allDomainEvents.addAll(existing.pullDomainEvents());
                } else {
                    // 新增弱點：直接納入儲存
                    findingsToSave.add(incoming);
                    newCount++;

                    if (incoming.getSeverity() == Severity.CRITICAL) {
                        criticalCount++;
                    } else if (incoming.getSeverity() == Severity.HIGH) {
                        highCount++;
                    }
                    allDomainEvents.addAll(incoming.pullDomainEvents());
                }
            }

            // 7. 比對已消失弱點：先前為 OPEN，但本次 SonarQube 未再回報 -> 自動標記為 RESOLVED (修復結案)
            for (Finding active : activeDbFindings) {
                if (!incomingFingerprints.contains(active.getFingerprint())) {
                    active.markAsResolved(commitHash, scanTimestamp);
                    findingsToSave.add(active);
                    resolvedCount++;
                    allDomainEvents.addAll(active.pullDomainEvents());
                }
            }

            // 8. 批次儲存弱點
            findingRepositoryPort.saveAll(findingsToSave);

            // 9. 計算門禁判定標準 (例如: 0 Critical 且 <= 2 High)
            boolean passedQualityGate = (criticalCount == 0 && highCount <= 2);

            ScanSummary summary = new ScanSummary(
                    incomingFindings.size(),
                    newCount,
                    existingCount,
                    resolvedCount,
                    criticalCount,
                    highCount,
                    passedQualityGate
            );

            // 10. 結算並儲存 ScanExecution
            scanExecution.complete(summary);
            allDomainEvents.addAll(scanExecution.pullDomainEvents());
            ScanExecution savedExecution = scanExecutionRepositoryPort.save(scanExecution);

            // 11. 統一發布領域事件
            findingEventPublisherPort.publishAll(allDomainEvents);

            // 12. 組裝回傳 DTO
            List<FindingGottenResult> findingResults = findingsToSave.stream()
                    .map(findingDtoAssembler::toResult)
                    .toList();

            return scanExecutionDtoAssembler.toResult(savedExecution, findingResults);
        } catch (Exception e) {
            if (scanExecution != null) {
                scanExecution.fail(e.getMessage());
                scanExecutionRepositoryPort.save(scanExecution);
                findingEventPublisherPort.publishAll(scanExecution.pullDomainEvents());
            }
            throw e;
        } finally {
            // 釋放分散式鎖
            distributedLockPort.releaseLock(lockKey);
        }
    }
}
