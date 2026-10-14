package com.example.scsfacade.application.assembler;

import com.example.scsfacade.application.dto.FindingGottenResult;
import com.example.scsfacade.application.view.FindingGottenView;
import com.example.scsfacade.domain.finding.aggregate.root.Finding;
import org.springframework.stereotype.Component;

/**
 * 弱點應用層組裝器 (FindingDtoAssembler)。
 * <p>
 * 專門負責領域模型 (Finding) 與 DTO (FindingGottenResult) / View (FindingGottenView) 之間的雙向轉換。
 * 遵守單一職責原則，維持 DTO 與 View 作為純資料載體。
 * </p>
 */
@Component
public class FindingDtoAssembler {

    /**
     * 將領域模型 Finding 轉換為 FindingGottenResult DTO。
     *
     * @param finding 領域聚合根
     * @return 轉換後的 FindingGottenResult
     */
    public FindingGottenResult toResult(Finding finding) {
        if (finding == null) {
            return null;
        }
        return new FindingGottenResult(
                finding.getId().getValue(),
                finding.getProjectId(),
                finding.getBranch(),
                finding.getCommitHash(),
                finding.getFirstSeenCommit(),
                finding.getLastSeenCommit(),
                finding.getResolvedInCommit(),
                finding.getFingerprint().getValue(),
                finding.getToolInfo().getName(),
                finding.getToolInfo().getVersion(),
                finding.getToolInfo().getCategory(),
                finding.getCategory() != null ? finding.getCategory().name() : "CODE_SMELL",
                finding.getRuleInfo().getRuleId(),
                finding.getRuleInfo().getName(),
                finding.getRuleInfo().getCweList(),
                finding.getRuleInfo().getCve(),
                finding.getRuleInfo().getTags(),
                finding.getSeverity().name(),
                finding.getLocation().getFilePath(),
                finding.getLocation().getStartLine(),
                finding.getLocation().getEndLine(),
                finding.getLocation().getSnippet(),
                finding.getStatus().name(),
                finding.getFirstSeenAt(),
                finding.getLastSeenAt(),
                finding.getResolvedAt(),
                finding.getRawDetails()
        );
    }

    /**
     * 將領域模型 Finding 轉換為 FindingGottenView 投影視圖。
     *
     * @param finding 領域聚合根
     * @return 轉換後的 FindingGottenView
     */
    public FindingGottenView toView(Finding finding) {
        if (finding == null) {
            return null;
        }
        return new FindingGottenView(
                finding.getId().getValue(),
                finding.getProjectId(),
                finding.getRuleInfo().getRuleId(),
                finding.getSeverity().name(),
                finding.getLocation().getFilePath(),
                finding.getLocation().getStartLine(),
                finding.getStatus().name()
        );
    }
}
