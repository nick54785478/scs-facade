package com.example.scsfacade.domain;

import com.example.scsfacade.domain.finding.aggregate.vo.FindingCategory;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingFingerprint;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingId;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingLifecycle;
import com.example.scsfacade.domain.finding.aggregate.vo.FindingLocation;
import com.example.scsfacade.domain.finding.aggregate.vo.RuleInfo;
import com.example.scsfacade.domain.finding.aggregate.vo.ScannerToolInfo;
import com.example.scsfacade.domain.qualitygate.aggregate.vo.QualityGatePolicyId;
import com.example.scsfacade.domain.qualitygate.aggregate.vo.QualityGateThreshold;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionId;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanExecutionPeriod;
import com.example.scsfacade.domain.scanexecution.aggregate.vo.ScanSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 領域實質物件完整測試 (ValueObjectsTest)。
 * <p>
 * 針對各聚合根下的 Value Objects 測試其不可變性、工廠方法、生命週期轉換、以及 equals/hashCode 行為。
 * </p>
 */
class ValueObjectsTest {

    @Test
    @DisplayName("ScanSummary - 驗證邊界值保護、getters 與 equals/hashCode")
    void shouldVerifyScanSummary() {
        ScanSummary s1 = new ScanSummary(-5, -2, -1, -3, -4, -6, true);
        assertThat(s1.getTotalActiveCount()).isZero();
        assertThat(s1.getNewCount()).isZero();
        assertThat(s1.getExistingCount()).isZero();
        assertThat(s1.getResolvedCount()).isZero();
        assertThat(s1.getCriticalCount()).isZero();
        assertThat(s1.getHighCount()).isZero();
        assertThat(s1.isPassedQualityGate()).isTrue();

        ScanSummary empty = ScanSummary.empty();
        assertThat(empty).isEqualTo(s1);
        assertThat(empty.hashCode()).isEqualTo(s1.hashCode());

        ScanSummary s2 = new ScanSummary(10, 5, 5, 2, 1, 2, false);
        assertThat(s2).isNotEqualTo(s1);
        assertThat(s2.equals("other")).isFalse();
        assertThat(s2.equals(s2)).isTrue();
    }

    @Test
    @DisplayName("ScanExecutionPeriod - 驗證開始、完成、失敗狀態轉變與耗時計算")
    void shouldVerifyScanExecutionPeriod() {
        Instant start = Instant.now().minusSeconds(10);
        ScanExecutionPeriod period = ScanExecutionPeriod.start(start);
        assertThat(period.getStartedAt()).isEqualTo(start);
        assertThat(period.getCompletedAt()).isNull();
        assertThat(period.getErrorMessage()).isNull();
        assertThat(period.getDuration()).isNotNull();

        Instant completedAt = Instant.now();
        ScanExecutionPeriod completed = period.complete(completedAt);
        assertThat(completed.getCompletedAt()).isEqualTo(completedAt);
        assertThat(completed.getErrorMessage()).isNull();

        ScanExecutionPeriod failed = period.fail("掃描中斷", completedAt);
        assertThat(failed.getErrorMessage()).isEqualTo("掃描中斷");
        assertThat(failed.getCompletedAt()).isEqualTo(completedAt);

        ScanExecutionPeriod ofPeriod = ScanExecutionPeriod.of(start, completedAt, "err");
        assertThat(ofPeriod.getErrorMessage()).isEqualTo("err");
        assertThat(ofPeriod).isEqualTo(new ScanExecutionPeriod(start, completedAt, "err"));
        assertThat(ofPeriod.hashCode()).isEqualTo(new ScanExecutionPeriod(start, completedAt, "err").hashCode());
        assertThat(ofPeriod.equals(null)).isFalse();
    }

    @Test
    @DisplayName("FindingLifecycle - 驗證弱點生命週期狀態與 Commit 對齊")
    void shouldVerifyFindingLifecycle() {
        Instant now = Instant.now();
        FindingLifecycle lifecycle = FindingLifecycle.init("commit-1", now);
        assertThat(lifecycle.getFirstSeenCommit()).isEqualTo("commit-1");
        assertThat(lifecycle.getLastSeenCommit()).isEqualTo("commit-1");
        assertThat(lifecycle.getResolvedInCommit()).isNull();

        FindingLifecycle touched = lifecycle.touch("commit-2", now.plusSeconds(5));
        assertThat(touched.getFirstSeenCommit()).isEqualTo("commit-1");
        assertThat(touched.getLastSeenCommit()).isEqualTo("commit-2");

        FindingLifecycle resolved = touched.resolve("commit-3", now.plusSeconds(10));
        assertThat(resolved.getResolvedInCommit()).isEqualTo("commit-3");
        assertThat(resolved.getResolvedAt()).isNotNull();

        FindingLifecycle reconstructed = FindingLifecycle.of("commit-1", "commit-2", "commit-3", now, now, now);
        assertThat(reconstructed).isNotNull();
        assertThat(reconstructed).isEqualTo(FindingLifecycle.of("commit-1", "commit-2", "commit-3", now, now, now));
    }

    @Test
    @DisplayName("FindingLocation, RuleInfo, ScannerToolInfo - 屬性與 equals 驗證")
    void shouldVerifyOtherFindingValueObjects() {
        FindingLocation loc1 = new FindingLocation("src/App.java", 10, 20, "public void foo()");
        FindingLocation loc2 = new FindingLocation("src/App.java", 10, 20, "public void foo()");
        assertThat(loc1.getFilePath()).isEqualTo("src/App.java");
        assertThat(loc1.getStartLine()).isEqualTo(10);
        assertThat(loc1.getEndLine()).isEqualTo(20);
        assertThat(loc1.getSnippet()).isEqualTo("public void foo()");
        assertThat(loc1).isEqualTo(loc2);
        assertThat(loc1.hashCode()).isEqualTo(loc2.hashCode());

        RuleInfo rule1 = new RuleInfo("java:S100", "Naming convention", List.of("CWE-1"), "CVE-2026-001", List.of("bad-practice"));
        RuleInfo rule2 = new RuleInfo("java:S100", "Naming convention", List.of("CWE-1"), "CVE-2026-001", List.of("bad-practice"));
        assertThat(rule1.getRuleId()).isEqualTo("java:S100");
        assertThat(rule1.getName()).isEqualTo("Naming convention");
        assertThat(rule1.getCve()).isEqualTo("CVE-2026-001");
        assertThat(rule1).isEqualTo(rule2);

        ScannerToolInfo tool1 = new ScannerToolInfo("SonarQube", "10.0", "SAST");
        ScannerToolInfo tool2 = new ScannerToolInfo("SonarQube", "10.0", "SAST");
        assertThat(tool1.getName()).isEqualTo("SonarQube");
        assertThat(tool1.getVersion()).isEqualTo("10.0");
        assertThat(tool1.getCategory()).isEqualTo("SAST");
        assertThat(tool1).isEqualTo(tool2);
    }

    @Test
    @DisplayName("QualityGateThreshold, PolicyId, FindingId, ScanExecutionId - 識別碼與門檻驗證")
    void shouldVerifyIdentifiersAndPolicyObjects() {
        QualityGateThreshold threshold = new QualityGateThreshold(0, 2, true);
        assertThat(threshold.getMaxCritical()).isZero();
        assertThat(threshold.getMaxHigh()).isEqualTo(2);
        assertThat(threshold.isBlockOnSecrets()).isTrue();
        assertThat(threshold).isEqualTo(new QualityGateThreshold(0, 2, true));

        QualityGatePolicyId pid1 = QualityGatePolicyId.of("policy-1");
        QualityGatePolicyId pid2 = QualityGatePolicyId.generate();
        assertThat(pid1.getValue()).isEqualTo("policy-1");
        assertThat(pid1.toString()).isEqualTo("policy-1");
        assertThat(pid1).isEqualTo(QualityGatePolicyId.of("policy-1"));
        assertThat(pid1).isNotEqualTo(pid2);

        FindingId fid1 = FindingId.of("f-1");
        FindingId fid2 = FindingId.generate();
        assertThat(fid1.getValue()).isEqualTo("f-1");
        assertThat(fid1.toString()).isEqualTo("f-1");
        assertThat(fid1).isEqualTo(FindingId.of("f-1"));
        assertThat(fid1).isNotEqualTo(fid2);

        ScanExecutionId eid1 = ScanExecutionId.of("e-1");
        ScanExecutionId eid2 = ScanExecutionId.generate();
        assertThat(eid1.getValue()).isEqualTo("e-1");
        assertThat(eid1.toString()).isEqualTo("e-1");
        assertThat(eid1).isEqualTo(ScanExecutionId.of("e-1"));
        assertThat(eid1).isNotEqualTo(eid2);

        FindingFingerprint fp1 = FindingFingerprint.of("fp-abc");
        FindingFingerprint fp2 = FindingFingerprint.compute("proj", "main", "file", "rule", "code");
        assertThat(fp1.getValue()).isEqualTo("fp-abc");
        assertThat(fp1.toString()).isEqualTo("fp-abc");
        assertThat(fp1).isEqualTo(FindingFingerprint.of("fp-abc"));
        assertThat(fp1).isNotEqualTo(fp2);
    }
}
