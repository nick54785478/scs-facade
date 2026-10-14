package com.example.scsfacade.domain.qualitygate.aggregate.vo;

import java.util.Objects;
import java.util.UUID;

/**
 * 門禁策略識別碼 (QualityGatePolicyId) 實質物件。
 * <p>
 * 封裝專案品質門禁策略之唯一識別碼，確保不可變性與型別安全。
 * </p>
 */
public final class QualityGatePolicyId {

    /**
     * 門禁策略唯一識別碼 UUID 字串值。
     */
    private final String value;

    public QualityGatePolicyId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("QualityGatePolicyId 不可為空");
        }
        this.value = value.trim();
    }

    public static QualityGatePolicyId generate() {
        return new QualityGatePolicyId(UUID.randomUUID().toString());
    }

    public static QualityGatePolicyId of(String value) {
        return new QualityGatePolicyId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (!(o instanceof QualityGatePolicyId that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
