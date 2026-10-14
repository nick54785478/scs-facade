package com.example.scsfacade.application.service;

import com.example.scsfacade.application.command.ReconcileAllProjectsCommand;
import com.example.scsfacade.application.command.ReconcileProjectScanCommand;
import com.example.scsfacade.application.dto.ScanReconciliationGottenResult;
import com.example.scsfacade.application.port.in.ReconcileAllProjectsUseCase;
import com.example.scsfacade.application.port.in.ReconcileProjectScanUseCase;
import com.example.scsfacade.application.port.out.DistributedLockPort;
import com.example.scsfacade.application.port.out.ProjectDiscoveryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 全專案掃描低頻批次對帳命令服務 (ReconcileAllProjectsCommandService)。
 * <p>
 * 遵循 ... + CommandService 命名規範。實作 ReconcileAllProjectsUseCase Inbound Port。
 * 職責：
 * 1. 透過分散式全域鎖避免多節點或排程重複並發執行。
 * 2. 呼叫 ProjectDiscoveryPort 探索所有納管專案代碼。
 * 3. 逐一委託 ReconcileProjectScanUseCase 執行個別專案之對帳。
 * 4. 加入平滑流量控制 (Pacing)，避免短時間內對外部掃描伺服器造成突發衝擊。
 * </p>
 */
@Service
public class ReconcileAllProjectsCommandService implements ReconcileAllProjectsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReconcileAllProjectsCommandService.class);
    private static final String GLOBAL_LOCK_KEY = "lock:reconciliation:global";

    private final ProjectDiscoveryPort projectDiscoveryPort;
    private final ReconcileProjectScanUseCase reconcileProjectScanUseCase;
    private final DistributedLockPort distributedLockPort;

    public ReconcileAllProjectsCommandService(ProjectDiscoveryPort projectDiscoveryPort,
                                             ReconcileProjectScanUseCase reconcileProjectScanUseCase,
                                             DistributedLockPort distributedLockPort) {
        this.projectDiscoveryPort = projectDiscoveryPort;
        this.reconcileProjectScanUseCase = reconcileProjectScanUseCase;
        this.distributedLockPort = distributedLockPort;
    }

    @Override
    public List<ScanReconciliationGottenResult> execute(ReconcileAllProjectsCommand command) {
        boolean locked = distributedLockPort.tryLock(GLOBAL_LOCK_KEY, 300);
        if (!locked) {
            log.info("全域掃描對帳排程作業正在其他節點或執行緒處理中，本次自動略過 [lockKey: {}]", GLOBAL_LOCK_KEY);
            return Collections.emptyList();
        }

        try {
            List<String> projectIds = projectDiscoveryPort.discoverConfiguredProjectIds();
            if (projectIds == null || projectIds.isEmpty()) {
                log.info("無任何已配置之納管專案代碼，結束批次對帳");
                return Collections.emptyList();
            }

            log.info("開始執行全專案低頻批次對帳作業，納管專案總數: {}", projectIds.size());
            List<ScanReconciliationGottenResult> results = new ArrayList<>();

            for (String projectId : projectIds) {
                try {
                    ReconcileProjectScanCommand singleCommand = new ReconcileProjectScanCommand(
                            projectId,
                            "main",
                            command.forceSync()
                    );
                    ScanReconciliationGottenResult result = reconcileProjectScanUseCase.execute(singleCommand);
                    results.add(result);
                } catch (Exception e) {
                    log.error("專案 [{}] 執行低頻對帳時發生異常，記錄錯誤並繼續處理其餘專案: {}", projectId, e.getMessage(), e);
                }

                // 平滑流量控制：每個專案完成後間歇 100 毫秒，避免衝擊遠端 API
                paceNextExecution();
            }

            log.info("全專案低頻批次對帳作業完成，成功對帳專案數: {}/{}", results.size(), projectIds.size());
            return results;

        } finally {
            distributedLockPort.releaseLock(GLOBAL_LOCK_KEY);
        }
    }

    private void paceNextExecution() {
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("批次對帳平滑流控暫停時收到線程中斷信號");
        }
    }
}
