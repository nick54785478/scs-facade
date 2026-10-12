package com.example.scsfacade.domain.finding.aggregate.vo;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 規則與弱點分類 (RuleInfo) 實質物件。
 * <p>
 * 封裝規則代碼、名稱、對應之 CWE 編號與 CVE 編號。
 * </p>
 */
public final class RuleInfo {

    private final String ruleId;
    private final String name;
    private final List<String> cweList;
    private final String cve;

    public RuleInfo(String ruleId, String name, List<String> cweList, String cve) {
        if (ruleId == null || ruleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule ID 不可為空");
        }
        this.ruleId = ruleId.trim();
        this.name = name != null ? name.trim() : "";
        this.cweList = cweList != null ? List.copyOf(cweList) : Collections.emptyList();
        this.cve = cve != null ? cve.trim() : null;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getName() {
        return name;
    }

    public List<String> getCweList() {
        return cweList;
    }

    public String getCve() {
        return cve;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RuleInfo ruleInfo)) return false;
        return Objects.equals(ruleId, ruleInfo.ruleId) &&
                Objects.equals(name, ruleInfo.name) &&
                Objects.equals(cweList, ruleInfo.cweList) &&
                Objects.equals(cve, ruleInfo.cve);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleId, name, cweList, cve);
    }
}
