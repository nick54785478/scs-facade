package com.example.scsfacade.domain.finding.aggregate.vo;

import java.util.Objects;
import java.util.UUID;

/**
 * 弱點唯一識別碼 (FindingId) 實質物件。
 * <p>
 * 封裝領域內弱點的唯一識別碼，確保不可變性與純 Java 實踐。
 * </p>
 */
public final class FindingId {

    /**
     * 弱點唯一識別碼 UUID 字串值。
     */
    private final String value;

    public FindingId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("FindingId 值不可為空");
        }
        this.value = value.trim();
    }

    /**
     * 生成新的隨機 FindingId。
     *
     * @return 新的 FindingId 實例
     */
    public static FindingId generate() {
        return new FindingId(UUID.randomUUID().toString());
    }

    /**
     * 依指定字串建立 FindingId。
     *
     * @param value 識別碼字串
     * @return FindingId 實例
     */
    public static FindingId of(String value) {
        return new FindingId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FindingId findingId)) {
            return false;
        }
        return Objects.equals(value, findingId.value);
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
