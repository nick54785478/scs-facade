package com.example.scsfacade.domain.finding.aggregate.root;

import com.example.scsfacade.domain.finding.aggregate.vo.FindingFingerprint;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingId;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingLocation;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingStatus;
import com.example.scsfacade.domain.finding.aggregate.vo.RuleInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import com.example.scsfacade.domain.finding.event.FindingIngestedEvent;
import com.example.scsfacade.domain.finding.event.FindingStatusChangedEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 弱點聚合根 (Finding Aggregate Root)。
 * <p>
 * DDD 核心領域模型，封裝單一源碼弱點的業務規則、狀態轉移邏輯與領域事件。
 * 遵守規範：純 Java 實作，零框架、零 JPA、零 Jackson、嚴禁使用 Lombok。
 * 所有狀態變更皆透過具業務意圖的方法進行。
 * </p>
 */
public class Finding {

    private final FindingId id;
    private final String projectId;
    private final String branch;
    private final String commitHash;
    private final FindingFingerprint fingerprint;
    private final ScannerToolInfo toolInfo;
    private final RuleInfo ruleInfo;
    private Severity severity;
    private final FindingLocation location;
    private FindingStatus status;
    private final Instant firstSeenAt;
    private Instant lastSeenAt;
    private final String rawDetails;

    /**
     * 內部累積之領域事件清單
     */
    private final List<Object> domainEvents = new ArrayList<>();

    /**
     * 領域聚合建構子（受保護或私有，供工廠或重建呼叫）。
     */
    public Finding(FindingId id,
                   String projectId,
                   String branch,
                   String commitHash,
                   FindingFingerprint fingerprint,
                   ScannerToolInfo toolInfo,
                   RuleInfo ruleInfo,
                   Severity severity,
                   FindingLocation location,
                   FindingStatus status,
                   Instant firstSeenAt,
                   Instant lastSeenAt,
                   String rawDetails) {
        this.id = Objects.requireNonNull(id, "FindingId 不可為空");
        this.projectId = Objects.requireNonNull(projectId, "projectId 不可為空");
        this.branch = branch != null ? branch.trim() : "main";
        this.commitHash = commitHash != null ? commitHash.trim() : "";
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint 不可為空");
        this.toolInfo = Objects.requireNonNull(toolInfo, "toolInfo 不可為空");
        this.ruleInfo = Objects.requireNonNull(ruleInfo, "ruleInfo 不可為空");
        this.severity = Objects.requireNonNull(severity, "severity 不可為空");
        this.location = Objects.requireNonNull(location, "location 不可為空");
        this.status = Objects.requireNonNull(status, "status 不可為空");
        this.firstSeenAt = firstSeenAt != null ? firstSeenAt : Instant.now();
        this.lastSeenAt = lastSeenAt != null ? lastSeenAt : this.firstSeenAt;
        this.rawDetails = rawDetails != null ? rawDetails : "{}";
    }

    /**
     * 工廠方法：建立一筆新發現的弱點，並發布 FindingIngestedEvent。
     *
     * @param projectId  專案識別碼
     * @param branch     分支名稱
     * @param commitHash 提交雜湊
     * @param toolInfo   掃描工具資訊
     * @param ruleInfo   規則分類資訊
     * @param severity   標準化嚴重等級
     * @param location   位置資訊
     * @param rawDetails 原始工具回傳 JSON
     * @return 新建的 Finding 聚合根實例
     */
    public static Finding createNew(String projectId,
                                    String branch,
                                    String commitHash,
                                    ScannerToolInfo toolInfo,
                                    RuleInfo ruleInfo,
                                    Severity severity,
                                    FindingLocation location,
                                    String rawDetails) {
        FindingId newId = FindingId.generate();
        FindingFingerprint computedFingerprint = FindingFingerprint.compute(
                projectId,
                location.getFilePath(),
                ruleInfo.getRuleId(),
                location.getSnippet()
        );
        Instant now = Instant.now();

        Finding finding = new Finding(
                newId,
                projectId,
                branch,
                commitHash,
                computedFingerprint,
                toolInfo,
                ruleInfo,
                severity,
                location,
                FindingStatus.OPEN,
                now,
                now,
                rawDetails
        );

        finding.registerEvent(new FindingIngestedEvent(newId, projectId, severity, now));
        return finding;
    }

    /**
     * 標記弱點已修復 (Resolved)。
     * <p>
     * 觸發狀態轉移並發布 FindingStatusChangedEvent。
     * </p>
     */
    public void markAsResolved() {
        if (this.status == FindingStatus.RESOLVED) {
            return;
        }
        FindingStatus oldStatus = this.status;
        this.status = FindingStatus.RESOLVED;
        this.lastSeenAt = Instant.now();
        registerEvent(new FindingStatusChangedEvent(this.id, oldStatus, this.status, this.lastSeenAt));
    }

    /**
     * 標記弱點為誤報 (False Positive)。
     */
    public void markAsFalsePositive() {
        if (this.status == FindingStatus.FALSE_POSITIVE) {
            return;
        }
        FindingStatus oldStatus = this.status;
        this.status = FindingStatus.FALSE_POSITIVE;
        registerEvent(new FindingStatusChangedEvent(this.id, oldStatus, this.status, Instant.now()));
    }

    /**
     * 確認該弱點為真實風險 (Confirmed)。
     */
    public void markAsConfirmed() {
        if (this.status == FindingStatus.CONFIRMED) {
            return;
        }
        FindingStatus oldStatus = this.status;
        this.status = FindingStatus.CONFIRMED;
        registerEvent(new FindingStatusChangedEvent(this.id, oldStatus, this.status, Instant.now()));
    }

    /**
     * 刷新最近一次掃描時間戳記。
     *
     * @param scanTime 掃描完成時間
     */
    public void touchScan(Instant scanTime) {
        if (scanTime != null) {
            this.lastSeenAt = scanTime;
        }
    }

    /**
     * 調整嚴重等級。
     *
     * @param newSeverity 新的嚴重等級
     */
    public void adjustSeverity(Severity newSeverity) {
        if (newSeverity != null && this.severity != newSeverity) {
            this.severity = newSeverity;
        }
    }

    /**
     * 註冊領域事件。
     *
     * @param event 領域事件物件
     */
    protected void registerEvent(Object event) {
        if (event != null) {
            this.domainEvents.add(event);
        }
    }

    /**
     * 取得並清空當前累積之領域事件（供發布者取出處理）。
     *
     * @return 領域事件清單
     */
    public List<Object> pullDomainEvents() {
        List<Object> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return Collections.unmodifiableList(events);
    }

    // --- 純 Java Getters (遵循封裝性，嚴禁 Lombok) ---

    public FindingId getId() {
        return id;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getBranch() {
        return branch;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public FindingFingerprint getFingerprint() {
        return fingerprint;
    }

    public ScannerToolInfo getToolInfo() {
        return toolInfo;
    }

    public RuleInfo getRuleInfo() {
        return ruleInfo;
    }

    public Severity getSeverity() {
        return severity;
    }

    public FindingLocation getLocation() {
        return location;
    }

    public FindingStatus getStatus() {
        return status;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public String getRawDetails() {
        return rawDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Finding finding)) return false;
        return Objects.equals(id, finding.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
