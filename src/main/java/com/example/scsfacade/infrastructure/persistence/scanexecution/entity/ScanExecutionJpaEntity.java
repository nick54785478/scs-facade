package com.example.scsfacade.infrastructure.persistence.scanexecution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 掃描執行 JPA 持久化實體 (ScanExecutionJpaEntity)。
 * <p>
 * 位於基礎設施層，專門負責與關係型資料庫 scan_executions 表格 mapping。
 * </p>
 */
@Entity
@Table(name = "scan_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScanExecutionJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "project_id", length = 128, nullable = false)
    private String projectId;

    @Column(name = "branch", length = 128, nullable = false)
    private String branch;

    @Column(name = "commit_hash", length = 64)
    private String commitHash;

    @Column(name = "tool_name", length = 64, nullable = false)
    private String toolName;

    @Column(name = "tool_version", length = 32)
    private String toolVersion;

    @Column(name = "trigger_type", length = 32, nullable = false)
    private String triggerType;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "total_active_count", nullable = false)
    private int totalActiveCount;

    @Column(name = "new_count", nullable = false)
    private int newCount;

    @Column(name = "existing_count", nullable = false)
    private int existingCount;

    @Column(name = "resolved_count", nullable = false)
    private int resolvedCount;

    @Column(name = "critical_count", nullable = false)
    private int criticalCount;

    @Column(name = "high_count", nullable = false)
    private int highCount;

    @Column(name = "passed_quality_gate", nullable = false)
    private boolean passedQualityGate;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "external_task_id", length = 128)
    private String externalTaskId;
}
