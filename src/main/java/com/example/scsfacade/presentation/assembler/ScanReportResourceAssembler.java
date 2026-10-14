package com.example.scsfacade.presentation.assembler;

import com.example.scsfacade.application.command.UploadScanReportCommand;
import com.example.scsfacade.application.dto.ScanReportUploadedResult;
import com.example.scsfacade.presentation.resource.in.UploadScanReportResource;
import com.example.scsfacade.presentation.resource.out.ScanReportUploadedResource;
import org.springframework.stereotype.Component;

/**
 * 掃描報告表現層轉換組裝器 (ScanReportResourceAssembler)。
 * <p>
 * 遵循六角形架構防腐層 (ACL) 原則，獨立負責 Presentation Resource 與 Application Command/Result 之雙向對齊轉換。
 * </p>
 */
@Component
public class ScanReportResourceAssembler {

    /**
     * 將表現層上傳中繼資料與檔案二進位位元組組裝為應用層命令。
     *
     * @param resource      上傳中繼資料載體
     * @param reportContent 報告檔案二進位內容
     * @param filename      檔案名稱
     * @return 應用層上傳命令
     */
    public UploadScanReportCommand toCommand(UploadScanReportResource resource, byte[] reportContent, String filename) {
        if (resource == null) {
            throw new IllegalArgumentException("UploadScanReportResource 不可為空");
        }
        return new UploadScanReportCommand(
                resource.projectId(),
                resource.branch(),
                resource.commitHash(),
                resource.toolName(),
                reportContent,
                filename,
                resource.externalTaskId()
        );
    }

    /**
     * 將應用層結算結果轉換為表現層回應載體。
     *
     * @param result 應用層結算結果 DTO
     * @return 表現層回應載體
     */
    public ScanReportUploadedResource toResource(ScanReportUploadedResult result) {
        if (result == null) {
            return null;
        }
        return new ScanReportUploadedResource(
                result.scanExecutionId(),
                result.projectId(),
                result.branch(),
                result.commitHash(),
                result.toolName(),
                result.status(),
                result.passedQualityGate(),
                result.totalActiveCount(),
                result.newCount(),
                result.existingCount(),
                result.resolvedCount(),
                result.criticalCount(),
                result.highCount(),
                result.completedAt()
        );
    }
}
