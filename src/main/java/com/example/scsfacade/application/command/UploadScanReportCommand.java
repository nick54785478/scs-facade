package com.example.scsfacade.application.command;

import java.util.Objects;

/**
 * 上傳掃描報告命令 (UploadScanReportCommand)。
 * <p>
 * 遵循 V + N + Command 命名規範。攜帶 CI/CD 執行後主動推送之掃描報告二進位資料或字串與版本識別參數。
 * </p>
 */
public record UploadScanReportCommand(
        String projectId,
        String branch,
        String commitHash,
        String toolName,
        byte[] reportContent,
        String filename,
        String externalTaskId
) {

    public UploadScanReportCommand {
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("專案代碼 (projectId) 不可為空");
        }
        if (reportContent == null || reportContent.length == 0) {
            throw new IllegalArgumentException("掃描報告內容 (reportContent) 不可為空");
        }
        if (branch == null || branch.trim().isEmpty()) {
            branch = "main";
        }
        if (commitHash == null) {
            commitHash = "";
        }
        if (toolName != null && toolName.trim().isEmpty()) {
            toolName = null;
        }
        if (filename == null || filename.trim().isEmpty()) {
            filename = "report.sarif";
        }
    }

    public UploadScanReportCommand(String projectId,
                                   String branch,
                                   String commitHash,
                                   String toolName,
                                   byte[] reportContent,
                                   String filename) {
        this(projectId, branch, commitHash, toolName, reportContent, filename, null);
    }
}
