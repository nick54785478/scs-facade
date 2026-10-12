package com.example.scsfacade.application.port.in;

import com.example.scsfacade.application.dto.FindingGottenResult;
import com.example.scsfacade.application.query.GetFindingQuery;
import com.example.scsfacade.application.query.ListFindingQuery;
import com.example.scsfacade.application.view.FindingGottenView;

import java.util.List;

/**
 * 查詢弱點使用案例 (GetFindingUseCase)。
 * <p>
 * 遵循 ... + UseCase 命名規範。Inbound Port 查詢合約。
 * </p>
 */
public interface GetFindingUseCase {

    /**
     * 依據 ID 取得單一弱點詳細結果。
     *
     * @param query 單一弱點查詢物件
     * @return 弱點詳細 DTO
     */
    FindingGottenResult getById(GetFindingQuery query);

    /**
     * 依據專案代碼列出弱點投影視圖。
     *
     * @param query 列表查詢物件
     * @return 弱點摘要視圖清單
     */
    List<FindingGottenView> listByProject(ListFindingQuery query);
}
