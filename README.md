# SCS Facade (Source Code Scan Facade)

源碼掃描多來源結果整合門面服務 (SCS Facade) 是一個基於 **Domain-Driven Design (DDD)** 與 **六角形架構 (Hexagonal Architecture / Ports & Adapters)** 的現代化 DevSecOps 微服務。

專為整合異質程式碼掃描工具（如 SonarQube、Snyk、Trivy、Gitleaks 等）所設計，具備弱點模型標準化、跨掃描輪次指紋去重 (Fingerprint Deduplication) 以及 CI/CD 統一品質門禁 (Quality Gate) 審查功能。

---

## 核心特色

1. **嚴格遵守 DDD 與六角形架構 (Ports & Adapters)**：
   * **Domain Layer**：純 Java 實作，零框架、零 JPA、零 Lombok 侵入，領域規則高度內聚。
   * **Application Layer**：嚴格隔離技術細節，Inbound Ports (UseCases) 與 Outbound Ports (Ports) 僅處理領域物件與純資料 DTO。
   * **Infrastructure Layer**：包含 Spring Data JPA 持久化適配器與 SonarQube 外部客戶端適配器。
   * **Presentation Layer**：標準 REST API 端點，支援 Webhook 觸發、弱點查詢與品質門禁判定。
2. **多源等級標準化 (Severity Normalization)**：
   * 將異質掃描工具之不同嚴重程度定義（如 Blocker、Critical、Major、Minor 等）統一對齊至 `CRITICAL`、`HIGH`、`MEDIUM`、`LOW`、`INFO` 五級標準。
3. **智慧指紋去重機制 (SHA-256 Fingerprint)**：
   * 根據 `ProjectId + FilePath + RuleId + NormalizedSnippet` 生成 SHA-256 指紋，有效避免行號位移導致的重複通報。
4. **CI/CD 自動化品質門禁 (Quality Gate Evaluator)**：
   * 提供單一門禁審查端點，可自訂 Critical / High 容忍門檻，直接與 GitLab CI / Jenkins / GitHub Actions 整合。

---

## 技術棧

* **語言與運行環境**：Java 21 (Eclipse Temurin)
* **核心框架**：Spring Boot 3.3.4
* **建置工具**：Apache Maven 3.9 (內建 Maven Wrapper)
* **資料庫**：H2 Database (本機開發測試) / PostgreSQL (生產環境)
* **API 規格**：SpringDoc OpenAPI (Swagger UI)
* **容器化與 CI/CD**：Docker Compose (GitLab CE + GitLab Runner + SonarQube)

---

## 快速啟動

### 1. 執行單元測試
```bash
./mvnw test       # Linux / macOS
.\mvnw.cmd test   # Windows
```

### 2. 啟動微服務
```bash
./mvnw spring-boot:run       # Linux / macOS
.\mvnw.cmd spring-boot:run   # Windows
```

啟動後可開啟瀏覽器存取 Swagger UI 互動文件：
* **Swagger UI**：`http://localhost:8080/swagger-ui.html`
* **H2 Console**：`http://localhost:8080/h2-console`

---

## 相關文件

* [系統架構與 DDD 設計指南](doc/source_code_scan_facade_design.md)
* [GitLab CI 通過條件與品質門檻規範](doc/CI%20通過條件與品質門檻規範.md)
* [SonarQube Quality Gate 與測試覆蓋率整合指南](doc/SonarQube%20Quality%20Gate%20與測試覆蓋率整合指南.md)
* [GitLab CI、SonarQube 與 SCS Facade 整合架構](doc/ci_cd_integration_architecture.md)
* [建立 GitLab CI 與 SonarQube 操作指南](doc/建立%20GitLab%20CI.md)
* [GitLab 首次登入操作指南](doc/GitLab%20首次登入操作指南.md)
