package com.example.scsfacade.domain.finding.aggregate.vo;

/**
 * 統一嚴重程度等級 (Severity) 實質物件。
 * <p>
 * 將異質掃描工具的漏洞等級標準化為五種統一級別。
 * </p>
 */
public enum Severity {

    CRITICAL(5),
    HIGH(4),
    MEDIUM(3),
    LOW(2),
    INFO(1);

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
