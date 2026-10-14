package com.example.scsfacade.infrastructure.adapter;

import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingCategory;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingFingerprint;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingId;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingLocation;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingStatus;
import com.example.scsfacade.domain.finding.aggregate.vo.RuleInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import com.example.scsfacade.infrastructure.persistence.finding.entity.FindingJpaEntity;
import com.example.scsfacade.infrastructure.persistence.finding.repository.FindingJpaRepository;
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
 * 弱點儲存庫外向適配器單元測試 (FindingRepositoryAdapterTest)。
 * <p>
 * 驗證 FindingRepositoryAdapter 在 JPA Entity 與 Domain Aggregate 間之雙向轉換與查詢轉發。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class FindingRepositoryAdapterTest {

    @Mock
    private FindingJpaRepository jpaRepository;

    @InjectMocks
    private FindingRepositoryAdapter adapter;

    private FindingJpaEntity sampleEntity;

    @BeforeEach
    void setUp() {
        sampleEntity = new FindingJpaEntity(
                "finding-001",
                "scs-facade",
                "main",
                "commit-100",
                "commit-100",
                "commit-100",
                null,
                "fp-12345",
                "SonarQube",
                "10.0",
                "SAST",
                "VULNERABILITY",
                "java:S2077",
                "SQL Injection",
                "CWE-89",
                null,
                "sql,security",
                "HIGH",
                "src/OrderService.java",
                42,
                45,
                "SELECT * FROM orders",
                "OPEN",
                Instant.now(),
                Instant.now(),
                null,
                "{}"
        );
    }

    /**
     * Feature: 儲存單筆弱點聚合根
     * Scenario: 呼叫 save 成功將 Finding 聚合根轉為 Entity 並寫入
     */
    @Test
    @DisplayName("儲存單筆弱點 - 應正確持久化並映射回領域模型")
    void shouldSaveAndReturnDomainFinding() {
        // Given
        Finding domain = Finding.createNew(
                "scs-facade",
                "main",
                "commit-100",
                new ScannerToolInfo("SonarQube", "10.0", "SAST"),
                FindingCategory.VULNERABILITY,
                new RuleInfo("java:S2077", "SQL Injection", List.of("CWE-89"), null, List.of("sql")),
                Severity.HIGH,
                new FindingLocation("src/OrderService.java", 42, 45, "SELECT * FROM orders"),
                "{}"
        );
        when(jpaRepository.save(any(FindingJpaEntity.class))).thenReturn(sampleEntity);

        // When
        Finding saved = adapter.save(domain);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId().getValue()).isEqualTo("finding-001");
        assertThat(saved.getFingerprint().getValue()).isEqualTo("fp-12345");
        verify(jpaRepository).save(any(FindingJpaEntity.class));
    }

    /**
     * Feature: 批次儲存弱點聚合根清單
     * Scenario: 呼叫 saveAll
     */
    @Test
    @DisplayName("批次儲存弱點 - 應批次保存並回傳領域清單")
    void shouldSaveAllFindings() {
        // Given
        when(jpaRepository.saveAll(any())).thenReturn(List.of(sampleEntity));

        // When
        List<Finding> savedList = adapter.saveAll(List.of(Finding.createNew(
                "scs-facade", "main", "c1",
                new ScannerToolInfo("Sonar", "1", "SAST"),
                FindingCategory.CODE_SMELL,
                new RuleInfo("r1", "rule", List.of(), null, List.of()),
                Severity.LOW,
                new FindingLocation("a.java", 1, 2, "code"),
                "{}"
        )));

        // Then
        assertThat(savedList).hasSize(1);
    }

    /**
     * Feature: 依識別碼查詢弱點
     * Scenario: ID 存在時回傳對應弱點
     */
    @Test
    @DisplayName("依 ID 查詢弱點 - 存在時應正確回傳")
    void shouldFindByIdWhenExists() {
        // Given
        when(jpaRepository.findById("finding-001")).thenReturn(Optional.of(sampleEntity));

        // When
        Optional<Finding> found = adapter.findById(FindingId.of("finding-001"));

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId().getValue()).isEqualTo("finding-001");
    }

    /**
     * Feature: 依指紋查詢弱點
     * Scenario: 指紋存在時回傳對應弱點
     */
    @Test
    @DisplayName("依指紋查詢弱點 - 存在時應正確回傳")
    void shouldFindByFingerprintWhenExists() {
        // Given
        when(jpaRepository.findByFingerprint("fp-12345")).thenReturn(Optional.of(sampleEntity));

        // When
        Optional<Finding> found = adapter.findByFingerprint(FindingFingerprint.of("fp-12345"));

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getFingerprint().getValue()).isEqualTo("fp-12345");
    }

    /**
     * Feature: 查詢專案活躍狀態之弱點
     * Scenario: 呼叫 findActiveByProjectAndBranch 回傳 OPEN/CONFIRMED 狀態弱點
     */
    @Test
    @DisplayName("查詢專案活躍弱點 - 應正確過濾狀態")
    void shouldFindActiveByProjectAndBranch() {
        // Given
        when(jpaRepository.findByProjectIdAndBranchAndStatusIn(any(), any(), any()))
                .thenReturn(List.of(sampleEntity));

        // When
        List<Finding> activeFindings = adapter.findActiveByProjectAndBranch("scs-facade", "main");

        // Then
        assertThat(activeFindings).hasSize(1);
        assertThat(activeFindings.get(0).getStatus()).isEqualTo(FindingStatus.OPEN);
    }
}
