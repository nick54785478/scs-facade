package com.example.scsfacade.domain.scanexecution.aggregate.vo;

import java.util.Objects;
import java.util.UUID;

/**
 * 掃描執行識別碼 (ScanExecutionId) 實質物件。
 * <p>
 * 封裝掃描執行記錄之唯一識別碼，確保不可變性與型別安全。
 * </p>
 */
public final class ScanExecutionId {

    /**
     * 掃描執行批次唯一識別碼 UUID 字串值。
     */
    private final String value;

    public ScanExecutionId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("ScanExecutionId 不可為空");
        }
        this.value = value.trim();
    }

    public static ScanExecutionId generate() {
        return new ScanExecutionId(UUID.randomUUID().toString());
    }

    public static ScanExecutionId of(String value) {
        return new ScanExecutionId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ScanExecutionId that)) {
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
