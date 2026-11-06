# SCS Facade (Source Code Scan Facade)

> **源碼掃描多來源結果整合門面服務**：基於 **Domain-Driven Design (DDD, 領域驅動設計)** 與 **六角形架構 (Hexagonal Architecture / Ports & Adapters)** 的現代化企業級 DevSecOps 安全中樞微服務。

---

## 📌 專案定位與核心使命

在現代企業的 DevSecOps 縱深防禦實踐中，通常會組合多種專用靜態檢測工具：
* **靜態源碼安全 (SAST)**：SonarQube、Semgrep、CodeQL
* **開源相依性弱點 (SCA)**：Trivy、Snyk、OWASP Dependency-Check
* **機敏資訊洩漏檢測 (Secret Detection)**：Gitleaks、TruffleHog

然而，除了集中託管型工具（如 SonarQube）具備伺服器與 Webhook 之外，**Trivy、Gitleaks、Semgrep 等大多以 CLI 形式運行於 CI/CD Pipeline 的短暫容器中**。容器在構建完成後隨即銷毀，導致各工具的弱點資料嚴重分散、無法長期追蹤歷史趨勢與修復週期，更無法在企業層級落實一致的品質門禁（Quality Gate）。

**SCS Facade** 專為解決上述痛點而生，作為異質掃描工具的統一中樞，具備**弱點標準化模型**、**SHA-256 跨輪次指紋去重**、**雙軌制攝入機制**與 **CI/CD 自動化門禁審查**能力。

---

## 🔄 雙軌制整合架構 (Dual-Track Architecture)

SCS Facade 採用先進的**雙軌制資料攝入架構**，兼顧 CI/CD 的秒級即時反饋與後續狀態變更的最終一致性（Eventual Consistency）：

```
 ┌────────────────────────────────────────────────────────────────────────┐
 │ 【軌道一：即時主力 (Primary - Realtime)】                              │
 │   1. SonarQube Webhook 即時推播 (秒級響應、綁定掃描 Analysis Task)       │
 │   2. SARIF 2.1.0 Push 上傳 (Trivy / Gitleaks / Semgrep / Snyk 等 CLI)  │
 └───────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
                     ┌──────────────────────────────┐
                     │   SCS Facade 核心領域資料庫   │
                     │   (Finding & ScanExecution)  │
                     └───────────────▲──────────────┘
                                     │
 ┌───────────────────────────────────┴────────────────────────────────────┐
 │ 【軌道二：差異對帳補償 (Secondary - Reconciliation)】                  │
 │   1. 每日低頻定時排程 (Spring Scheduler Cron，預設凌晨 02:00)           │
 │   2. 隨選手動觸發 API (On-Demand POST /api/v1/scans/reconcile)           │
 │   3. 修補 Webhook 掉包、網路瞬斷、SonarQube UI 手動標記修復/誤報差異   │
 └────────────────────────────────────────────────────────────────────────┘
```

| 軌道 | 攝入模式 | 支援工具 / 來源 | 觸發時機 | 核心職責 |
| :--- | :--- | :--- | :--- | :--- |
| **軌道一** | **Push 模式 (Webhook)** | SonarQube Server | 掃描分析完成後由服務端推播 | 秒級反應、精準綁定 Analysis ID 與執行狀態 |
| **軌道一** | **Push 模式 (SARIF 上傳)** | Trivy, Gitleaks, Semgrep, Snyk 等 | CI/CD Pipeline 容器結束前 | 上傳 OASIS SARIF 2.1.0 檔案，綁定 CommitHash 與專案 |
| **軌道二** | **Pull 模式 (排程對帳補償)** | SonarQube REST API | 每日低頻排程或管理者手動 API 觸發 | 對齊本地資料庫與遠端最新狀態，修補遺漏並自動狀態流轉 |

---

## 💡 核心亮點與特色

1. **嚴格遵從 DDD 領域驅動與六角形架構 (Ports & Adapters)**：
   * **Domain Layer**：純 Java（Pure Java）打造，零框架、零 JPA、零 Lombok 侵入，領域規則（Aggregates、Value Objects、Domain Events）高度封裝。
   * **Application Layer**：落實 CQRS（Command / Query 分離），UseCase 接收 Command/Query，Port 介面嚴禁傳遞技術類物件，透過獨立 `assembler/` 轉換純資料 DTO。
   * **Infrastructure Layer**：包含 Spring Data JPA 持久化、Redisson 分散式協同、SARIF 串流解析器與 SonarQube REST 客戶端。
   * **Presentation Layer**：嚴格純資料 Resource 載體（`resource/in/`、`resource/out/`），由 `assembler/` 負責轉換，控制器邏輯極致輕薄。
2. **智慧指紋去重 (SHA-256 Fingerprint)**：
   * 採用 `ProjectId + FilePath + RuleId + NormalizedSnippet` 計算 SHA-256 唯一指紋，有效抵抗程式碼增刪造成的行號位移，避免重複通報。
3. **多源等級標準化 (Severity Normalization)**：
   * 將異質掃描工具的不同嚴重度定義（如 Blocker、Critical、Major、Minor 等）統一對齊至 `CRITICAL`、`HIGH`、`MEDIUM`、`LOW`、`INFO` 五級標準。
4. **生命週期隔離與同輪次結算 (Finding Lifecycle Management)**：
   * 支援多工具在同一輪 Commit 掃描，各工具的弱點狀態相互獨立。當新一輪未再檢出該工具的舊弱點時，自動標記為 `RESOLVED` 並記錄解決輪次的 CommitHash。
5. **Redisson 分散式並發與等冪鎖 (Concurrency & Idempotency Lock)**：
   * 針對報告上傳與排程對帳，使用 Redisson 分散式可重入鎖與看門狗自動續期，確保高並發環境下資料不被重入污染。
6. **CI/CD 自動化品質門禁 (Quality Gate Evaluator)**：
   * 提供獨立門禁審查端點，可彈性設定 Critical / High 容忍門檻，直接作為 GitLab CI、Jenkins、GitHub Actions 的通過阻擋指標。

---

## 🏗️ 專案根目錄與分層架構結構

### 1. 專案根目錄全貌 (Project Root Structure)

```
scs-facade/
├── .agents/                                 # 工作區開發架構準則 (AGENTS.md: DDD + 六角形架構規範)
├── doc/                                     # 系統詳細設計文件與 CI/CD 整合指南 (8 份專題規範)
│   ├── source_code_scan_facade_design.md    # 系統架構與 DDD 設計指南
│   ├── Push 模式 (CI_CD 驅動檔案上傳) 功能設計與整合指南.md # Push 模式 SARIF 串流解析與結算
│   ├── Pull 模式 (低頻排程對帳與最終一致性補償) 功能設計與整合指南.md # Pull 模式排程對帳與補償
│   ├── CI 通過條件與品質門檻規範.md           # GitLab CI Quality Gate 規範與指標
│   ├── SonarQube Quality Gate 與測試覆蓋率整合指南.md # SonarQube 閘門與 JaCoCo 整合
│   ├── ci_cd_integration_architecture.md   # GitLab CI、SonarQube 與 SCS Facade 整合架構
│   ├── 建立 GitLab CI.md                    # GitLab CI Runner 與 Pipeline 設定教學
│   └── GitLab 首次登入操作指南.md             # GitLab 初始密碼取得與專案設定
├── docker-compose/                          # 本地 DevSecOps 容器化環境 (一鍵起全套服務)
│   └── docker-compose.yml                   # 包含 GitLab CE + GitLab Runner + SonarQube + PostgreSQL
├── src/
│   ├── main/
│   │   ├── java/com/example/scsfacade/      # 核心後端原始碼 (DDD 與六角形架構實踐)
│   │   └── resources/
│   │       └── application.yml              # 系統環境參數、資料庫、Redis 與排程設定
│   └── test/
│       └── java/com/example/scsfacade/      # 單元測試與整合測試 (123+ 項測試，100% 通過率)
├── .gitignore                               # Git 版本控制忽略清單
├── .gitlab-ci.yml                           # GitLab CI/CD Pipeline 流程定義
├── mvnw / mvnw.cmd                          # Apache Maven 3.9 Wrapper (Linux/macOS 與 Windows)
├── pom.xml                                  # Maven 依賴管理、JaCoCo 與編譯建置配置
└── README.md                                # 專案說明與操作指南
```

### 2. 核心代碼分層結構 (Domain-Driven Design & Hexagonal Architecture)

```
src/main/java/com/example/scsfacade/
├── domain/                                  # 1. 領域層 (Pure Java, 零外部框架與 ORM 依賴)
│   ├── event/                               # 共用領域事件介面 (DomainEvent.java)
│   ├── finding/                             # Finding 聚合 (弱點項目)
│   │   ├── aggregate/                       # 聚合根 (Finding) 與 Value Objects (Fingerprint, Lifecycle, Severity)
│   │   ├── event/                           # 領域事件 (FindingIngestedEvent, FindingStatusChangedEvent)
│   │   └── exception/                       # 領域例外 (FindingNotFoundException)
│   ├── scanexecution/                       # ScanExecution 聚合 (掃描執行週期)
│   │   ├── aggregate/                       # 聚合根 (ScanExecution) 與 Value Objects (Period, Summary, Status)
│   │   ├── event/                           # 領域事件 (ScanExecutionStartedEvent, CompletedEvent)
│   │   └── exception/                       # 領域例外 (ScanExecutionNotFoundException)
│   └── qualitygate/                         # QualityGate 聚合 (門禁策略與閾值)
│       ├── aggregate/                       # 聚合根 (QualityGatePolicy) 與 Value Objects (Threshold, Status)
│       └── event/                           # 領域事件 (QualityGateEvaluatedEvent)
│
├── application/                             # 2. 應用層 (CQRS 調度、業務合約與純資料 DTO)
│   ├── port/
│   │   ├── in/                              # Inbound Ports (UploadScanReportUseCase, IngestSonarQubeUseCase, ...)
│   │   └── out/                             # Outbound Ports (FindingRepositoryPort, DistributedLockPort, ...)
│   ├── command/                             # CQRS Command 物件 (UploadScanReportCommand, ...)
│   ├── query/                               # CQRS Query 物件 (GetFindingQuery, ListFindingQuery, ...)
│   ├── dto/                                 # 純資料傳輸物件 (FindingGottenResult, ScanReportUploadedResult, ...)
│   ├── view/                                # View 純資料投影載體 (FindingGottenView, ...)
│   ├── assembler/                           # 領域物件與 DTO 雙向轉換器 (FindingDtoAssembler, ...)
│   └── service/                             # UseCase 實作 (CommandService / QueryService)
│
├── infrastructure/                          # 3. 基礎設施層 (Outbound Adapters 與技術實作)
│   ├── adapter/                             # Outbound Port 適配器實作 (FindingRepositoryAdapter, ...)
│   ├── persistence/                         # JPA 資料存取模組 (entity/ 與 repository/)
│   ├── client/                              # 外部系統介接 (SonarQubeRestClient, ...)
│   └── scheduler/                           # 排程任務 (ScanReconciliationScheduler)
│
├── presentation/                            # 4. 表現層 (Inbound Adapters - REST APIs)
│   ├── controller/                          # REST 控制器 (FindingController, ScanReportUploadController, ...)
│   ├── resource/
│   │   ├── in/                              # 請求資源純資料載體 (UploadScanReportResource, ...)
│   │   └── out/                             # 回應資源純資料載體 (ScanReportUploadedResource, ...)
│   └── assembler/                           # 資源與 Command/DTO 轉換器 (ScanReportResourceAssembler, ...)
│
├── config/                                  # 5. 全域配置層 (Spring Configuration, Swagger, Redis 等)
└── ScsFacadeApplication.java                # Spring Boot 應用程式入口
```

---

## 🔌 完整 REST API 端點清單

服務啟動後，完整 Swagger 互動文件可於 `http://localhost:8080/swagger-ui.html` 存取。

### 1. 弱點管理 (Findings)
| 方法 | 端點路徑 | 說明 | 參數 / Body |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/findings` | 條件式分頁查詢弱點清單 | 查詢參數：`projectId`, `status`, `severity`, `toolName`, `filePath`, `page`, `size` |
| `GET` | `/api/v1/findings/{id}` | 查詢單一弱點詳細資訊 | 路徑變數：`id` (Finding UUID) |

### 2. 掃描執行紀錄 (Scan Executions)
| 方法 | 端點路徑 | 說明 | 參數 / Body |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/scan-executions` | 條件查詢掃描執行紀錄 | 查詢參數：`projectId`, `status`, `page`, `size` |
| `GET` | `/api/v1/scan-executions/{id}` | 查詢單一掃描執行狀態與彙總統計 | 路徑變數：`id` (ScanExecution UUID) |

### 3. Push 模式：SARIF 報告檔案上傳
| 方法 | 端點路徑 | Content-Type | 說明 |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/scans/upload` | `multipart/form-data` | 接收 Trivy、Gitleaks、Semgrep 等產出之 SARIF 2.1.0 檔案，完成指紋計算與結算。 |

* **請求參數 (Form Data)**：
  * `file`：SARIF 格式 JSON 報告檔案 (`MultipartFile`)
  * `projectId`：專案識別代碼 (如 `payment-service`)
  * `branch`：Git 分支名稱 (如 `main`, `feature/auth`)
  * `commitHash`：Git 提交雜湊值 (40 位長度)
  * `toolName`：掃描工具名稱 (如 `trivy`, `gitleaks`, `semgrep`)
  * `externalTaskId`：外部 CI/CD Pipeline 或 Job ID (選填)

### 4. Push 模式：SonarQube Webhook 與主動拉取
| 方法 | 端點路徑 | 說明 |
| :--- | :--- | :--- |
| `POST` | `/api/v1/scans/sonarqube/webhook` | 接收 SonarQube Server 官方 Webhook 分析回呼 Payload。 |
| `POST` | `/api/v1/scans/sonarqube/trigger` | 手動提供 `projectKey` 與 `analysisId` 主動觸發 SonarQube Issue 拉取與攝入。 |

### 5. Pull 模式：低頻排程對帳與隨選補償
| 方法 | 端點路徑 | 說明 |
| :--- | :--- | :--- |
| `POST` | `/api/v1/scans/reconcile` | 隨選手動觸發指定單一專案之對帳補償 (`{"projectId": "scs-facade"}`)。 |
| `POST` | `/api/v1/scans/reconcile-all` | 手動觸發設定檔中所有納管專案 (`scs.reconciliation.managed-projects`) 之全量對帳。 |

### 6. 品質門禁審查 (Quality Gate)
| 方法 | 端點路徑 | 說明 |
| :--- | :--- | :--- |
| `POST` | `/api/v1/quality-gate/evaluate` | 傳入專案代碼與門檻要求，即時計算活躍弱點是否合格（回傳 `PASSED` / `FAILED`）。 |

---

## 🛠️ 技術棧與環境依賴

* **語言規格**：Java 21 (Eclipse Temurin JDK 21)
* **核心框架**：Spring Boot 3.3.4 (Web, Data JPA, Data Redis, Validation)
* **分散式協同**：Redisson 3.31.0 (分散式鎖、看門狗機制)
* **資料儲存**：PostgreSQL 15+ (生產環境) / H2 Database (本機開發與測試)
* **API 規格與文件**：SpringDoc OpenAPI 2.6.0 (Swagger UI)
* **測試框架**：JUnit 5, Mockito, AssertJ (123+ 測試全數通過，100% 通過率)
* **代碼覆蓋率**：JaCoCo Maven Plugin 0.8.12
* **靜態檢測與品質門禁**：SonarQube LTS Community (Clean as You Code 門檻：覆蓋率 $\ge 80\%$, Rating A)
* **容器化基礎設施**：Docker & Docker Compose (整合 GitLab CE + GitLab Runner + SonarQube)

---

## 🚀 快速啟動指南

### 1. 環境變數配置

專案支援標準 Spring Boot 環境變數替換，核心配置如下：

| 環境變數 | 預設值 | 說明 |
| :--- | :--- | :--- |
| `PORT` | `8080` | 微服務 HTTP 監聽通訊埠 |
| `DB_URL` | `jdbc:postgresql://localhost:5432/scs_facade` | PostgreSQL 資料庫連線 URL (測試環境預設使用 H2) |
| `DB_USERNAME` | `postgres` | 資料庫使用者名稱 |
| `DB_PASSWORD` | `postgres` | 資料庫密碼 |
| `REDIS_HOST` | `localhost` | Redis 主機位址 |
| `REDIS_PORT` | `6379` | Redis 通訊埠 |
| `SONAR_HOST_URL` | `http://localhost:9000` | SonarQube 伺服器根網址 |
| `SONAR_TOKEN` | (預設測試 Token) | 具備 Issue 讀取權限之 SonarQube 使用者或全域 Token |
| `SCS_RECONCILIATION_ENABLED` | `true` | 是否啟用每日低頻排程對帳 |
| `SCS_RECONCILIATION_CRON` | `0 0 2 * * ?` | 排程對帳 Cron 表達式（預設每日凌晨 02:00） |
| `SCS_MANAGED_PROJECTS` | `scs-facade` | 自動對帳納管的專案代碼清單（以逗號分隔） |

### 2. 本地編譯與執行單元測試

專案內建 Maven Wrapper，確保各環境建置版本一致：

```bash
# Windows
$env:JAVA_HOME="C:\path\to\jdk-21"; .\mvnw.cmd clean test

# Linux / macOS
export JAVA_HOME=/path/to/jdk-21; ./mvnw clean test
```

### 3. 本機啟動微服務

```bash
# Windows
$env:JAVA_HOME="C:\path\to\jdk-21"; .\mvnw.cmd spring-boot:run

# Linux / macOS
export JAVA_HOME=/path/to/jdk-21; ./mvnw spring-boot:run
```

啟動後即可存取：
* **Swagger UI 互動介面**：[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **OpenAPI 3.0 JSON**：[http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
* **H2 資料庫控制台**（若使用 H2）：[http://localhost:8080/h2-console](http://localhost:8080/h2-console)

---

## 🐳 Docker Compose 一鍵啟動 DevSecOps 基礎設施

專案於 `docker-compose/docker-compose.yml` 中提供了完整的本地 DevSecOps 整合環境，包含自建 GitLab CE、GitLab Runner、SonarQube 與 PostgreSQL：

```bash
# 切換至專案根目錄並啟動全套容器群
docker compose -f docker-compose/docker-compose.yml up -d
```

啟動後服務端點：
* **GitLab CE**：[http://localhost:8888](http://localhost:8888)
* **SonarQube**：[http://localhost:9000](http://localhost:9000)（預設帳密：`admin / admin`）
* **GitLab Runner**：自動掛載宿主機 Docker Socket，直接支援 DinD 容器建置與掃描

---

## 🚦 GitLab CI 與 SonarQube 品質門檻 (Quality Gate)

專案已配備正式的 `.gitlab-ci.yml`，每次推送至 `main` 或 `dev` 分支時自動執行 `sonarqube-analysis` 任務。

### 硬性品質門檻指標：
1. **單元與整合測試**：100% 通過（目前包含 123+ 個測試案例，0 失敗、0 錯誤）。
2. **新代碼覆蓋率 (New Code Coverage)**：由 JaCoCo 統計，排除純載體與配置類後必須 $\ge 80.0\%$。
3. **可靠性評級 (Reliability Rating)**：新代碼 **0 Bugs**，達成 Rating A。
4. **安全性評級 (Security Rating)**：新代碼 **0 Vulnerabilities**，達成 Rating A。
5. **可維護性評級 (Maintainability Rating)**：技術債比例 $\le 5.0\%$，達成 Rating A。
6. **重複程式碼密度 (Duplications)**：新代碼重複行 $\le 3.0\%$。

---

## 📚 延伸設計與整合文件 (Documentation Index)

詳細架構設計、實作指南與 CI 操作說明請參閱 `doc/` 目錄下各專題文件：

* 📘 [系統架構與 DDD 設計指南](doc/source_code_scan_facade_design.md)
* 🚀 [Push 模式 (CI/CD 驅動 SARIF 檔案上傳) 功能設計與整合指南](doc/Push%20模式%20%28CI_CD%20驅動檔案上傳%29%20功能設計與整合指南.md)
* 🔄 [Pull 模式 (低頻排程對帳與最終一致性補償) 功能設計與整合指南](doc/Pull%20模式%20%28低頻排程對帳與最終一致性補償%29%20功能設計與整合指南.md)
* 🛡️ [GitLab CI 通過條件與品質門檻規範](doc/CI%20通過條件與品質門檻規範.md)
* 🎯 [SonarQube Quality Gate 與測試覆蓋率整合指南](doc/SonarQube%20Quality%20Gate%20與測試覆蓋率整合指南.md)
* 🌐 [GitLab CI、SonarQube 與 SCS Facade 整合架構](doc/ci_cd_integration_architecture.md)
* 🛠️ [建立 GitLab CI 與 SonarQube 操作指南](doc/建立%20GitLab%20CI.md)
* 🔑 [GitLab 首次登入操作指南](doc/GitLab%20首次登入操作指南.md)
