package com.example.scsfacade.presentation.controller;

import com.example.scsfacade.application.command.UploadScanReportCommand;
import com.example.scsfacade.application.dto.ScanReportUploadedResult;
import com.example.scsfacade.application.port.in.UploadScanReportUseCase;
import com.example.scsfacade.presentation.assembler.ScanReportResourceAssembler;
import com.example.scsfacade.presentation.resource.in.UploadScanReportResource;
import com.example.scsfacade.presentation.resource.out.ScanReportUploadedResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 掃描報告主動上傳表現層控制器 (ScanReportUploadController)。
 * <p>
 * 提供 REST API 供 CI/CD Pipeline、CLI 腳本或第三方整合工具主動推送 SARIF 2.1.0 格式之安全報告。
 * </p>
 */
@RestController
@RequestMapping("/api/v1/scans")
@Tag(name = "Scan Ingestion", description = "源碼與弱點掃描報告匯入端點")
public class ScanReportUploadController {

    private final UploadScanReportUseCase uploadScanReportUseCase;
    private final ScanReportResourceAssembler scanReportResourceAssembler;

    public ScanReportUploadController(UploadScanReportUseCase uploadScanReportUseCase,
                                      ScanReportResourceAssembler scanReportResourceAssembler) {
        this.uploadScanReportUseCase = uploadScanReportUseCase;
        this.scanReportResourceAssembler = scanReportResourceAssembler;
    }

    /**
     * 上傳 SARIF/JSON 掃描報告並完成結算。
     *
     * @param file           報告檔案
     * @param projectId      專案識別碼
     * @param branch         分支名稱
     * @param commitHash     Commit SHA
     * @param toolName       指定工具名稱
     * @param externalTaskId 外部任務識別碼
     * @return 掃描報告結算回應
     * @throws IOException 檔案讀取異常
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "上傳掃描報告 (SARIF / JSON)",
            description = "接收 CI/CD 主動推送之 SARIF 2.1.0 掃描報告（如 Trivy, Gitleaks, Semgrep 等），解析弱點、對齊生命週期並結算品質門禁。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "報告解析與結算成功",
                            content = @Content(schema = @Schema(implementation = ScanReportUploadedResource.class))),
                    @ApiResponse(responseCode = "400", description = "請求參數錯誤或檔案內容無效"),
                    @ApiResponse(responseCode = "409", description = "相同任務併發衝突或正在處理中")
            }
    )
    public ResponseEntity<ScanReportUploadedResource> uploadScanReport(
            @Parameter(description = "掃描報告檔案 (支援 SARIF 2.1.0 或 JSON 格式)", required = true)
            @RequestPart("file") MultipartFile file,

            @Parameter(description = "專案代碼 (Project Key)", example = "payment-service", required = true)
            @RequestParam("projectId") String projectId,

            @Parameter(description = "掃描分支名稱 (預設 main)", example = "main")
            @RequestParam(value = "branch", required = false, defaultValue = "main") String branch,

            @Parameter(description = "對應之 Git Commit 雜湊碼", example = "a1b2c3d4e5f67890123456789abcdef012345678")
            @RequestParam(value = "commitHash", required = false, defaultValue = "") String commitHash,

            @Parameter(description = "指定工具名稱 (若未指定則自動從報告 driver 提取)", example = "Trivy")
            @RequestParam(value = "toolName", required = false) String toolName,

            @Parameter(description = "外部任務或 Pipeline 識別碼 (用於等冪防重放)", example = "gitlab-ci-pipeline-1234")
            @RequestParam(value = "externalTaskId", required = false) String externalTaskId
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        UploadScanReportResource resource = new UploadScanReportResource(projectId, branch, commitHash, toolName, externalTaskId);
        UploadScanReportCommand command = scanReportResourceAssembler.toCommand(resource, file.getBytes(), file.getOriginalFilename());
        ScanReportUploadedResult result = uploadScanReportUseCase.execute(command);
        return ResponseEntity.ok(scanReportResourceAssembler.toResource(result));
    }
}
