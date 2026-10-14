package com.example.scsfacade.presentation.controller;

import com.example.scsfacade.application.dto.ScanExecutionGottenResult;
import com.example.scsfacade.application.port.in.GetScanExecutionUseCase;
import com.example.scsfacade.application.port.in.ListScanExecutionsUseCase;
import com.example.scsfacade.application.query.GetScanExecutionQuery;
import com.example.scsfacade.application.query.ListScanExecutionsQuery;
import com.example.scsfacade.presentation.assembler.ScanExecutionResourceAssembler;
import com.example.scsfacade.presentation.resource.out.ScanExecutionGottenResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 掃描執行歷程查詢控制器 (ScanExecutionController)。
 * <p>
 * 表現層 Inbound Adapter。提供 CI/CD Pipeline 掃描歷程與指標查詢端點。
 * </p>
 */
@Tag(name = "Scan Execution API", description = "掃描執行歷程與 CI/CD 快照查詢端點")
@RestController
@RequestMapping("/api/v1/scan-executions")
public class ScanExecutionController {

    private final GetScanExecutionUseCase getScanExecutionUseCase;
    private final ListScanExecutionsUseCase listScanExecutionsUseCase;
    private final ScanExecutionResourceAssembler scanExecutionResourceAssembler;

    public ScanExecutionController(GetScanExecutionUseCase getScanExecutionUseCase,
                                   ListScanExecutionsUseCase listScanExecutionsUseCase,
                                   ScanExecutionResourceAssembler scanExecutionResourceAssembler) {
        this.getScanExecutionUseCase = getScanExecutionUseCase;
        this.listScanExecutionsUseCase = listScanExecutionsUseCase;
        this.scanExecutionResourceAssembler = scanExecutionResourceAssembler;
    }

    /**
     * 依據掃描執行識別碼查詢單筆掃描結果。
     *
     * @param id 掃描識別碼
     * @return 掃描執行回應載體
     */
    @Operation(summary = "依識別碼查詢單次掃描歷程記錄")
    @GetMapping("/{id}")
    public ResponseEntity<ScanExecutionGottenResource> getScanExecution(@PathVariable String id) {
        ScanExecutionGottenResult result = getScanExecutionUseCase.execute(new GetScanExecutionQuery(id));
        return ResponseEntity.ok(scanExecutionResourceAssembler.toResource(result));
    }

    /**
     * 查詢專案之歷史掃描清單。
     *
     * @param projectId 專案代碼
     * @param branch    分支名稱 (選填)
     * @return 掃描執行回應清單
     */
    @Operation(summary = "查詢專案歷史掃描歷程清單")
    @GetMapping
    public ResponseEntity<List<ScanExecutionGottenResource>> listScanExecutions(
            @RequestParam String projectId,
            @RequestParam(required = false) String branch) {
        List<ScanExecutionGottenResult> results = listScanExecutionsUseCase.execute(
                new ListScanExecutionsQuery(projectId, branch));
        List<ScanExecutionGottenResource> response = results.stream()
                .map(scanExecutionResourceAssembler::toResource)
                .toList();
        return ResponseEntity.ok(response);
    }
}
