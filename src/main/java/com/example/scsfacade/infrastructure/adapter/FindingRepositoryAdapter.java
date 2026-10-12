package com.example.scsfacade.infrastructure.adapter;

import com.example.scsfacade.application.port.out.FindingRepositoryPort;
import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingFingerprint;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingId;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingLocation;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingStatus;
import com.example.scsfacade.domain.finding.aggregate.vo.RuleInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;
import com.example.scsfacade.infrastructure.persistence.finding.entity.FindingJpaEntity;
import com.example.scsfacade.infrastructure.persistence.finding.repository.FindingJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 弱點儲存庫外向適配器 (FindingRepositoryAdapter)。
 * <p>
 * 遵循 ... + Adapter 命名規範。實作 FindingRepositoryPort。
 * 職責：協調底層 FindingJpaRepository，負責 JPA Entity 與 Domain Aggregate (Finding) 之間的雙向映射。
 * </p>
 */
@Component
public class FindingRepositoryAdapter implements FindingRepositoryPort {

    private final FindingJpaRepository jpaRepository;

    public FindingRepositoryAdapter(FindingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Finding save(Finding finding) {
        FindingJpaEntity entity = toEntity(finding);
        FindingJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<Finding> saveAll(List<Finding> findings) {
        if (findings == null || findings.isEmpty()) {
            return Collections.emptyList();
        }
        List<FindingJpaEntity> entities = findings.stream().map(this::toEntity).toList();
        return jpaRepository.saveAll(entities).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Finding> findById(FindingId id) {
        if (id == null) {
            return Optional.empty();
        }
        return jpaRepository.findById(id.getValue()).map(this::toDomain);
    }

    @Override
    public Optional<Finding> findByFingerprint(FindingFingerprint fingerprint) {
        if (fingerprint == null) {
            return Optional.empty();
        }
        return jpaRepository.findByFingerprint(fingerprint.getValue()).map(this::toDomain);
    }

    @Override
    public List<Finding> findByProjectId(String projectId) {
        return jpaRepository.findByProjectId(projectId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Finding> findActiveByProjectId(String projectId) {
        List<String> activeStatuses = List.of(FindingStatus.OPEN.name(), FindingStatus.CONFIRMED.name());
        return jpaRepository.findByProjectIdAndStatusIn(projectId, activeStatuses).stream()
                .map(this::toDomain)
                .toList();
    }

    // --- Entity 與 Domain 互轉方法 ---

    private FindingJpaEntity toEntity(Finding domain) {
        String cweJoined = domain.getRuleInfo().getCweList() != null
                ? String.join(",", domain.getRuleInfo().getCweList())
                : "";

        return new FindingJpaEntity(
                domain.getId().getValue(),
                domain.getProjectId(),
                domain.getBranch(),
                domain.getCommitHash(),
                domain.getFingerprint().getValue(),
                domain.getToolInfo().getName(),
                domain.getToolInfo().getVersion(),
                domain.getToolInfo().getCategory(),
                domain.getRuleInfo().getRuleId(),
                domain.getRuleInfo().getName(),
                cweJoined,
                domain.getRuleInfo().getCve(),
                domain.getSeverity().name(),
                domain.getLocation().getFilePath(),
                domain.getLocation().getStartLine(),
                domain.getLocation().getEndLine(),
                domain.getLocation().getSnippet(),
                domain.getStatus().name(),
                domain.getFirstSeenAt(),
                domain.getLastSeenAt(),
                domain.getRawDetails()
        );
    }

    private Finding toDomain(FindingJpaEntity entity) {
        List<String> cweList = entity.getCweList() != null && !entity.getCweList().isEmpty()
                ? Arrays.asList(entity.getCweList().split(","))
                : Collections.emptyList();

        RuleInfo ruleInfo = new RuleInfo(
                entity.getRuleId(),
                entity.getRuleName(),
                cweList,
                entity.getCve()
        );

        ScannerToolInfo toolInfo = new ScannerToolInfo(
                entity.getToolName(),
                entity.getToolVersion(),
                entity.getToolCategory()
        );

        FindingLocation location = new FindingLocation(
                entity.getFilePath(),
                entity.getStartLine(),
                entity.getEndLine(),
                entity.getSnippet()
        );

        return new Finding(
                FindingId.of(entity.getId()),
                entity.getProjectId(),
                entity.getBranch(),
                entity.getCommitHash(),
                FindingFingerprint.of(entity.getFingerprint()),
                toolInfo,
                ruleInfo,
                Severity.valueOf(entity.getSeverity()),
                location,
                FindingStatus.valueOf(entity.getStatus()),
                entity.getFirstSeenAt(),
                entity.getLastSeenAt(),
                entity.getRawDetails()
        );
    }
}
