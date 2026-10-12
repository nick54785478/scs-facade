package com.example.scsfacade.infrastructure.client.sonarqube;

import com.example.scsfacade.config.SonarQubeProperties;
import com.example.scsfacade.infrastructure.client.sonarqube.dto.SonarQubeIssueDto;
import com.example.scsfacade.infrastructure.client.sonarqube.dto.SonarQubeIssuesSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

/**
 * SonarQube REST API 客戶端 (SonarQubeRestClient)。
 * <p>
 * 使用 Spring 6 原生 RestClient 與 SonarQube Web API 通訊。
 * 負責發送 HTTP GET /api/issues/search 請求並解析回傳 JSON。
 * </p>
 */
@Component
public class SonarQubeRestClient {

    private static final Logger log = LoggerFactory.getLogger(SonarQubeRestClient.class);

    private final RestClient restClient;
    private final SonarQubeProperties properties;

    public SonarQubeRestClient(SonarQubeProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 呼叫 SonarQube /api/issues/search 取得指定專案之未解決 Issues。
     *
     * @param projectKey 專案代碼 (Component Key)
     * @param branch     分支名稱 (可為 null)
     * @return Issue DTO 清單
     */
    public List<SonarQubeIssueDto> searchIssues(String projectKey, String branch) {
        try {
            log.info("正在自 SonarQube 拉取專案 [{}] (branch: [{}]) 的 Issues...", projectKey, branch);

            SonarQubeIssuesSearchResponse response = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/api/issues/search")
                                .queryParam("componentKeys", projectKey)
                                .queryParam("resolved", "false")
                                .queryParam("ps", "500");
                        if (branch != null && !branch.trim().isEmpty()) {
                            uriBuilder.queryParam("branch", branch);
                        }
                        return uriBuilder.build();
                    })
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getToken())
                    .retrieve()
                    .body(SonarQubeIssuesSearchResponse.class);

            if (response != null && response.issues() != null) {
                log.info("成功自 SonarQube 獲取 {} 筆 Issues", response.issues().size());
                return response.issues();
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("呼叫 SonarQube 伺服器失敗 ({}), 回傳空弱點清單以供容錯處置", e.getMessage());
            return Collections.emptyList();
        }
    }
}
