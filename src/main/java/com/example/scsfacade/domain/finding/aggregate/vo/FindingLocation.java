package com.example.scsfacade.domain.finding.aggregate.vo;

import java.util.Objects;

/**
 * 弱點所在位置 (FindingLocation) 實質物件。
 * <p>
 * 封裝檔案路徑、起始行號、結束行號以及程式碼片段等位置資訊。
 * </p>
 */
public final class FindingLocation {

    /**
     * 源碼檔案相對路徑 (統一使用正斜線 /，例如 "src/main/java/App.java")。
     */
    private final String filePath;

    /**
     * 弱點發生起始行號 (1-based，若未提供則為 null)。
     */
    private final Integer startLine;

    /**
     * 弱點發生結束行號 (1-based，若未提供則為 null)。
     */
    private final Integer endLine;

    /**
     * 弱點關聯之程式碼片段或上下文摘要。
     */
    private final String snippet;

    public FindingLocation(String filePath, Integer startLine, Integer endLine, String snippet) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("檔案路徑不可為空");
        }
        this.filePath = filePath.trim();
        this.startLine = startLine;
        this.endLine = endLine;
        this.snippet = snippet != null ? snippet.trim() : "";
    }

    public String getFilePath() {
        return filePath;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public String getSnippet() {
        return snippet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FindingLocation that)){
            return false;
        }
        return Objects.equals(filePath, that.filePath) &&
                Objects.equals(startLine, that.startLine) &&
                Objects.equals(endLine, that.endLine) &&
                Objects.equals(snippet, that.snippet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, startLine, endLine, snippet);
    }
}
