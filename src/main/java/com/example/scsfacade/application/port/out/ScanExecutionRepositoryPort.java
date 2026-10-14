package com.example.scsfacade.application.port.out;

import com.example.scsfacade.domain.scanexecution.aggregate.root.ScanExecution;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionId;

import java.util.List;
import java.util.Optional;

/**
 * 掃描執行倉儲外向埠 (ScanExecutionRepositoryPort)。
 * <p>
 * 遵循 ... + Port 命名規範。定義應用層對掃描批次持久化基礎設施之合約。
 * 遵守規範：純 Java 介面，方法傳參與回傳值嚴禁涉及技術或 ORM 類物件。
 * </p>
 */
public interface ScanExecutionRepositoryPort {

    /**
     * 儲存掃描執行紀錄。
     *
     * @param scanExecution 聚合根物件
     * @return 儲存後之聚合根
     */
    ScanExecution save(ScanExecution scanExecution);

    /**
     * 依據識別碼查詢掃描執行紀錄。
     *
     * @param id 掃描識別碼
     * @return 掃描執行聚合根 Optional
     */
    Optional<ScanExecution> findById(ScanExecutionId id);

    /**
     * 依專案代碼與分支查詢歷史掃描執行紀錄清單。
     *
     * @param projectId 專案代碼
     * @param branch    分支名稱
     * @return 歷史掃描清單
     */
    List<ScanExecution> findByProjectIdAndBranch(String projectId, String branch);

    /**
     * 依專案代碼查詢歷史掃描執行紀錄清單。
     *
     * @param projectId 專案代碼
     * @return 歷史掃描清單
     */
    List<ScanExecution> findByProjectId(String projectId);

    /**
     * 依外部任務識別碼或冪等鍵查詢掃描執行紀錄。
     *
     * @param externalTaskId 外部任務識別碼 (如 SonarQube taskId)
     * @return 掃描執行聚合根 Optional
     */
    Optional<ScanExecution> findByExternalTaskId(String externalTaskId);
}
