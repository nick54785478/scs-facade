package com.example.scsfacade.presentation.controller;

import com.example.scsfacade.application.command.ReconcileAllProjectsCommand;
import com.example.scsfacade.application.command.ReconcileProjectScanCommand;
import com.example.scsfacade.application.dto.ScanReconciliationGottenResult;
import com.example.scsfacade.application.port.in.ReconcileAllProjectsUseCase;
import com.example.scsfacade.application.port.in.ReconcileProjectScanUseCase;
import com.example.scsfacade.presentation.assembler.ScanReconciliationResourceAssembler;
import com.example.scsfacade.presentation.resource.in.ReconcileProjectScanResource;
import com.example.scsfacade.presentation.resource.out.ScanReconciledResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 掃描對帳表現層控制器 (ScanReconciliationController)。
 * <p>
 * 表現層 Inbound Adapter。提供 REST API 供管理員或維運系統手動觸發單一專案或全專案之低頻狀態對帳，
 * 達成外部掃描伺服器與本地 Facade 之最終一致性。
 * </p>
 */
@RestController
@RequestMapping("/api/v1/scans")
@Tag(name = "Scan Reconciliation", description = "掃描狀態低頻對帳與最終一致性補償端點")
public class ScanReconciliationController {

    private final ReconcileProjectScanUseCase reconcileProjectScanUseCase;
    private final ReconcileAllProjectsUseCase reconcileAllProjectsUseCase;
    private final ScanReconciliationResourceAssembler assembler;

    public ScanReconciliationController(ReconcileProjectScanUseCase reconcileProjectScanUseCase,
                                        ReconcileAllProjectsUseCase reconcileAllProjectsUseCase,
                                        ScanReconciliationResourceAssembler assembler) {
        this.reconcileProjectScanUseCase = reconcileProjectScanUseCase;
        this.reconcileAllProjectsUseCase = reconcileAllProjectsUseCase;
        this.assembler = assembler;
    }

    /**
     * 手動觸發單一專案低頻對帳。
     *
     * @param resource 專案對帳請求載體
     * @return 對帳結算回應
     */
    @PostMapping(value = "/reconcile", produces = "application/json")
    @Operation(
            summary = "手動發起單一專案掃描狀態對帳",
            description = "立即比對指定專案在 SonarQube 與 Facade 資料庫間之未修復弱點差異，補償修復狀態、補錄遺漏弱點並同步嚴重等級。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "對帳作業執行完成",
                            content = @Content(schema = @Schema(implementation = ScanReconciledResource.class))),
                    @ApiResponse(responseCode = "400", description = "請求參數錯誤"),
                    @ApiResponse(responseCode = "409", description = "該專案對帳作業正在執行中或並發衝突")
            }
    )
    public ResponseEntity<ScanReconciledResource> reconcileProject(
            @Valid @RequestBody ReconcileProjectScanResource resource) {
        ReconcileProjectScanCommand command = assembler.toCommand(resource);
        ScanReconciliationGottenResult result = reconcileProjectScanUseCase.execute(command);
        return ResponseEntity.ok(assembler.toResource(result));
    }

    /**
     * 手動觸發所有納管專案批次低頻對帳。
     *
     * @param forceSync 是否強制重新同步
     * @return 批次對帳結果清單
     */
    @PostMapping(value = "/reconcile-all", produces = "application/json")
    @Operation(
            summary = "手動發起全專案批次掃描狀態對帳",
            description = "批次比對所有納管專案之弱點狀態，具備平滑流量控制與全域排程鎖保護。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "批次對帳作業執行完成",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ScanReconciledResource.class)))),
                    @ApiResponse(responseCode = "409", description = "全域對帳作業正在執行中")
            }
    )
    public ResponseEntity<List<ScanReconciledResource>> reconcileAllProjects(
            @RequestParam(value = "forceSync", required = false, defaultValue = "false") boolean forceSync) {
        ReconcileAllProjectsCommand command = new ReconcileAllProjectsCommand(forceSync);
        List<ScanReconciliationGottenResult> results = reconcileAllProjectsUseCase.execute(command);
        return ResponseEntity.ok(assembler.toResourceList(results));
    }
}
