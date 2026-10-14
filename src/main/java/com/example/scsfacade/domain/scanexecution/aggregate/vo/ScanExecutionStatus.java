package com.example.scsfacade.domain.scanexecution.aggregate.vo;

/**
 * 掃描執行狀態 (ScanExecutionStatus) 列舉。
 * <p>
 * 定義單一掃描批次歷程之生命週期狀態。
 * </p>
 */
public enum ScanExecutionStatus {

    /**
     * 執行中 (RUNNING)：掃描作業已啟動，正在拉取報表或比對弱點中。
     */
    RUNNING,

    /**
     * 成功完成 (SUCCESS)：掃描與對齊結算順利完成，已產出完整統計與門禁判定。
     */
    SUCCESS,

    /**
     * 執行失敗 (FAILED)：掃描拉取失敗、外部工具逾時或系統處理異常中斷。
     */
    FAILED
}
