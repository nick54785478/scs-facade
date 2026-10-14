package com.example.scsfacade.domain.finding.aggregate.vo;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 規則與弱點分類 (RuleInfo) 實質物件。
 * <p>
 * 封裝規則代碼、名稱、對應之 CWE 編號清單、CVE 編號以及標籤 tags。
 * </p>
 */
public final class RuleInfo {

    /**
     * 掃描工具之規則識別碼 (Rule Identifier，例如 "java:S2077")。
     */
    private final String ruleId;

    /**
     * 規則人類可讀名稱或描述性標題 (Rule Name，例如 "SQL queries should not be vulnerable to injection attacks")。
     */
    private final String name;

    /**
     * 關聯之通用缺陷列舉清單 (CWE IDs，例如 ["CWE-89"])。
     */
    private final List<String> cweList;

    /**
     * 關聯之通用漏洞披露編號 (CVE ID，例如 "CVE-2023-1234"，無則為 null)。
     */
    private final String cve;

    /**
     * 規則關聯之標籤分類清單 (Tags，例如 ["security", "sql", "owasp-a1"])。
     */
    private final List<String> tags;

    public RuleInfo(String ruleId, String name, List<String> cweList, String cve, List<String> tags) {
        if (ruleId == null || ruleId.trim().isEmpty()) {
            throw new IllegalArgumentException("Rule ID 不可為空");
        }
        this.ruleId = ruleId.trim();
        this.name = name != null ? name.trim() : "";
        this.cweList = cweList != null ? List.copyOf(cweList) : Collections.emptyList();
        this.cve = cve != null ? cve.trim() : null;
        this.tags = tags != null ? List.copyOf(tags) : Collections.emptyList();
    }

    public RuleInfo(String ruleId, String name, List<String> cweList, String cve) {
        this(ruleId, name, cweList, cve, Collections.emptyList());
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

    public List<String> getTags() {
        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RuleInfo ruleInfo)) return false;
        return Objects.equals(ruleId, ruleInfo.ruleId) &&
                Objects.equals(name, ruleInfo.name) &&
                Objects.equals(cweList, ruleInfo.cweList) &&
                Objects.equals(cve, ruleInfo.cve) &&
                Objects.equals(tags, ruleInfo.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleId, name, cweList, cve, tags);
    }
}
