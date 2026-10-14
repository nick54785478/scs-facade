package com.example.scsfacade.domain.qualitygate.aggregate.vo;

/**
 * 門禁評估狀態 (QualityGateStatus) 列舉。
 * <p>
 * 定義品質門禁審查之判定結果。
 * </p>
 */
public enum QualityGateStatus {

    /**
     * 審查合格 (PASSED)：未超出任何定義之重大風險或機敏資訊門檻，允許 CI/CD Pipeline 繼續部署。
     */
    PASSED,

    /**
     * 審查不合格 (FAILED)：存在超標之 Critical/High 弱點或機敏資訊，應立即阻擋部署流程。
     */
    FAILED
}
