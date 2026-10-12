package com.example.scsfacade.infrastructure.persistence.finding.entity;

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
 * 弱點 JPA 持久化實體 (FindingJpaEntity)。
 * <p>
 * 位於基礎設施層，專門負責與關係型資料庫 (PostgreSQL / H2) 表格 mapping。
 * 遵循架構原則：JPA 實體與 Lombok 僅限於基礎設施層，絕不侵入 Domain 聚合根。
 * </p>
 */
@Entity
@Table(name = "findings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FindingJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "project_id", length = 128, nullable = false)
    private String projectId;

    @Column(name = "branch", length = 128)
    private String branch;

    @Column(name = "commit_hash", length = 64)
    private String commitHash;

    @Column(name = "fingerprint", length = 64, nullable = false, unique = true)
    private String fingerprint;

    @Column(name = "tool_name", length = 64, nullable = false)
    private String toolName;

    @Column(name = "tool_version", length = 32)
    private String toolVersion;

    @Column(name = "tool_category", length = 32)
    private String toolCategory;

    @Column(name = "rule_id", length = 128, nullable = false)
    private String ruleId;

    @Column(name = "rule_name", length = 255)
    private String ruleName;

    @Column(name = "cwe_list", length = 255)
    private String cweList; // 以逗號分隔之 CWE 清單

    @Column(name = "cve", length = 64)
    private String cve;

    @Column(name = "severity", length = 32, nullable = false)
    private String severity;

    @Column(name = "file_path", length = 512, nullable = false)
    private String filePath;

    @Column(name = "start_line")
    private Integer startLine;

    @Column(name = "end_line")
    private Integer endLine;

    @Lob
    @Column(name = "snippet")
    private String snippet;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Lob
    @Column(name = "raw_details")
    private String rawDetails;
}
