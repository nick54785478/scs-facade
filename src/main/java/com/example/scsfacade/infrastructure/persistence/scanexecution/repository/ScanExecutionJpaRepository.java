package com.example.scsfacade.infrastructure.persistence.scanexecution.repository;

import com.example.scsfacade.infrastructure.persistence.scanexecution.entity.ScanExecutionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 掃描執行 Spring Data JPA 資料訪問介面 (ScanExecutionJpaRepository)。
 */
@Repository
public interface ScanExecutionJpaRepository extends JpaRepository<ScanExecutionJpaEntity, String> {

    /**
     * 依專案代碼與分支查詢掃描執行清單（依開始時間降冪排列）。
     */
    List<ScanExecutionJpaEntity> findByProjectIdAndBranchOrderByStartedAtDesc(String projectId, String branch);

    /**
     * 依專案代碼查詢掃描執行清單（依開始時間降冪排列）。
     */
    List<ScanExecutionJpaEntity> findByProjectIdOrderByStartedAtDesc(String projectId);

    /**
     * 依外部任務識別碼或冪等鍵查詢掃描執行實體。
     */
    Optional<ScanExecutionJpaEntity> findByExternalTaskId(String externalTaskId);
}
