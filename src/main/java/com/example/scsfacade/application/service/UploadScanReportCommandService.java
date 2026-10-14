package com.example.scsfacade.application.service;

import com.example.scsfacade.application.command.UploadScanReportCommand;
import com.example.scsfacade.application.dto.ScanReportUploadedResult;
import com.example.scsfacade.application.port.in.UploadScanReportUseCase;
import com.example.scsfacade.application.port.out.DistributedLockPort;
import com.example.scsfacade.application.port.out.FindingEventPublisherPort;
import com.example.scsfacade.application.port.out.FindingRepositoryPort;
import com.example.scsfacade.application.port.out.ScanExecutionRepositoryPort;
import com.example.scsfacade.application.port.out.ScanReportParserPort;
import com.example.scsfacade.application.port.out.ScanReportParserPort.ParsedFindingItem;
import com.example.scsfacade.application.port.out.ScanReportParserPort.ParsedScanReport;
import com.example.scsfacade.domain.event.DomainEvent;
import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingFingerprint;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import com.example.scsfacade.domain.scanexecution.aggregate.root.ScanExecution;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionStatus;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanSummary;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanTriggerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * 掃描報告上傳命令服務 (UploadScanReportCommandService)。
 * <p>
 * 遵循 ... + CommandService 命名規範。實作 UploadScanReportUseCase Inbound Port。
 * 負責調度異質報告解析器、比對弱點生命週期狀態、計算指標並結算 ScanExecution 聚合根。
 * </p>
 */
@Service
public class UploadScanReportCommandService implements UploadScanReportUseCase {

    private static final Logger log = LoggerFactory.getLogger(UploadScanReportCommandService.class);

    private final ScanReportParserPort scanReportParserPort;
    private final ScanExecutionRepositoryPort scanExecutionRepositoryPort;
    private final FindingRepositoryPort findingRepositoryPort;
    private final FindingEventPublisherPort findingEventPublisherPort;
    private final DistributedLockPort distributedLockPort;

    public UploadScanReportCommandService(ScanReportParserPort scanReportParserPort,
                                          ScanExecutionRepositoryPort scanExecutionRepositoryPort,
                                          FindingRepositoryPort findingRepositoryPort,
                                          FindingEventPublisherPort findingEventPublisherPort,
                                          DistributedLockPort distributedLockPort) {
        this.scanReportParserPort = scanReportParserPort;
        this.scanExecutionRepositoryPort = scanExecutionRepositoryPort;
        this.findingRepositoryPort = findingRepositoryPort;
        this.findingEventPublisherPort = findingEventPublisherPort;
        this.distributedLockPort = distributedLockPort;
    }

    @Override
    @Transactional
    public ScanReportUploadedResult execute(UploadScanReportCommand command) {
        String branch = command.branch() != null && !command.branch().trim().isEmpty() ? command.branch().trim() : "main";
        String commitHash = command.commitHash() != null ? command.commitHash().trim() : "";
        String externalTaskId = command.externalTaskId() != null && !command.externalTaskId().trim().isEmpty()
                ? command.externalTaskId().trim()
                : null;

        // 1. 冪等性防重檢查：若相同 externalTaskId 已成功完成，直接回傳
        if (externalTaskId != null) {
            Optional<ScanExecution> existingOpt = scanExecutionRepositoryPort.findByExternalTaskId(externalTaskId);
            if (existingOpt.isPresent()) {
                ScanExecution existing = existingOpt.get();
                if (existing.getStatus() == ScanExecutionStatus.SUCCESS) {
                    log.info("掃描任務已完成，直接回傳冪等結果: externalTaskId={}", externalTaskId);
                    return toResult(existing, existing.getToolInfo().getName());
                }
            }
        }

        // 2. 獲取分散式鎖，防止併發重試造成重複插入
        String toolHint = command.toolName() != null ? command.toolName().trim() : "sarif";
        String lockKey = (externalTaskId != null)
                ? "lock:scan:upload:task:" + externalTaskId
                : "lock:scan:upload:" + command.projectId() + ":" + branch + ":" + toolHint;

        boolean locked = distributedLockPort.tryLock(lockKey, 60);
        if (!locked) {
            throw new IllegalStateException("相同報告上傳任務正在處理中，請勿重複提交: " + lockKey);
        }

        ScanExecution scanExecution = null;
        try {
            // 3. 呼叫解析器解析 SARIF 報告內容
            ParsedScanReport parsedReport = scanReportParserPort.parse(command.reportContent(), command.toolName());
            ScannerToolInfo toolInfo = parsedReport.toolInfo();

            // 4. 啟動 ScanExecution 聚合根
            scanExecution = ScanExecution.start(
                    command.projectId(),
                    branch,
                    commitHash,
                    toolInfo,
                    ScanTriggerType.UPLOAD,
                    externalTaskId
            );
            List<DomainEvent> allEvents = new ArrayList<>(scanExecution.pullDomainEvents());

            // 5. 轉換為傳入之 Finding 實體清單
            List<Finding> incomingFindings = new ArrayList<>();
            for (ParsedFindingItem item : parsedReport.items()) {
                Finding finding = Finding.createNew(
                        command.projectId(),
                        branch,
                        commitHash,
                        toolInfo,
                        item.category(),
                        item.ruleInfo(),
                        item.severity(),
                        item.location(),
                        item.rawDetails()
                );
                incomingFindings.add(finding);
            }

            // 6. 撈取目前資料庫中同專案同分支之活躍弱點
            List<Finding> activeDbFindings = findingRepositoryPort.findActiveByProjectAndBranch(command.projectId(), branch);

            // 僅比對同工具類別之弱點，避免 Trivy 結算時將 Gitleaks 弱點標記為修復
            Map<FindingFingerprint, Finding> activeMap = activeDbFindings.stream()
                    .filter(f -> isSameTool(f.getToolInfo(), toolInfo))
                    .collect(Collectors.toMap(Finding::getFingerprint, f -> f, (e1, e2) -> e1));

            List<Finding> findingsToSave = new ArrayList<>();
            Set<FindingFingerprint> incomingFingerprints = new HashSet<>();

            Instant scanTimestamp = Instant.now();
            int newCount = 0;
            int existingCount = 0;
            int resolvedCount = 0;
            int criticalCount = 0;
            int highCount = 0;

            // 7. 比對傳入弱點
            for (Finding incoming : incomingFindings) {
                incomingFingerprints.add(incoming.getFingerprint());
                Finding existing = activeMap.get(incoming.getFingerprint());

                if (existing != null) {
                    // 既有弱點持續存在：刷新最後偵測 Commit 與時間，同步等級
                    existing.touchScan(commitHash, scanTimestamp);
                    existing.adjustSeverity(incoming.getSeverity());
                    findingsToSave.add(existing);
                    existingCount++;

                    if (existing.getSeverity() == Severity.CRITICAL) {
                        criticalCount++;
                    } else if (existing.getSeverity() == Severity.HIGH) {
                        highCount++;
                    }
                } else {
                    // 全新弱點
                    allEvents.addAll(incoming.pullDomainEvents());
                    findingsToSave.add(incoming);
                    newCount++;

                    if (incoming.getSeverity() == Severity.CRITICAL) {
                        criticalCount++;
                    } else if (incoming.getSeverity() == Severity.HIGH) {
                        highCount++;
                    }
                }
            }

            // 8. 比對庫內已消失之同工具弱點 (標記為已修復 RESOLVED)
            for (Finding existing : activeMap.values()) {
                if (!incomingFingerprints.contains(existing.getFingerprint())) {
                    existing.markAsResolved(commitHash, scanTimestamp);
                    allEvents.addAll(existing.pullDomainEvents());
                    findingsToSave.add(existing);
                    resolvedCount++;
                }
            }

            // 9. 結算統計指標與門禁判定 (零 Critical 且 High <= 2)
            int totalActive = existingCount + newCount;
            boolean passedQualityGate = (criticalCount == 0 && highCount <= 2);

            ScanSummary summary = new ScanSummary(
                    totalActive,
                    newCount,
                    existingCount,
                    resolvedCount,
                    criticalCount,
                    highCount,
                    passedQualityGate
            );

            scanExecution.complete(summary);
            allEvents.addAll(scanExecution.pullDomainEvents());

            // 10. 持久化入庫
            findingRepositoryPort.saveAll(findingsToSave);
            ScanExecution savedExecution = scanExecutionRepositoryPort.save(scanExecution);

            // 11. 發布領域事件
            findingEventPublisherPort.publishAll(allEvents);

            log.info("報告上傳結算完成: executionId={}, project={}, tool={}, totalActive={}, new={}, resolved={}, passed={}",
                    savedExecution.getId().getValue(), command.projectId(), toolInfo.getName(),
                    totalActive, newCount, resolvedCount, passedQualityGate);

            return toResult(savedExecution, toolInfo.getName());

        } catch (Exception e) {
            log.error("報告上傳結算作業異常: {}", e.getMessage(), e);
            if (scanExecution != null) {
                scanExecution.fail(e.getMessage());
                scanExecutionRepositoryPort.save(scanExecution);
            }
            throw e;
        } finally {
            distributedLockPort.releaseLock(lockKey);
        }
    }

    private boolean isSameTool(ScannerToolInfo t1, ScannerToolInfo t2) {
        if (t1 == null || t2 == null) {
            return false;
        }
        return t1.getName().equalsIgnoreCase(t2.getName());
    }

    private ScanReportUploadedResult toResult(ScanExecution execution, String toolName) {
        ScanSummary s = execution.getSummary();
        return new ScanReportUploadedResult(
                execution.getId().getValue(),
                execution.getProjectId(),
                execution.getBranch(),
                execution.getCommitHash(),
                toolName,
                execution.getStatus().name(),
                s.isPassedQualityGate(),
                s.getTotalActiveCount(),
                s.getNewCount(),
                s.getExistingCount(),
                s.getResolvedCount(),
                s.getCriticalCount(),
                s.getHighCount(),
                execution.getCompletedAt()
        );
    }
}
