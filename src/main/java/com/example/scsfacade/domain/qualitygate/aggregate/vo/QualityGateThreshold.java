package com.example.scsfacade.domain.qualitygate.aggregate.vo;

import java.util.Objects;

/**
 * 品質門禁閾值規則 (QualityGateThreshold) 實質物件。
 * <p>
 * 定義允許的各等級最大弱點數量門檻。
 * </p>
 */
public final class QualityGateThreshold {

    /**
     * 允許之嚴重等級 (Critical) 弱點最大數量上限 (超過則門禁判定不合格)。
     */
    private final int maxCritical;

    /**
     * 允許之高危險等級 (High) 弱點最大數量上限 (超過則門禁判定不合格)。
     */
    private final int maxHigh;

    /**
     * 是否在偵測到機敏資訊洩漏 (Secret Leak) 時強制阻擋。
     */
    private final boolean blockOnSecrets;

    public QualityGateThreshold(int maxCritical, int maxHigh, boolean blockOnSecrets) {
        if (maxCritical < 0 || maxHigh < 0) {
            throw new IllegalArgumentException("弱點數量門檻不可小於 0");
        }
        this.maxCritical = maxCritical;
        this.maxHigh = maxHigh;
        this.blockOnSecrets = blockOnSecrets;
    }

    /**
     * 建立預設嚴格門檻：零容忍 Critical，允許最多 2 個 High。
     *
     * @return 預設 QualityGateThreshold
     */
    public static QualityGateThreshold defaultStrict() {
        return new QualityGateThreshold(0, 2, true);
    }

    public int getMaxCritical() {
        return maxCritical;
    }

    public int getMaxHigh() {
        return maxHigh;
    }

    public boolean isBlockOnSecrets() {
        return blockOnSecrets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (!(o instanceof QualityGateThreshold that)) {
            return false;
        }
        return maxCritical == that.maxCritical &&
                maxHigh == that.maxHigh &&
                blockOnSecrets == that.blockOnSecrets;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxCritical, maxHigh, blockOnSecrets);
    }
}
