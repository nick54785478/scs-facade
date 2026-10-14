package com.example.scsfacade.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring 排程任務全域配置類別 (SchedulingConfiguration)。
 * <p>
 * 遵循配置集中存放於 config/ 之規範。
 * 啟用 Spring @Scheduled 排程支援，供低頻對帳機制 (ScanReconciliationScheduler) 週期性排程作業執行。
 * </p>
 */
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}
