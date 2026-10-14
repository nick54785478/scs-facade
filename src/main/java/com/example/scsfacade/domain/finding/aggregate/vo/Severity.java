package com.example.scsfacade.domain.finding.aggregate.vo;

/**
 * 統一嚴重程度等級 (Severity) 實質物件。
 * <p>
 * 將異質掃描工具的漏洞等級標準化為五種統一級別。
 * </p>
 */
public enum Severity {

    /**
     * 嚴重 (Critical)：最高危險性缺陷，具直接攻擊風險或嚴重系統破壞性，門禁絕對阻擋。
     */
    CRITICAL(5),

    /**
     * 高危險 (High)：高風險弱點，可能導致敏感資訊外洩或權限提升，通常列入門禁管制。
     */
    HIGH(4),

    /**
     * 中度 (Medium)：中度風險缺陷，需在一定修復週期 (SLA) 內處理。
     */
    MEDIUM(3),

    /**
     * 低度 (Low)：低影響性問題或微小瑕疵。
     */
    LOW(2),

    /**
     * 資訊等級 (Info)：最佳實踐建議或一般資訊提示。
     */
    INFO(1);

    /**
     * 嚴重等級數值權重 (1 ~ 5，數值越大越嚴重)。
     */
    private final int level;

    Severity(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /**
     * 判斷當前等級是否大於或等於指定等級。
     *
     * @param other 比較的目標等級
     * @return 若等於或更嚴重則回傳 true
     */
    public boolean isAtLeast(Severity other) {
        if (other == null) {
            return true;
        }
        return this.level >= other.level;
    }
}
