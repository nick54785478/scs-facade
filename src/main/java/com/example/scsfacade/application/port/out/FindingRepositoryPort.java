package com.example.scsfacade.application.port.out;

import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingFingerprint;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingId;

import java.util.List;
import java.util.Optional;

/**
 * 弱點儲存庫外向埠 (FindingRepositoryPort)。
 * <p>
 * 遵循 ... + Port 命名規範。應用層要求持久化基礎設施提供的合約。
 * 遵守規範：所有傳參及回傳型別嚴禁使用 JPA Entity 等技術物件，僅接收與回傳領域物件。
 * </p>
 */
public interface FindingRepositoryPort {

    /**
     * 儲存單一弱點聚合根。
     *
     * @param finding 領域聚合根
     * @return 儲存後的領域聚合根
     */
    Finding save(Finding finding);

    /**
     * 批次儲存弱點聚合根清單。
     *
     * @param findings 領域聚合根清單
     * @return 儲存後的領域聚合根清單
     */
    List<Finding> saveAll(List<Finding> findings);

    /**
     * 依弱點 ID 查詢領域聚合根。
     *
     * @param id 弱點 ID
     * @return 可能存在的 Finding 聚合根
     */
    Optional<Finding> findById(FindingId id);

    /**
     * 依指紋查詢領域聚合根（用於去重比對）。
     *
     * @param fingerprint 弱點指紋實質物件
     * @return 可能存在的 Finding 聚合根
     */
    Optional<Finding> findByFingerprint(FindingFingerprint fingerprint);

    /**
     * 依專案代碼查詢所有弱點。
     *
     * @param projectId 專案代碼
     * @return 該專案之所有弱點領域聚合根清單
     */
    List<Finding> findByProjectId(String projectId);

    /**
     * 依專案代碼查詢所有待處理（活躍中）的弱點。
     *
     * @param projectId 專案代碼
     * @return 該專案之活躍弱點領域聚合根清單
     */
    List<Finding> findActiveByProjectId(String projectId);
}
