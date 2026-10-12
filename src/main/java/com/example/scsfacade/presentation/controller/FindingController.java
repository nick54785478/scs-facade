package com.example.scsfacade.presentation.controller;

import com.example.scsfacade.application.dto.FindingGottenResult;
import com.example.scsfacade.application.port.in.GetFindingUseCase;
import com.example.scsfacade.application.query.GetFindingQuery;
import com.example.scsfacade.application.query.ListFindingQuery;
import com.example.scsfacade.application.view.FindingGottenView;
import com.example.scsfacade.presentation.assembler.FindingResourceAssembler;
import com.example.scsfacade.presentation.resource.out.FindingGottenResource;
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
 * 弱點查詢控制器 (FindingController)。
 * <p>
 * 表現層 Inbound Adapter。提供依 ID 取得單一弱點與依專案查詢列表端點。
 * </p>
 */
@Tag(name = "Finding Query API", description = "標準化弱點查詢與檢視端點")
@RestController
@RequestMapping("/api/v1/findings")
public class FindingController {

    private final GetFindingUseCase getFindingUseCase;
    private final FindingResourceAssembler findingResourceAssembler;

    public FindingController(GetFindingUseCase getFindingUseCase,
                             FindingResourceAssembler findingResourceAssembler) {
        this.getFindingUseCase = getFindingUseCase;
        this.findingResourceAssembler = findingResourceAssembler;
    }

    /**
     * 依據弱點 ID 取得詳細資訊。
     *
     * @param id 弱點 UUID
     * @return 弱點詳細回應
     */
    @Operation(summary = "依據弱點 ID 查詢完整資訊")
    @GetMapping("/{id}")
    public ResponseEntity<FindingGottenResource> getById(@PathVariable("id") String id) {
        GetFindingQuery query = new GetFindingQuery(id);
        FindingGottenResult result = getFindingUseCase.getById(query);
        FindingGottenResource response = findingResourceAssembler.toResource(result);
        return ResponseEntity.ok(response);
    }

    /**
     * 依據專案查詢弱點清單投影視圖。
     *
     * @param projectId 專案代碼
     * @param status    狀態過濾條件 (可選，如 OPEN)
     * @return 弱點投影清單
     */
    @Operation(summary = "依據專案查詢弱點清單 (Projection View)")
    @GetMapping
    public ResponseEntity<List<FindingGottenView>> listByProject(
            @RequestParam("projectId") String projectId,
            @RequestParam(value = "status", required = false) String status) {
        ListFindingQuery query = new ListFindingQuery(projectId, status);
        List<FindingGottenView> views = getFindingUseCase.listByProject(query);
        return ResponseEntity.ok(views);
    }
}
