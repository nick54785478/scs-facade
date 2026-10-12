package com.example.scsfacade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SonarQube 外部伺服器連線配置屬性類別。
 * <p>
 * 依據規範，配置類集中存放於根目錄之 config/ 套件中。
 * </p>
 */
@Configuration
@ConfigurationProperties(prefix = "sonarqube")
public class SonarQubeProperties {

    private String baseUrl = "http://localhost:9000";
    private String token = "default-dummy-token";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
