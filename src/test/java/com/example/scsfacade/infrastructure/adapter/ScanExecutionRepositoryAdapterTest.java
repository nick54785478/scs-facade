package com.example.scsfacade.infrastructure.adapter;

import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.scanexecution.aggregate.root.ScanExecution;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionId;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionPeriod;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionStatus;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanSummary;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanTriggerType;
import com.example.scsfacade.infrastructure.persistence.scanexecution.entity.ScanExecutionJpaEntity;
import com.example.scsfacade.infrastructure.persistence.scanexecution.repository.ScanExecutionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 掃描執行倉儲適配器單元測試 (ScanExecutionRepositoryAdapterTest)。
 * <p>
 * 驗證 ScanExecutionRepositoryAdapter 在 JPA Entity 與 Domain Aggregate 間之雙向轉換與查詢轉發。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ScanExecutionRepositoryAdapterTest {

    @Mock
    private ScanExecutionJpaRepository jpaRepository;

    @InjectMocks
    private ScanExecutionRepositoryAdapter adapter;

    private ScanExecutionJpaEntity sampleEntity;

    @BeforeEach
    void setUp() {
        sampleEntity = new ScanExecutionJpaEntity(
                "exec-101",
                "scs-facade",
                "main",
                "commit-123",
                "SonarQube",
                "10.0",
                "WEBHOOK",
                "SUCCESS",
                10,
                2,
                8,
                1,
                0,
                3,
                true,
                Instant.now(),
                Instant.now(),
                null,
                "task-abc-123"
        );
    }

    /**
     * Feature: 儲存掃描執行聚合根
     * Scenario: 呼叫 save 成功將領域聚合根轉換為 JPA Entity 並寫入資料庫
     * Given: 傳入一筆有效之 ScanExecution 聚合根
     * When: 調用 adapter.save
     * Then: 正確轉交 jpaRepository.save 並回傳轉換後之 ScanExecution
     */
    @Test
    @DisplayName("儲存聚合根 - 應正確持久化並回傳映射之領域模型")
    void shouldSaveAndReturnDomainModel() {
        // Given
        ScanExecution domain = ScanExecution.start(
                "scs-facade",
                "main",
                "commit-123",
                new ScannerToolInfo("SonarQube", "10.0", "SAST"),
                ScanTriggerType.WEBHOOK,
                "task-abc-123"
        );
        when(jpaRepository.save(any(ScanExecutionJpaEntity.class))).thenReturn(sampleEntity);

        // When
        ScanExecution saved = adapter.save(domain);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId().getValue()).isEqualTo("exec-101");
        assertThat(saved.getExternalTaskId()).isEqualTo("task-abc-123");
        verify(jpaRepository).save(any(ScanExecutionJpaEntity.class));
    }

    /**
     * Feature: 依識別碼查詢掃描執行歷程
     * Scenario: 當 ID 存在時回傳含資料之 Optional
     * Given: jpaRepository 查得該筆資料
     * When: 調用 adapter.findById
     * Then: 回傳非空 Optional，屬性正確映射
     */
    @Test
    @DisplayName("依 ID 查詢 - 存在時應正確轉換為領域模型")
    void shouldFindByIdWhenExists() {
        // Given
        when(jpaRepository.findById("exec-101")).thenReturn(Optional.of(sampleEntity));

        // When
        Optional<ScanExecution> found = adapter.findById(ScanExecutionId.of("exec-101"));

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getProjectId()).isEqualTo("scs-facade");
        assertThat(found.get().getStatus()).isEqualTo(ScanExecutionStatus.SUCCESS);
    }

    /**
     * Feature: 依識別碼查詢掃描執行歷程
     * Scenario: 當 ID 為 null 時安全回傳 empty
     */
    @Test
    @DisplayName("依 ID 查詢 - 傳入 null 時應安全回傳 empty")
    void shouldReturnEmptyWhenIdIsNull() {
        // When
        Optional<ScanExecution> found = adapter.findById(null);

        // Then
        assertThat(found).isEmpty();
    }

    /**
     * Feature: 依 externalTaskId 查詢掃描執行（冪等性校驗用）
     * Scenario: 查詢已存在的 externalTaskId
     * Given: 資料庫存在對應 externalTaskId
     * When: 調用 adapter.findByExternalTaskId
     * Then: 正確回傳該聚合根
     */
    @Test
    @DisplayName("依 externalTaskId 查詢 - 存在時應正確回傳")
    void shouldFindByExternalTaskId() {
        // Given
        when(jpaRepository.findByExternalTaskId("task-abc-123")).thenReturn(Optional.of(sampleEntity));

        // When
        Optional<ScanExecution> found = adapter.findByExternalTaskId("task-abc-123");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getExternalTaskId()).isEqualTo("task-abc-123");
    }

    /**
     * Feature: 依專案代碼與分支查詢掃描執行歷程
     * Scenario: 查詢專案歷史清單
     */
    @Test
    @DisplayName("依專案與分支查詢 - 應回傳轉換後之清單")
    void shouldFindByProjectIdAndBranch() {
        // Given
        when(jpaRepository.findByProjectIdAndBranchOrderByStartedAtDesc("scs-facade", "main"))
                .thenReturn(List.of(sampleEntity));

        // When
        List<ScanExecution> list = adapter.findByProjectIdAndBranch("scs-facade", "main");

        // Then
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId().getValue()).isEqualTo("exec-101");
    }

    /**
     * Feature: 依專案代碼查詢所有歷史掃描清單
     */
    @Test
    @DisplayName("依專案查詢所有歷史 - 應回傳轉換後之清單")
    void shouldFindByProjectId() {
        // Given
        when(jpaRepository.findByProjectIdOrderByStartedAtDesc("scs-facade"))
                .thenReturn(List.of(sampleEntity));

        // When
        List<ScanExecution> list = adapter.findByProjectId("scs-facade");

        // Then
        assertThat(list).hasSize(1);
    }
}
