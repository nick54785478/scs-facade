# GitLab CI 通過條件與代碼品質門檻規範 (CI Quality Gate Specification)

本文件定義 `scs-facade` 專案在 GitLab CI/CD 流程中的**正式通過條件（Pass Criteria）**、**SonarQube 品質閘門（Quality Gate）標準**、**測試覆蓋率範圍與排除規則**，以及**開發者提交代碼前的檢核清單與排錯指南**。

---

## 一、 CI Pipeline 通過總覽 (Executive Summary)

每一次 Commit 推送至 `dev` 或 `main` 分支時，GitLab CI 將自動觸發 `sonarqube-analysis` 任務。該任務必須滿足以下**三大核心階段的嚴格條件**，整體 Pipeline 方能呈現綠色通過標記（`Passed`）：

```
[ Stage 1: Build & Compile ] ──> [ Stage 2: Unit / Integration Tests ] ──> [ Stage 3: SonarQube Quality Gate ]
       編譯成功 (Zero Error)               87+ 項測試全數通過 (100% Pass)              5 大指標全數達標 (Rating A & 80% Coverage)
```

| 檢驗層次 | 檢核指標 | 通過標準 (Pass Condition) | 判定工具 / 機制 |
| :--- | :--- | :--- | :--- |
| **1. 編譯與依賴** | 專案建置狀態 | `BUILD SUCCESS`，零編譯錯誤與相依衝突 | Maven 3.9 (Eclipse Temurin JDK 21) |
| **2. 自動化測試** | 單元與整合測試 | **100% 通過率**（0 Failures, 0 Errors, 0 Skipped） | JUnit 5 + Mockito + AssertJ |
| **3. 新代碼覆蓋率** | New Code Coverage | **$\ge 80.0\%$**（排除純載體與基礎設施後） | JaCoCo 0.8.12 (`jacoco.xml`) |
| **4. 可靠性評級** | Reliability Rating | **Rating A**（新代碼 0 Bugs，零 Critical/Major 缺陷） | SonarQube Scanner 5.8 |
| **5. 安全性評級** | Security Rating | **Rating A**（新代碼 0 Vulnerabilities，零安全漏洞） | SonarQube Scanner 5.8 |
| **6. 可維護性評級**| Maintainability Rating | **Rating A**（技術債比例 $\le 5.0\%$，Code Smells 最小化） | SonarQube Scanner 5.8 |
| **7. 重複代碼密度**| Duplicated Lines | **$\le 3.0\%$**（新代碼重複程式碼密度） | SonarQube CPD Block Indexer |
| **8. 閘門判定狀態**| Quality Gate Status | **`STATUS: OK`**（Wait 機制回傳合格） | `-Dsonar.qualitygate.wait=true` |

---

## 二、 SonarQube Quality Gate 五大指標硬性規範

專案採用 SonarQube Clean as You Code (CaYC) 理念，重點審查**新代碼週期（New Code Period）**所引入的程式碼變更：

### 2.1 新代碼覆蓋率 (New Code Coverage $\ge 80.0\%$)
* **標準**：凡屬於業務邏輯範疇之新代碼或修改代碼，其實際行覆蓋率（Line Coverage）與分支覆蓋率（Branch Coverage）綜合計算值必須達到 **80.0% 以上**。
* **報告來源**：由 JaCoCo 外掛在 `mvn clean test` 期間生成之 `target/site/jacoco/jacoco.xml`。
* **計算基數**：已套用架構排除規則（見第三章節），排除純配置、純實體與純 DTO 載體。

### 2.2 可靠性評級 (New Reliability Rating $\le 1$ / Rating A)
* **標準**：新代碼中嚴格禁止出現任何 **Blocker / Critical / Major** 級別的 Bug。
* **特別規範**：若遇底層框架或工具庫誤判（如 Redisson 分散式鎖之 `java:S2222` 跨方法釋放誤判），必須在類別或方法上標註 `@SuppressWarnings("java:XXXX")`，並加上詳細 Javadoc 說明跨方法釋放的業務架構合理性，禁止無故抑制或降低代碼品質。

### 2.3 安全性評級 (New Security Rating $\le 1$ / Rating A)
* **標準**：新代碼中嚴格禁止任何安全弱點（Vulnerabilities）與注入風險（如 SQL Injection、Command Injection、Path Traversal）。
* **機敏資訊防護**：代碼中禁止寫入任何明文密碼、API 金鑰或 Token（由 TextAndSecretsSensor 掃描阻擋）。

### 2.4 可維護性評級 (New Maintainability Rating $\le 1$ / Rating A)
* **標準**：新代碼之技術債率（Technical Debt Ratio）必須在 **5.0% 以下**。
* **代碼異味管控**：避免過長方法、過深巢狀（Cyclomatic Complexity）、未使用的私有欄位與無效引入（Unused Imports）。

### 2.5 重複代碼率 (New Duplicated Lines Density $\le 3.0\%$)
* **標準**：新代碼之重複行比例必須在 **3.0% 以下**。
* **規範**：重複的資料組裝或業務邏輯應抽取至 Assembler、Mapper 或共用輔助方法，禁止隨意複製貼上。

---

## 三、 測試覆蓋率範圍與架構排除規範 (Coverage Scope & Exclusions)

依據專案之 **DDD (領域驅動設計)** 與 **Clean Architecture (六角形架構)** 分層，各層程式碼之覆蓋率要求與排除設定如下：

### 3.1 必須高覆蓋之核心模組 (Mandatory High Coverage)

| 架構分層 | 套件路徑 (Package) | 覆蓋重點 |
| :--- | :--- | :--- |
| **Domain Layer** | `domain/*/aggregate/root/`<br>`domain/*/aggregate/vo/` | **核心商業邏輯**：聚合根狀態轉移（如 `ScanExecution.complete()`, `Finding.markAsResolved()`）、實質物件不可變性與驗證防護、領域事件發布機制。 |
| **Application Layer** | `application/service/` | **流程調度**：CQRS Command/Query Services、事務邊界控制、分散式鎖防重放機制、例外分支與重試流程。 |
| **Infrastructure Layer** | `infrastructure/adapter/` | **出站適配器**：資料庫 Repository 適配、訊息事件發布適配、SonarQube API 介接與解析適配、分散式鎖適配。 |
| **Presentation Layer** | `presentation/controller/`<br>`presentation/assembler/` | **入站適配器與防腐轉換**：REST 端點請求驗證、HTTP 狀態碼對應、Resource 與 Command/Query/DTO 間之雙向資料轉換。 |

### 3.2 官方核准之覆蓋率排除項目 (Coverage Exclusions)

純資料結構、ORM 映射實體、環境配置與啟動類不含可變商業邏輯，統一於 `pom.xml` 中配置 `<sonar.coverage.exclusions>` 排除計算：

```xml
<sonar.coverage.exclusions>
    **/config/**,                               <!-- Spring 全域組態與技術配置 (The Dirtiest Layer) -->
    **/infrastructure/persistence/**/entity/**, <!-- JPA ORM 實體物件 (純資料庫映射) -->
    **/infrastructure/client/**,                <!-- 外部 HTTP Client 介面與第三方 DTO -->
    **/presentation/resource/**,                <!-- REST Request/Response 純資料載體 -->
    **/application/dto/**,                      <!-- CQRS 查詢回傳純 DTO (Result 物件) -->
    **/application/view/**,                     <!-- CQRS 投影純 View 物件 -->
    **/application/command/**,                  <!-- CQRS Command 純參數封裝物件 -->
    **/application/query/**,                    <!-- CQRS Query 純參數封裝物件 -->
    **/domain/**/event/**,                      <!-- 領域事件 Payload 純資料載體 -->
    **/domain/**/exception/**,                  <!-- 領域自定義例外類別 -->
    **/domain/event/DomainEvent.java,           <!-- 領域事件標記介面 -->
    **/ScsFacadeApplication.java                <!-- Spring Boot 框架啟動入口 -->
</sonar.coverage.exclusions>
```

---

## 四、 測試撰寫與驗證準則 (Testing Guidelines)

依照 `.agents/AGENTS.md` 規範，所有提交至本專案之單元與整合測試，必須嚴格遵守以下條款：

1. **強制具備 UseCase 測試 (Mandatory UseCase Tests)**：
   - 每個新增或異動之 Application UseCase（如 `IngestSonarQubeUseCase`、`EvaluateQualityGateUseCase` 等），必須具備涵蓋正常流程、異常處理、等冪防重放與事件發布之完整測試。
2. **BDD 與 Gherkin 測試註解規範**：
   - 每個測試方法上方必須使用 Gherkin 語法撰寫 Javadoc：
     ```java
     /**
      * Feature: 掃描執行批次啟動
      * Scenario: 建立新掃描並發布 ScanExecutionStartedEvent
      * Given: 專案代碼、分支與觸發來源 PIPELINE
      * When: 呼叫 ScanExecution.start()
      * Then: 狀態為 RUNNING，累積 ScanExecutionStartedEvent
      */
     @Test
     @DisplayName("掃描啟動 - 應正確初始化並發布啟動事件")
     void shouldStartScanExecutionSuccessfully() { ... }
     ```
3. **等冪性與異常邊界測試**：
   - 針對分散式鎖爭搶失敗、重試機制、重複呼叫（如已完成之狀態重複呼叫 `complete()`）皆需具備明確單元測試。

---

## 五、 開發者提交前檢驗 SOP (Pre-Push Checklist)

開發者在執行 `git push` 前，請務必於本地端依序完成以下檢查：

```bash
# 步驟 1：本機編譯測試並執行所有單元測試
$env:JAVA_HOME = "C:\Users\<user>\.jdks\temurin-22.0.2"   # 設定 JDK 21+
.\mvnw.cmd test

# 步驟 2：確認測試結果
# 輸出必須滿足：
# [INFO] Tests run: 87, Failures: 0, Errors: 0, Skipped: 0
# [INFO] BUILD SUCCESS

# 步驟 3：確認 git 工作目錄乾淨，無未追蹤之多餘測試檔案
git status
```

---

## 六、 CI 失敗排錯手冊 (Troubleshooting Playbook)

當 GitLab CI 出現紅色或失敗時，請依以下徵兆定位問題：

| 失敗徵兆 | 可能原因 | 排除步驟 |
| :--- | :--- | :--- |
| **`QUALITY GATE STATUS: FAILED`**<br>`new_coverage < 80%` | 新增的類別（如 Service、Adapter、Controller）未撰寫測試，或分支覆蓋率不足。 | 1. 檢視 SonarQube Dashboard 之 Measures $\rightarrow$ Coverage 標籤頁。<br>2. 找出標紅的新程式碼檔案。<br>3. 補齊對應的 UseCase/Adapter 測試案例；若是純資料載體，檢查是否漏加進 `pom.xml` 的排除清單。 |
| **`QUALITY GATE STATUS: FAILED`**<br>`new_reliability_rating > 1` | 存在 Critical / Major Bug（如未釋放資源、潛在空指標、鎖操作不當）。 | 1. 檢視 SonarQube Issues 標籤頁，篩選 Severity 為 Critical/Major。<br>2. 若為真缺陷，立即修改修復。<br>3. 若為 Redisson 分散式鎖等合理跨方法管理，參照 `DistributedLockAdapter.java` 標註 `@SuppressWarnings` 並附上明確說明。 |
| **`BUILD FAILURE`**<br>`Tests run with failure` | 本地未跑過測試即推送，代碼與現有測試斷言不符。 | 在本地端執行 `.\mvnw.cmd test`，依據失敗的測試方法定位邏輯變更並修正。 |
| **`Sonar 401 Unauthorized`** | SonarQube 認證權限過期或參數不相容。 | SonarQube 9.9 LTS 同時要求 `-Dsonar.token` 與 `-Dsonar.login`，請確認 `.gitlab-ci.yml` 包含兩者，且 GitLab CI/CD Variables 中的 `SONAR_TOKEN` 有效。 |

---

## 七、 版本歷史與修訂紀錄

| 版本 | 修訂日期 | 修訂人員 | 變更說明 |
| :--- | :--- | :--- | :--- |
| **v1.0.0** | 2026-10-12 | DevSecOps 團隊 | 初始版本：整合 GitLab CI、JaCoCo 覆蓋率與 SonarQube 基本掃描流程。 |
| **v1.1.0** | 2026-10-14 | Architecture 團隊 | 1. 制定正式 Quality Gate 5 大硬性門檻指標。<br>2. 配合 DDD 與 Clean Architecture 定義覆蓋率排除規範（排除 Command/Query/Event/Entity 等純資料載體）。<br>3. 解決 Redisson S2222 鎖誤判，達成 Reliability Rating A。<br>4. 新增完整 87 項單元與整合測試，覆蓋率達成 83.6% 正式通過標準。 |
