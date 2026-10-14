package com.example.scsfacade.infrastructure.scheduler;

import com.example.scsfacade.application.command.ReconcileAllProjectsCommand;
import com.example.scsfacade.application.port.in.ReconcileAllProjectsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 掃描狀態低頻對帳定時排程排程器 (ScanReconciliationScheduler)。
 * <p>
 * 屬於 Infrastructure 基礎設施排程 Worker。
 * 週期性（預設每日凌晨 02:00）觸發低頻全專案狀態對帳，修復漏接 Webhook 與遠端人工修復之最終一致性。
 * </p>
 */
@Component
public class ScanReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ScanReconciliationScheduler.class);

    private final ReconcileAllProjectsUseCase reconcileAllProjectsUseCase;
    private final boolean enabled;

    public ScanReconciliationScheduler(
            ReconcileAllProjectsUseCase reconcileAllProjectsUseCase,
            @Value("${scs.reconciliation.enabled:true}") boolean enabled) {
        this.reconcileAllProjectsUseCase = reconcileAllProjectsUseCase;
        this.enabled = enabled;
    }

    /**
     * 觸發週期性低頻對帳作業。
     */
    @Scheduled(cron = "${scs.reconciliation.cron:0 0 2 * * ?}")
    public void runScheduledReconciliation() {
        if (!enabled) {
            log.info("低頻對帳排程已被停用 (scs.reconciliation.enabled=false)，跳過定時執行");
            return;
        }

        log.info("觸發低頻對帳定時排程任務...");
        try {
            reconcileAllProjectsUseCase.execute(new ReconcileAllProjectsCommand(false));
            log.info("低頻對帳定時排程任務執行完畢");
        } catch (Exception e) {
            log.error("執行低頻對帳定時排程任務時發生未預期異常: {}", e.getMessage(), e);
        }
    }
}
