package com.example.scsfacade.application.port.out;

import com.example.scsfacade.domain.finding.aggregate.root.Finding;

import java.util.List;

/**
 * SonarQube 掃描器外向埠 (SonarQubeScannerPort)。
 * <p>
 * 遵循 ... + Port 命名規範。應用層要求外部 SonarQube 基礎設施提供之合約。
 * 回傳直接標準化後之 Finding 領域聚合根清單。
 * </p>
 */
public interface SonarQubeScannerPort {

    /**
     * 呼叫 SonarQube 伺服器並拉取專案之 Issues，並初步封裝為 Finding 領域物件清單。
     *
     * @param projectId  專案識別碼
     * @param branch     分析分支
     * @param commitHash 提交雜湊
     * @return 領域弱點物件清單
     */
    List<Finding> fetchProjectIssues(String projectId, String branch, String commitHash);
}
