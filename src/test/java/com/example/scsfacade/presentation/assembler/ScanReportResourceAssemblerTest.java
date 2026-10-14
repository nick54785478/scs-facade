package com.example.scsfacade.presentation.assembler;

import com.example.scsfacade.application.command.UploadScanReportCommand;
import com.example.scsfacade.application.dto.ScanReportUploadedResult;
import com.example.scsfacade.presentation.resource.in.UploadScanReportResource;
import com.example.scsfacade.presentation.resource.out.ScanReportUploadedResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 掃描報告轉換器單元測試 (ScanReportResourceAssemblerTest)。
 */
class ScanReportResourceAssemblerTest {

    private final ScanReportResourceAssembler assembler = new ScanReportResourceAssembler();

    /**
     * Feature: Resource 轉換為 Command
     * Scenario: 傳入合法之 UploadScanReportResource 與檔案位元組
     * Given: UploadScanReportResource 物件
     * When: 呼叫 toCommand()
     * Then: 正確組裝為 UploadScanReportCommand
     */
    @Test
    @DisplayName("組裝命令 - 應正確將表現層 Resource 轉為應用層 Command")
    void shouldAssembleCommandFromResource() {
        // Given
        UploadScanReportResource resource = new UploadScanReportResource(
                "proj-1", "feature/auth", "commit-aaa", "Trivy", "task-100"
        );
        byte[] content = "test content".getBytes(StandardCharsets.UTF_8);

        // When
        UploadScanReportCommand command = assembler.toCommand(resource, content, "report.sarif");

        // Then
        assertThat(command.projectId()).isEqualTo("proj-1");
        assertThat(command.branch()).isEqualTo("feature/auth");
        assertThat(command.commitHash()).isEqualTo("commit-aaa");
        assertThat(command.toolName()).isEqualTo("Trivy");
        assertThat(command.reportContent()).isEqualTo(content);
        assertThat(command.filename()).isEqualTo("report.sarif");
        assertThat(command.externalTaskId()).isEqualTo("task-100");

        // Null check
        assertThatThrownBy(() -> assembler.toCommand(null, content, "report.sarif"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Feature: Result 轉換為 Resource
     * Scenario: 傳入應用層結算結果 DTO
     * Given: ScanReportUploadedResult 物件
     * When: 呼叫 toResource()
     * Then: 正確轉為 ScanReportUploadedResource 回應載體
     */
    @Test
    @DisplayName("組裝回應 - 應正確將應用層 Result 轉為表現層 Resource")
    void shouldAssembleResourceFromResult() {
        // Given
        Instant now = Instant.now();
        ScanReportUploadedResult result = new ScanReportUploadedResult(
                "exec-1", "proj-1", "main", "commit-bbb", "Trivy", "SUCCESS",
                true, 10, 2, 8, 1, 0, 1, now
        );

        // When
        ScanReportUploadedResource resource = assembler.toResource(result);

        // Then
        assertThat(resource).isNotNull();
        assertThat(resource.scanExecutionId()).isEqualTo("exec-1");
        assertThat(resource.projectId()).isEqualTo("proj-1");
        assertThat(resource.toolName()).isEqualTo("Trivy");
        assertThat(resource.passedQualityGate()).isTrue();
        assertThat(resource.totalActiveCount()).isEqualTo(10);
        assertThat(resource.newCount()).isEqualTo(2);
        assertThat(resource.resolvedCount()).isEqualTo(1);
        assertThat(resource.completedAt()).isEqualTo(now);

        // Null check
        assertThat(assembler.toResource(null)).isNull();
    }
}
