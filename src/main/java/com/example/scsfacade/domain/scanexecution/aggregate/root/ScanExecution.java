package com.example.scsfacade.domain.scanexecution.aggregate.root;

import com.example.scsfacade.domain.event.DomainEvent;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionId;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionPeriod;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionStatus;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanSummary;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanTriggerType;
import com.example.scsfacade.domain.scanexecution.event.ScanExecutionCompletedEvent;
import com.example.scsfacade.domain.scanexecution.event.ScanExecutionStartedEvent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 掃描執行批次聚合根 (ScanExecution Aggregate Root)。
 * <p>
 * DDD 核心領域模型，代表每一次 CI/CD Pipeline 觸發或手動發起的掃描歷程。
 * 遵循規範：純 Java 實作，零框架、零 JPA、零 Jackson、嚴禁使用 Lombok。
 * 建構子嚴格私有化，物件建立透過業務工廠方法 start 或持久化還原工廠 reconstitute。
 * </p>
 */
public class ScanExecution {

    /**
     * 掃描執行批次聚合根宇宙唯一識別碼 (ScanExecutionId)。
     */
    private final ScanExecutionId id;

    /**
     * 執行掃描之專案代碼 (Project Identifier)。
     */
    private final String projectId;

    /**
     * 掃描目標分支名稱 (Target Branch，例如 "main", "feature/auth")。
     */
    private final String branch;

    /**
     * 觸發該次掃描之源碼版本 Commit 雜湊值 (Commit Hash)。
     */
    private final String commitHash;

    /**
     * 負責執行該次檢測之掃描工具元數據 (Scanner Tool Metadata，如 SonarQube 10.x SAST)。
     */
    private final ScannerToolInfo toolInfo;

    /**
     * 觸發此掃描作業之事件來源類型 (Scan Trigger Type，如 PIPELINE, WEBHOOK, MANUAL, SCHEDULED)。
     */
    private final ScanTriggerType triggerType;

    /**
     * 外部掃描任務識別碼或冪等鍵 (External Task ID / Idempotency Key，如 SonarQube taskId)。
     */
    private final String externalTaskId;

    /**
     * 當前掃描批次執行狀態 (Execution Status，如 RUNNING, SUCCESS, FAILED, TIMEOUT)。
     */
    private ScanExecutionStatus status;

    /**
     * 掃描指標統計結算與門禁判定摘要 (Scan Summary & Metrics)。
     * <p>
     * 記錄該次掃描相較前版的總活躍弱點數、新發現數、既有數、修復數、重大風險數與門禁合格與否。
     * </p>
     */
    private ScanSummary summary;

    /**
     * 掃描執行期間與時間戳記記錄 (Execution Period & Duration)，
     * 封裝啟動時間、完成時間、執行耗時計算以及失敗時之錯誤訊息。
     */
    private ScanExecutionPeriod period;

    /**
     * 內部累積之領域事件清單 (In-Memory Domain Event Buffer)。
     */
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * 私有聚合建構子，由工廠方法統一組裝。
     */
    private ScanExecution(ScanExecutionId id,
                          String projectId,
                          String branch,
                          String commitHash,
                          ScannerToolInfo toolInfo,
                          ScanTriggerType triggerType,
                          ScanExecutionStatus status,
                          ScanSummary summary,
                          ScanExecutionPeriod period,
                          String externalTaskId) {
        this.id = Objects.requireNonNull(id, "ScanExecutionId 不可為空");
        this.projectId = Objects.requireNonNull(projectId, "projectId 不可為空");
        this.branch = branch != null ? branch.trim() : "main";
        this.commitHash = commitHash != null ? commitHash.trim() : "";
        this.toolInfo = Objects.requireNonNull(toolInfo, "toolInfo 不可為空");
        this.triggerType = Objects.requireNonNull(triggerType, "triggerType 不可為空");
        this.status = Objects.requireNonNull(status, "status 不可為空");
        this.summary = summary != null ? summary : ScanSummary.empty();
        this.period = Objects.requireNonNull(period, "ScanExecutionPeriod 不可為空");
        this.externalTaskId = (externalTaskId != null && !externalTaskId.trim().isEmpty()) ? externalTaskId.trim() : null;
    }

    /**
     * 業務工廠方法：啟動一筆新的掃描執行紀錄，並發布 ScanExecutionStartedEvent。
     *
     * @param projectId   專案代碼
     * @param branch      分支名稱
     * @param commitHash  提交雜湊
     * @param toolInfo    掃描工具資訊
     * @param triggerType 觸發類型
     * @return 新建的 ScanExecution 實例
     */
    public static ScanExecution start(String projectId,
                                      String branch,
                                      String commitHash,
                                      ScannerToolInfo toolInfo,
                                      ScanTriggerType triggerType) {
        return start(projectId, branch, commitHash, toolInfo, triggerType, null);
    }

    /**
     * 業務工廠方法：支援指定外部任務或冪等鍵啟動掃描執行紀錄。
     *
     * @param projectId      專案代碼
     * @param branch         分支名稱
     * @param commitHash     提交雜湊
     * @param toolInfo       掃描工具資訊
     * @param triggerType    觸發類型
     * @param externalTaskId 外部任務識別碼或冪等鍵 (如 SonarQube taskId)
     * @return 新建的 ScanExecution 實例
     */
    public static ScanExecution start(String projectId,
                                      String branch,
                                      String commitHash,
                                      ScannerToolInfo toolInfo,
                                      ScanTriggerType triggerType,
                                      String externalTaskId) {
        ScanExecutionId newId = ScanExecutionId.generate();
        Instant now = Instant.now();
        ScanExecutionPeriod newPeriod = ScanExecutionPeriod.start(now);

        ScanExecution execution = new ScanExecution(
                newId,
                projectId,
                branch,
                commitHash,
                toolInfo,
                triggerType,
                ScanExecutionStatus.RUNNING,
                ScanSummary.empty(),
                newPeriod,
                externalTaskId
        );

        execution.registerEvent(new ScanExecutionStartedEvent(
                newId,
                projectId,
                execution.getBranch(),
                execution.getCommitHash(),
                LocalDateTime.now()
        ));
        return execution;
    }

    /**
     * 持久化還原工廠方法 (Reconstitution Factory)。
     * <p>
     * 專門供基礎設施層 (ScanExecutionRepositoryAdapter) 自資料庫實體完整重建領域聚合根。
     * </p>
     */
    public static ScanExecution reconstitute(ScanExecutionId id,
                                             String projectId,
                                             String branch,
                                             String commitHash,
                                             ScannerToolInfo toolInfo,
                                             ScanTriggerType triggerType,
                                             ScanExecutionStatus status,
                                             ScanSummary summary,
                                             ScanExecutionPeriod period) {
        return reconstitute(id, projectId, branch, commitHash, toolInfo, triggerType, status, summary, period, null);
    }

    /**
     * 持久化還原工廠方法 (Reconstitution Factory，支援外部任務識別碼)。
     */
    public static ScanExecution reconstitute(ScanExecutionId id,
                                             String projectId,
                                             String branch,
                                             String commitHash,
                                             ScannerToolInfo toolInfo,
                                             ScanTriggerType triggerType,
                                             ScanExecutionStatus status,
                                             ScanSummary summary,
                                             ScanExecutionPeriod period,
                                             String externalTaskId) {
        return new ScanExecution(
                id,
                projectId,
                branch,
                commitHash,
                toolInfo,
                triggerType,
                status,
                summary,
                period,
                externalTaskId
        );
    }

    /**
     * 完成掃描執行並更新統計數據與門禁判定。
     *
     * @param scanSummary 掃描統計與判定結果
     */
    public void complete(ScanSummary scanSummary) {
        if (this.status == ScanExecutionStatus.SUCCESS) {
            return;
        }
        Instant now = Instant.now();
        this.status = ScanExecutionStatus.SUCCESS;
        this.summary = Objects.requireNonNull(scanSummary, "ScanSummary 不可為空");
        this.period = this.period.complete(now);

        registerEvent(new ScanExecutionCompletedEvent(
                this.id,
                this.projectId,
                this.branch,
                this.commitHash,
                this.summary.isPassedQualityGate(),
                this.summary,
                LocalDateTime.now()
        ));
    }

    /**
     * 標記掃描失敗。
     *
     * @param errorMsg 錯誤訊息
     */
    public void fail(String errorMsg) {
        this.status = ScanExecutionStatus.FAILED;
        this.period = this.period.fail(errorMsg, Instant.now());
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

    // --- 純 Java Getters ---

    public ScanExecutionId getId() {
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

    public ScannerToolInfo getToolInfo() {
        return toolInfo;
    }

    public ScanTriggerType getTriggerType() {
        return triggerType;
    }

    public ScanExecutionStatus getStatus() {
        return status;
    }

    public ScanSummary getSummary() {
        return summary;
    }

    public ScanExecutionPeriod getPeriod() {
        return period;
    }

    public Instant getStartedAt() {
        return period.getStartedAt();
    }

    public Instant getCompletedAt() {
        return period.getCompletedAt();
    }

    public String getErrorMessage() {
        return period.getErrorMessage();
    }

    public String getExternalTaskId() {
        return externalTaskId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScanExecution that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
