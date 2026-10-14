package com.example.scsfacade.domain.scanexecution.aggregate.vo;

/**
 * 掃描觸發來源類型 (ScanTriggerType) 列舉。
 * <p>
 * 定義啟動源碼檢測作業的觸發管道。
 * </p>
 */
public enum ScanTriggerType {

    /**
     * Webhook 事件自動觸發 (例如由 SonarQube 完成後主動回呼系統)。
     */
    WEBHOOK,

    /**
     * CI/CD Pipeline 流程執行中主動觸發 (例如 GitLab CI / GitHub Actions 步驟呼叫)。
     */
    PIPELINE,

    /**
     * 管理人員手動發起 (例如在後台點擊重新檢測或補抓歷史紀錄)。
     */
    MANUAL,

    /**
     * 檔案上傳模式主動匯入 (例如 CI/CD 上傳 SARIF/JSON 報告)。
     */
    UPLOAD
}
