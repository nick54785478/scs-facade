package com.example.scsfacade.presentation.controller;

import com.example.scsfacade.application.command.EvaluateQualityGateCommand;
import com.example.scsfacade.application.dto.QualityGateEvaluatedResult;
import com.example.scsfacade.application.port.in.EvaluateQualityGateUseCase;
import com.example.scsfacade.presentation.assembler.QualityGateResourceAssembler;
import com.example.scsfacade.presentation.resource.in.EvaluateQualityGateResource;
import com.example.scsfacade.presentation.resource.out.QualityGateEvaluatedResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 品質門禁控制器 (QualityGateController)。
 * <p>
 * 表現層 Inbound Adapter。提供單一標準化端點供 CI/CD Pipeline 查詢門禁審查結果。
 * </p>
 */
@Tag(name = "Quality Gate API", description = "統一品質門禁評估端點")
@RestController
@RequestMapping("/api/v1/quality-gate")
public class QualityGateController {

    private final EvaluateQualityGateUseCase evaluateQualityGateUseCase;
    private final QualityGateResourceAssembler qualityGateResourceAssembler;

    public QualityGateController(EvaluateQualityGateUseCase evaluateQualityGateUseCase,
                                QualityGateResourceAssembler qualityGateResourceAssembler) {
        this.evaluateQualityGateUseCase = evaluateQualityGateUseCase;
        this.qualityGateResourceAssembler = qualityGateResourceAssembler;
    }

    /**
     * 評估專案門禁是否合格。
     *
     * @param resource 門禁評估請求
     * @return 門禁評估回應
     */
    @Operation(summary = "評估專案是否通過品質門禁 (CI/CD Quality Gate Check)")
    @PostMapping("/evaluate")
    public ResponseEntity<QualityGateEvaluatedResource> evaluate(
            @Valid @RequestBody EvaluateQualityGateResource resource) {
        EvaluateQualityGateCommand command = qualityGateResourceAssembler.toCommand(resource);
        QualityGateEvaluatedResult result = evaluateQualityGateUseCase.execute(command);
        QualityGateEvaluatedResource response = qualityGateResourceAssembler.toResource(result);
        return ResponseEntity.ok(response);
    }
}
