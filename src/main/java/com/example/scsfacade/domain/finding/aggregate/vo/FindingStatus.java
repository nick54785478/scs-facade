package com.example.scsfacade.domain.finding.aggregate.vo;

/**
 * 弱點生命週期狀態 (FindingStatus) 實質物件。
 * <p>
 * 定義弱點在全生命週期中的處理狀態。
 * </p>
 */
public enum FindingStatus {

    /**
     * 新發現或仍在存在之未處理弱點
     */
    OPEN,

    /**
     * 經資安人員確認為真實風險之弱點
     */
    CONFIRMED,

    /**
     * 經審核標記為誤報
     */
    FALSE_POSITIVE,

    /**
     * 已評估但暫不修復（接受風險）
     */
    ACCEPTED_RISK,

    /**
     * 已修復（最新一輪掃描已不存在）
     */
    RESOLVED;

    /**
     * 判斷此狀態是否為作用中（尚未修復且非誤報/接受風險）。
     *
     * @return 若為活躍待處理狀態回傳 true
     */
    public boolean isActive() {
        return this == OPEN || this == CONFIRMED;
    }
}
