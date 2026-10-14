package com.example.scsfacade.domain.finding.aggregate.vo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * 弱點指紋 (FindingFingerprint) 實質物件。
 * <p>
 * 用於跨掃描輪次追蹤與跨工具去重。封裝 SHA-256 Hash 計算邏輯，確保不可變性。
 * </p>
 */
public final class FindingFingerprint {

    /**
     * SHA-256 指紋雜湊字串 (十六進制小寫字串)。
     */
    private final String value;

    public FindingFingerprint(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("指紋雜湊值不可為空");
        }
        this.value = value.trim().toLowerCase();
    }

    /**
     * 根據專案、分支、檔案相對路徑、規則 ID 與代碼上下文計算 SHA-256 唯一指紋。
     *
     * @param projectId 專案代碼
     * @param branch    分支名稱
     * @param filePath  檔案路徑
     * @param ruleId    規則代碼
     * @param snippet   代碼片段（或正規化上下文字串）
     * @return 計算後的 FindingFingerprint
     */
    public static FindingFingerprint compute(String projectId, String branch, String filePath, String ruleId, String snippet) {
        String normalizedSnippet = snippet != null ? snippet.replaceAll("\\s+", " ").trim() : "";
        String raw = String.format("%s:%s:%s:%s:%s",
                projectId != null ? projectId.trim() : "",
                branch != null ? branch.trim() : "main",
                filePath != null ? filePath.trim().replace("\\", "/") : "",
                ruleId != null ? ruleId.trim() : "",
                normalizedSnippet);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return new FindingFingerprint(HexFormat.of().formatHex(hash));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("無法取得 SHA-256 演算法", e);
        }
    }

    /**
     * 預設分支（向後相容）計算指紋。
     */
    public static FindingFingerprint compute(String projectId, String filePath, String ruleId, String snippet) {
        return compute(projectId, "main", filePath, ruleId, snippet);
    }

    public static FindingFingerprint of(String value) {
        return new FindingFingerprint(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FindingFingerprint that)) {
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
