package com.example.scsfacade.application.service;

import com.example.scsfacade.application.assembler.FindingDtoAssembler;
import com.example.scsfacade.application.dto.FindingGottenResult;
import com.example.scsfacade.application.port.in.GetFindingUseCase;
import com.example.scsfacade.application.port.out.FindingRepositoryPort;
import com.example.scsfacade.application.query.GetFindingQuery;
import com.example.scsfacade.application.query.ListFindingQuery;
import com.example.scsfacade.application.view.FindingGottenView;
import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingId;
import com.example.scsfacade.domain.finding.exception.FindingNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 弱點查詢服務 (FindingQueryService)。
 * <p>
 * 遵循 ... + QueryService 命名規範。實作 Inbound Port (GetFindingUseCase)。
 * 職責：處理單一弱點詳細資料與多筆清單之只讀查詢，並調用組裝器回傳 DTO / View。
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class FindingQueryService implements GetFindingUseCase {

    private final FindingRepositoryPort findingRepositoryPort;
    private final FindingDtoAssembler findingDtoAssembler;

    public FindingQueryService(FindingRepositoryPort findingRepositoryPort, FindingDtoAssembler findingDtoAssembler) {
        this.findingRepositoryPort = findingRepositoryPort;
        this.findingDtoAssembler = findingDtoAssembler;
    }

    @Override
    public FindingGottenResult getById(GetFindingQuery query) {
        FindingId id = FindingId.of(query.findingId());
        Finding finding = findingRepositoryPort.findById(id)
                .orElseThrow(() -> new FindingNotFoundException(id));
        return findingDtoAssembler.toResult(finding);
    }

    @Override
    public List<FindingGottenView> listByProject(ListFindingQuery query) {
        List<Finding> findings = findingRepositoryPort.findByProjectId(query.projectId());

        if (query.status() != null && !query.status().trim().isEmpty()) {
            String targetStatus = query.status().trim().toUpperCase();
            findings = findings.stream()
                    .filter(f -> f.getStatus().name().equals(targetStatus))
                    .toList();
        }

        return findings.stream()
                .map(findingDtoAssembler::toView)
                .toList();
    }
}
