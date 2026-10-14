package com.example.scsfacade.domain.finding.aggregate.root;

import com.example.scsfacade.domain.event.DomainEvent;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingCategory;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingFingerprint;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingId;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingLifecycle;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingLocation;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingStatus;
import com.example.scsfacade.domain.finding.aggregate.vo.RuleInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import com.example.scsfacade.domain.finding.event.FindingIngestedEvent;
import com.example.scsfacade.domain.finding.event.FindingStatusChangedEvent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 弱點聚合根 (Finding Aggregate Root)。
 * <p>
 * DDD 核心領域模型，封裝單一源碼弱點的業務規則、狀態轉移邏輯與領域事件。
 * 遵守規範：純 Java 實作，零框架、零 JPA、零 Jackson、嚴禁使用 Lombok。
 * 建構子嚴格私有化，物件建立透過業務工廠方法 createNew 或持久化還原工廠 reconstitute。
 * </p>
 */
public class Finding {

    /**
     * 弱點聚合根宇宙唯一識別碼 (FindingId)。
     */
    private final FindingId id;

    /**
     * 歸屬之專案代碼 (Project Identifier)。
     */
    private final String projectId;

    /**
     * 偵測到該弱點之 Git 分支名稱 (Git Branch，例如 "main", "develop")。
     */
    private final String branch;

    /**
     * 弱點去重唯一特徵指紋 (SHA-256 Fingerprint)。
     * <p>
     * 核心去重機制：由專案代碼、分支、檔案路徑、規則 ID 與代碼片段雜湊計算而成，
     * 跨越不同掃描批次精確識別是否為同一筆弱點問題。
     * </p>
     */
    private final FindingFingerprint fingerprint;

    /**
     * 掃描工具元數據 (Scanner Tool Metadata)，記錄分析工具名稱、版本與型態 (如 SAST/DAST)。
     */
    private final ScannerToolInfo toolInfo;

    /**
     * 弱點領域本質分類 (Finding Category，如 VULNERABILITY, CODE_SMELL, BUG, SECURITY_HOTSPOT)。
     */
    private final FindingCategory category;

    /**
     * 掃描規則與安全標準分類資訊 (Rule Metadata，封裝 Rule ID, 規則名稱, CWE 清單, CVE 與標籤)。
     */
    private final RuleInfo ruleInfo;

    /**
     * 標準化嚴重程度等級 (Standardized Severity Level，如 CRITICAL, HIGH, MEDIUM, LOW, INFO)。
     */
    private Severity severity;

    /**
     * 弱點於源碼庫中的實體位置 (Code Location，包含檔案路徑、行號範圍與代碼片段)。
     */
    private final FindingLocation location;

    /**
     * 弱點當前生命週期狀態 (Lifecycle Status，如 OPEN, CONFIRMED, FALSE_POSITIVE, RESOLVED)。
     */
    private FindingStatus status;

    /**
     * 弱點生命週期與 Commit 版本歷程 (Lifecycle Timestamps & Commits)，
     * 記錄首次發現、最近偵測與修復結案時之 Commit 雜湊與時間戳記。
     */
    private FindingLifecycle lifecycle;

    /**
     * 原始掃描工具回傳之完整 JSON 字串，保留底層完整上下文以供審計或除錯。
     */
    private final String rawDetails;

    /**
     * 內部累積之領域事件清單 (In-Memory Domain Event Buffer)。
     */
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * 私有聚合建構子，由工廠方法統一組裝。
     */
    private Finding(FindingId id,
                    String projectId,
                    String branch,
                    FindingFingerprint fingerprint,
                    ScannerToolInfo toolInfo,
                    FindingCategory category,
                    RuleInfo ruleInfo,
                    Severity severity,
                    FindingLocation location,
                    FindingStatus status,
                    FindingLifecycle lifecycle,
                    String rawDetails) {
        this.id = Objects.requireNonNull(id, "FindingId 不可為空");
        this.projectId = Objects.requireNonNull(projectId, "projectId 不可為空");
        this.branch = branch != null ? branch.trim() : "main";
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint 不可為空");
        this.toolInfo = Objects.requireNonNull(toolInfo, "toolInfo 不可為空");
        this.category = category != null ? category : FindingCategory.CODE_SMELL;
        this.ruleInfo = Objects.requireNonNull(ruleInfo, "ruleInfo 不可為空");
        this.severity = Objects.requireNonNull(severity, "severity 不可為空");
        this.location = Objects.requireNonNull(location, "location 不可為空");
        this.status = Objects.requireNonNull(status, "status 不可為空");
        this.lifecycle = Objects.requireNonNull(lifecycle, "FindingLifecycle 不可為空");
        this.rawDetails = rawDetails != null ? rawDetails : "{}";
    }

    /**
     * 業務工廠方法：建立一筆新發現的弱點，並發布 FindingIngestedEvent。
     *
     * @param projectId  專案識別碼
     * @param branch     分支名稱
     * @param commitHash 當前掃描 Commit
     * @param toolInfo   掃描工具資訊
     * @param category   弱點分類
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
                                    FindingCategory category,
                                    RuleInfo ruleInfo,
                                    Severity severity,
                                    FindingLocation location,
                                    String rawDetails) {
        FindingId newId = FindingId.generate();
        String validBranch = branch != null ? branch.trim() : "main";
        FindingFingerprint computedFingerprint = FindingFingerprint.compute(
                projectId,
                validBranch,
                location.getFilePath(),
                ruleInfo.getRuleId(),
                location.getSnippet()
        );
        Instant now = Instant.now();
        FindingLifecycle newLifecycle = FindingLifecycle.init(commitHash, now);

        Finding finding = new Finding(
                newId,
                projectId,
                validBranch,
                computedFingerprint,
                toolInfo,
                category != null ? category : FindingCategory.CODE_SMELL,
                ruleInfo,
                severity,
                location,
                FindingStatus.OPEN,
                newLifecycle,
                rawDetails
        );

        finding.registerEvent(new FindingIngestedEvent(newId, projectId, severity, LocalDateTime.now()));
        return finding;
    }

    /**
     * 業務工廠方法（簡化版，預設分類為 CODE_SMELL）。
     */
    public static Finding createNew(String projectId,
                                    String branch,
                                    String commitHash,
                                    ScannerToolInfo toolInfo,
                                    RuleInfo ruleInfo,
                                    Severity severity,
                                    FindingLocation location,
                                    String rawDetails) {
        return createNew(projectId, branch, commitHash, toolInfo, FindingCategory.CODE_SMELL,
                ruleInfo, severity, location, rawDetails);
    }

    /**
     * 持久化還原工廠方法 (Reconstitution Factory)。
     * <p>
     * 專門供基礎設施層 (FindingRepositoryAdapter) 自資料庫實體完整重建領域聚合根，不發布事件。
     * </p>
     */
    public static Finding reconstitute(FindingId id,
                                       String projectId,
                                       String branch,
                                       FindingFingerprint fingerprint,
                                       ScannerToolInfo toolInfo,
                                       FindingCategory category,
                                       RuleInfo ruleInfo,
                                       Severity severity,
                                       FindingLocation location,
                                       FindingStatus status,
                                       FindingLifecycle lifecycle,
                                       String rawDetails) {
        return new Finding(
                id,
                projectId,
                branch,
                fingerprint,
                toolInfo,
                category,
                ruleInfo,
                severity,
                location,
                status,
                lifecycle,
                rawDetails
        );
    }

    /**
     * 標記弱點已在指定 Commit 修復 (Resolved)。
     *
     * @param commitHash 修復此問題之 Commit 雜湊
     * @param resolvedAt 修復掃描時間戳記
     */
    public void markAsResolved(String commitHash, Instant resolvedAt) {
        if (this.status == FindingStatus.RESOLVED) {
            return;
        }
        FindingStatus oldStatus = this.status;
        this.status = FindingStatus.RESOLVED;
        this.lifecycle = this.lifecycle.resolve(commitHash, resolvedAt);
        registerEvent(new FindingStatusChangedEvent(this.id, oldStatus, this.status, LocalDateTime.now()));
    }

    /**
     * 標記弱點已修復 (Resolved)。
     */
    public void markAsResolved() {
        markAsResolved(this.lifecycle.getLastSeenCommit(), Instant.now());
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
        registerEvent(new FindingStatusChangedEvent(this.id, oldStatus, this.status, LocalDateTime.now()));
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
        registerEvent(new FindingStatusChangedEvent(this.id, oldStatus, this.status, LocalDateTime.now()));
    }

    /**
     * 刷新最近一次掃描時間戳記與當前偵測 Commit。
     *
     * @param commitHash 當前掃描 Commit 雜湊
     * @param scanTime   掃描完成時間
     */
    public void touchScan(String commitHash, Instant scanTime) {
        this.lifecycle = this.lifecycle.touch(commitHash, scanTime);
    }

    public void touchScan(Instant scanTime) {
        touchScan(this.lifecycle.getLastSeenCommit(), scanTime);
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

    protected void registerEvent(DomainEvent event) {
        if (event != null) {
            this.domainEvents.add(event);
        }
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(this.domainEvents);
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

    public FindingFingerprint getFingerprint() {
        return fingerprint;
    }

    public ScannerToolInfo getToolInfo() {
        return toolInfo;
    }

    public FindingCategory getCategory() {
        return category;
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

    public FindingLocation location() {
        return location;
    }

    public FindingStatus getStatus() {
        return status;
    }

    public FindingLifecycle getLifecycle() {
        return lifecycle;
    }

    public String getFirstSeenCommit() {
        return lifecycle.getFirstSeenCommit();
    }

    public String getLastSeenCommit() {
        return lifecycle.getLastSeenCommit();
    }

    public String getResolvedInCommit() {
        return lifecycle.getResolvedInCommit();
    }

    public String getCommitHash() {
        return lifecycle.getLastSeenCommit();
    }

    public Instant getFirstSeenAt() {
        return lifecycle.getFirstSeenAt();
    }

    public Instant getLastSeenAt() {
        return lifecycle.getLastSeenAt();
    }

    public Instant getResolvedAt() {
        return lifecycle.getResolvedAt();
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
