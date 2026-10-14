package com.example.scsfacade.infrastructure.adapter;

import com.example.scsfacade.application.port.out.ScanExecutionRepositoryPort;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.scanexecution.aggregate.root.ScanExecution;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionId;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionPeriod;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionStatus;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanSummary;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanTriggerType;
import com.example.scsfacade.infrastructure.persistence.scanexecution.entity.ScanExecutionJpaEntity;
import com.example.scsfacade.infrastructure.persistence.scanexecution.repository.ScanExecutionJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 掃描執行倉儲外向適配器 (ScanExecutionRepositoryAdapter)。
 * <p>
 * 遵循 ... + Adapter 命名規範。實作 ScanExecutionRepositoryPort。
 * 負責 JPA Entity 與 ScanExecution 聚合根之間的雙向轉換。
 * </p>
 */
@Component
public class ScanExecutionRepositoryAdapter implements ScanExecutionRepositoryPort {

    private final ScanExecutionJpaRepository jpaRepository;

    public ScanExecutionRepositoryAdapter(ScanExecutionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ScanExecution save(ScanExecution scanExecution) {
        ScanExecutionJpaEntity entity = toEntity(scanExecution);
        ScanExecutionJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ScanExecution> findById(ScanExecutionId id) {
        if (id == null) {
            return Optional.empty();
        }
        return jpaRepository.findById(id.getValue()).map(this::toDomain);
    }

    @Override
    public List<ScanExecution> findByProjectIdAndBranch(String projectId, String branch) {
        return jpaRepository.findByProjectIdAndBranchOrderByStartedAtDesc(projectId, branch).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ScanExecution> findByProjectId(String projectId) {
        return jpaRepository.findByProjectIdOrderByStartedAtDesc(projectId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ScanExecution> findByExternalTaskId(String externalTaskId) {
        if (externalTaskId == null || externalTaskId.trim().isEmpty()) {
            return Optional.empty();
        }
        return jpaRepository.findByExternalTaskId(externalTaskId.trim()).map(this::toDomain);
    }

    // --- Entity 與 Domain 互轉 ---

    private ScanExecutionJpaEntity toEntity(ScanExecution domain) {
        return new ScanExecutionJpaEntity(
                domain.getId().getValue(),
                domain.getProjectId(),
                domain.getBranch(),
                domain.getCommitHash(),
                domain.getToolInfo().getName(),
                domain.getToolInfo().getVersion(),
                domain.getTriggerType().name(),
                domain.getStatus().name(),
                domain.getSummary().getTotalActiveCount(),
                domain.getSummary().getNewCount(),
                domain.getSummary().getExistingCount(),
                domain.getSummary().getResolvedCount(),
                domain.getSummary().getCriticalCount(),
                domain.getSummary().getHighCount(),
                domain.getSummary().isPassedQualityGate(),
                domain.getStartedAt(),
                domain.getCompletedAt(),
                domain.getErrorMessage(),
                domain.getExternalTaskId()
        );
    }

    private ScanExecution toDomain(ScanExecutionJpaEntity entity) {
        ScannerToolInfo toolInfo = new ScannerToolInfo(
                entity.getToolName(),
                entity.getToolVersion(),
                "SAST"
        );

        ScanSummary summary = new ScanSummary(
                entity.getTotalActiveCount(),
                entity.getNewCount(),
                entity.getExistingCount(),
                entity.getResolvedCount(),
                entity.getCriticalCount(),
                entity.getHighCount(),
                entity.isPassedQualityGate()
        );

        ScanExecutionPeriod period = ScanExecutionPeriod.of(
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getErrorMessage()
        );

        return ScanExecution.reconstitute(
                ScanExecutionId.of(entity.getId()),
                entity.getProjectId(),
                entity.getBranch(),
                entity.getCommitHash(),
                toolInfo,
                ScanTriggerType.valueOf(entity.getTriggerType()),
                ScanExecutionStatus.valueOf(entity.getStatus()),
                summary,
                period,
                entity.getExternalTaskId()
        );
    }
}
