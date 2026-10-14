package com.example.scsfacade.application.port.out;

import com.example.scsfacade.domain.finding.aggregate.vo.FindingCategory;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingLocation;
import com.example.scsfacade.domain.finding.aggregate.vo.RuleInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.Severity;

import java.util.List;

/**
 * 掃描報告解析出站埠 (ScanReportParserPort)。
 * <p>
 * 遵循 ... + Port 命名規範。定義由基礎設施層解析異質格式報告（如 SARIF、JSON 等）之合約。
 * 嚴禁傳入或回傳任何框架或技術相依物件。
 * </p>
 */
public interface ScanReportParserPort {

    /**
     * 解析掃描報告內容為標準化之掃描工具與弱點項目清單。
     *
     * @param reportContent   報告二進位位元組內容
     * @param defaultToolName 呼叫端指定之預設工具名稱 (若報告內部未宣告則採用)
     * @return 解析完成之掃描報告資料
     */
    ParsedScanReport parse(byte[] reportContent, String defaultToolName);

    /**
     * 判定本解析器是否支援該檔案格式或內容。
     *
     * @param filename      報告檔案名稱
     * @param reportContent 報告二進位位元組內容
     * @return 若支援則回傳 true
     */
    boolean supports(String filename, byte[] reportContent);

    /**
     * 解析後之標準化掃描報告資料載體 (ParsedScanReport)。
     */
    record ParsedScanReport(
            ScannerToolInfo toolInfo,
            List<ParsedFindingItem> items
    ) {
    }

    /**
     * 解析後之單筆弱點資料載體 (ParsedFindingItem)。
     */
    record ParsedFindingItem(
            RuleInfo ruleInfo,
            FindingCategory category,
            Severity severity,
            FindingLocation location,
            String rawDetails
    ) {
    }
}
