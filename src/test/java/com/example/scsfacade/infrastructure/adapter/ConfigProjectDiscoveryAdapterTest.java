package com.example.scsfacade.infrastructure.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 組態納管專案探索適配器單元測試 (ConfigProjectDiscoveryAdapterTest)。
 */
class ConfigProjectDiscoveryAdapterTest {

    /**
     * Feature: 組態專案探索解析
     * Scenario: 輸入含空白與逗點之專案設定字串
     * Given: 字串 "scs-facade, payment-service , auth-service, payment-service"
     * When: 建立 ConfigProjectDiscoveryAdapter 並呼叫 discoverConfiguredProjectIds
     * Then: 正確解析、修整空白並去重
     */
    @Test
    @DisplayName("專案解析 - 逗號分隔字串應正確去空白並去重")
    void shouldCorrectlyParseAndDeduplicateProjects() {
        // Given
        String input = "scs-facade, payment-service , auth-service, payment-service";

        // When
        ConfigProjectDiscoveryAdapter adapter = new ConfigProjectDiscoveryAdapter(input);
        List<String> projectIds = adapter.discoverConfiguredProjectIds();

        // Then
        assertThat(projectIds).containsExactly("scs-facade", "payment-service", "auth-service");
    }

    /**
     * Feature: 組態專案探索空值防護
     * Scenario: 傳入 null 或空字串
     * Given: 傳入 null
     * When: 建立 ConfigProjectDiscoveryAdapter
     * Then: 回傳空清單而不拋出例外
     */
    @Test
    @DisplayName("空值防護 - 傳入 null 或空白應回傳空清單")
    void shouldReturnEmptyListForNullOrBlank() {
        // When & Then
        ConfigProjectDiscoveryAdapter nullAdapter = new ConfigProjectDiscoveryAdapter(null);
        assertThat(nullAdapter.discoverConfiguredProjectIds()).isEmpty();

        ConfigProjectDiscoveryAdapter blankAdapter = new ConfigProjectDiscoveryAdapter("   ");
        assertThat(blankAdapter.discoverConfiguredProjectIds()).isEmpty();
    }
}
