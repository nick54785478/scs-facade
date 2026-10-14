package com.example.scsfacade.presentation.controller;

import com.example.scsfacade.application.command.IngestSonarQubeCommand;
import com.example.scsfacade.application.dto.ScanExecutionGottenResult;
import com.example.scsfacade.application.port.in.IngestSonarQubeUseCase;
import com.example.scsfacade.presentation.assembler.SonarQubeResourceAssembler;
import com.example.scsfacade.presentation.resource.in.IngestSonarQubeResource;
import com.example.scsfacade.presentation.resource.in.ReceiveSonarQubeWebhookResource;
import com.example.scsfacade.presentation.resource.out.SonarQubeIngestedResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SonarQube 介接與 Webhook 控制器 (SonarQubeWebhookController)。
 * <p>
 * 表現層 Inbound Adapter。接收外來 HTTP 請求（如 CI/CD 或 Webhook 觸發），
 * 委託 Assembler 轉換為 Command 後呼叫 Inbound Port (IngestSonarQubeUseCase)。
 * </p>
 */
@Tag(name = "SonarQube Ingestion API", description = "SonarQube 掃描結果匯入與 Webhook 介接端點")
@RestController
@RequestMapping("/api/v1/scans/sonarqube")
public class SonarQubeWebhookController {

    private final IngestSonarQubeUseCase ingestSonarQubeUseCase;
    private final SonarQubeResourceAssembler sonarQubeResourceAssembler;

    public SonarQubeWebhookController(IngestSonarQubeUseCase ingestSonarQubeUseCase,
                                     SonarQubeResourceAssembler sonarQubeResourceAssembler) {
        this.ingestSonarQubeUseCase = ingestSonarQubeUseCase;
        this.sonarQubeResourceAssembler = sonarQubeResourceAssembler;
    }

    /**
     * 觸發 SonarQube 報告同步或手動觸發匯入。
     *
     * @param resource 請求載體
     * @return 匯入標準弱點與執行歷程結果
     */
    @Operation(summary = "觸發 SonarQube 弱點報告拉取並進行生命週期對齊匯入")
    @PostMapping("/trigger")
    public ResponseEntity<SonarQubeIngestedResource> triggerIngestion(
            @Valid @RequestBody IngestSonarQubeResource resource) {
        IngestSonarQubeCommand command = sonarQubeResourceAssembler.toCommand(resource);
        ScanExecutionGottenResult result = ingestSonarQubeUseCase.execute(command);
        SonarQubeIngestedResource response = sonarQubeResourceAssembler.toResource(result);
        return ResponseEntity.ok(response);
    }

    /**
     * 接收 SonarQube 伺服器原生 Webhook 推播並自動執行標準化匯入。
     *
     * @param resource SonarQube 原生 Webhook 載體
     * @return 匯入標準弱點與執行歷程結果
     */
    @Operation(summary = "接收 SonarQube 原生 Webhook 推播並自動執行生命週期對齊匯入")
    @PostMapping("/webhook")
    public ResponseEntity<SonarQubeIngestedResource> handleWebhook(
            @RequestBody ReceiveSonarQubeWebhookResource resource) {
        IngestSonarQubeCommand command = sonarQubeResourceAssembler.toCommand(resource);
        ScanExecutionGottenResult result = ingestSonarQubeUseCase.execute(command);
        SonarQubeIngestedResource response = sonarQubeResourceAssembler.toResource(result);
        return ResponseEntity.ok(response);
    }
}
