package com.example.scsfacade.application.service;

import com.example.scsfacade.application.assembler.ScanExecutionDtoAssembler;
import com.example.scsfacade.application.dto.ScanExecutionGottenResult;
import com.example.scsfacade.application.port.in.GetScanExecutionUseCase;
import com.example.scsfacade.application.port.in.ListScanExecutionsUseCase;
import com.example.scsfacade.application.port.out.ScanExecutionRepositoryPort;
import com.example.scsfacade.application.query.GetScanExecutionQuery;
import com.example.scsfacade.application.query.ListScanExecutionsQuery;
import com.example.scsfacade.domain.scanexecution.aggregate.root.ScanExecution;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionId;
import com.example.scsfacade.domain.scanexecution.exception.ScanExecutionNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 掃描執行查詢應用服務 (ScanExecutionQueryService)。
 * <p>
 * 遵循 ... + QueryService 命名規範。實作 GetScanExecutionUseCase 與 ListScanExecutionsUseCase。
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class ScanExecutionQueryService implements GetScanExecutionUseCase, ListScanExecutionsUseCase {

    private final ScanExecutionRepositoryPort scanExecutionRepositoryPort;
    private final ScanExecutionDtoAssembler scanExecutionDtoAssembler;

    public ScanExecutionQueryService(ScanExecutionRepositoryPort scanExecutionRepositoryPort,
                                    ScanExecutionDtoAssembler scanExecutionDtoAssembler) {
        this.scanExecutionRepositoryPort = scanExecutionRepositoryPort;
        this.scanExecutionDtoAssembler = scanExecutionDtoAssembler;
    }

    @Override
    public ScanExecutionGottenResult execute(GetScanExecutionQuery query) {
        ScanExecution execution = scanExecutionRepositoryPort.findById(ScanExecutionId.of(query.executionId()))
                .orElseThrow(() -> new ScanExecutionNotFoundException(ScanExecutionId.of(query.executionId())));
        return scanExecutionDtoAssembler.toResult(execution);
    }

    @Override
    public List<ScanExecutionGottenResult> execute(ListScanExecutionsQuery query) {
        List<ScanExecution> list;
        if (query.branch() != null && !query.branch().trim().isEmpty()) {
            list = scanExecutionRepositoryPort.findByProjectIdAndBranch(query.projectId(), query.branch().trim());
        } else {
            list = scanExecutionRepositoryPort.findByProjectId(query.projectId());
        }
        return list.stream()
                .map(scanExecutionDtoAssembler::toResult)
                .toList();
    }
}
