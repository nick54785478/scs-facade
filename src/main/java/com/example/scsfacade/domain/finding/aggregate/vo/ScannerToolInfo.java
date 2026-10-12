package com.example.scsfacade.domain.finding.aggregate.vo;

import java.util.Objects;

/**
 * 掃描工具資訊 (ScannerToolInfo) 實質物件。
 * <p>
 * 紀錄該弱點是由何種工具（如 SonarQube、Trivy、Gitleaks）及其版本與類別所掃描產出。
 * </p>
 */
public final class ScannerToolInfo {

    private final String name;
    private final String version;
    private final String category; // SAST, SCA, SECRET, IAC

    public ScannerToolInfo(String name, String version, String category) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("掃描工具名稱不可為空");
        }
        this.name = name.trim();
        this.version = version != null ? version.trim() : "unknown";
        this.category = category != null ? category.trim().toUpperCase() : "SAST";
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScannerToolInfo that)) return false;
        return Objects.equals(name, that.name) &&
                Objects.equals(version, that.version) &&
                Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, category);
    }
}
