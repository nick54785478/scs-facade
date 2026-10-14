package com.example.scsfacade.infrastructure.persistence.finding.repository;

import com.example.scsfacade.infrastructure.persistence.finding.entity.FindingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 弱點 Spring Data JPA 資料訪問介面 (FindingJpaRepository)。
 * <p>
 * 專門負責資料庫底層 SQL 與 ORM 操作。
 * </p>
 */
@Repository
public interface FindingJpaRepository extends JpaRepository<FindingJpaEntity, String> {

    /**
     * 依指紋雜湊值查詢弱點紀錄。
     *
     * @param fingerprint SHA-256 指紋
     * @return 可能存在的實體
     */
    Optional<FindingJpaEntity> findByFingerprint(String fingerprint);

    /**
     * 依專案代碼查詢所有弱點記錄。
     *
     * @param projectId 專案代碼
     * @return 實體清單
     */
    List<FindingJpaEntity> findByProjectId(String projectId);

    /**
     * 依專案代碼與狀態清單查詢弱點記錄。
     *
     * @param projectId 專案代碼
     * @param statuses  狀態清單 (例如 OPEN, CONFIRMED)
     * @return 實體清單
     */
    List<FindingJpaEntity> findByProjectIdAndStatusIn(String projectId, List<String> statuses);

    /**
     * 依專案代碼、分支與狀態清單查詢弱點記錄。
     *
     * @param projectId 專案代碼
     * @param branch    分支名稱
     * @param statuses  狀態清單
     * @return 實體清單
     */
    List<FindingJpaEntity> findByProjectIdAndBranchAndStatusIn(String projectId, String branch, List<String> statuses);
}
