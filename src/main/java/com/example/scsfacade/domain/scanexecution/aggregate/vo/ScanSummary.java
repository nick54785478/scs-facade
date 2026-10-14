package com.example.scsfacade.domain.scanexecution.aggregate.vo;

import java.util.Objects;

/**
 * 掃描摘要統計 (ScanSummary) 實質物件。
 * <p>
 * 封裝單次掃描批次之各項統計指標與門禁判定。
 * </p>
 */
public final class ScanSummary {

    /**
     * 當前分支累計之總活躍弱點數量 (Total Active Findings)。
     */
    private final int totalActiveCount;

    /**
     * 本次掃描新發現之弱點數量 (Newly Detected Findings)。
     */
    private final int newCount;

    /**
     * 本次掃描與歷史紀錄對齊後之既有弱點數量 (Existing Active Findings)。
     */
    private final int existingCount;

    /**
     * 本次掃描相較前版已修復結案之弱點數量 (Resolved / Fixed Findings)。
     */
    private final int resolvedCount;

    /**
     * 當前活躍弱點中嚴重等級為 CRITICAL 之數量 (Critical Severity Findings)。
     */
    private final int criticalCount;

    /**
     * 當前活躍弱點中嚴重等級為 HIGH 之數量 (High Severity Findings)。
     */
    private final int highCount;

    /**
     * 本次掃描是否通過專案品質門禁審查 (Passed Quality Gate)。
     */
    private final boolean passedQualityGate;

    public ScanSummary(int totalActiveCount,
                       int newCount,
                       int existingCount,
                       int resolvedCount,
                       int criticalCount,
                       int highCount,
                       boolean passedQualityGate) {
        this.totalActiveCount = Math.max(0, totalActiveCount);
        this.newCount = Math.max(0, newCount);
        this.existingCount = Math.max(0, existingCount);
        this.resolvedCount = Math.max(0, resolvedCount);
        this.criticalCount = Math.max(0, criticalCount);
        this.highCount = Math.max(0, highCount);
        this.passedQualityGate = passedQualityGate;
    }

    public static ScanSummary empty() {
        return new ScanSummary(0, 0, 0, 0, 0, 0, true);
    }

    public int getTotalActiveCount() {
        return totalActiveCount;
    }

    public int getNewCount() {
        return newCount;
    }

    public int getExistingCount() {
        return existingCount;
    }

    public int getResolvedCount() {
        return resolvedCount;
    }

    public int getCriticalCount() {
        return criticalCount;
    }

    public int getHighCount() {
        return highCount;
    }

    public boolean isPassedQualityGate() {
        return passedQualityGate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ScanSummary that)) {
            return false;
        }
        return totalActiveCount == that.totalActiveCount &&
                newCount == that.newCount &&
                existingCount == that.existingCount &&
                resolvedCount == that.resolvedCount &&
                criticalCount == that.criticalCount &&
                highCount == that.highCount &&
                passedQualityGate == that.passedQualityGate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalActiveCount, newCount, existingCount, resolvedCount, criticalCount, highCount, passedQualityGate);
    }
}
