# SonarQube Quality Gate 與 JaCoCo 測試覆蓋率整合實踐指南

本文件詳細記錄 `scs-facade` 在導入 GitLab CI 與 SonarQube 靜態代碼分析過程中，針對 **Quality Gate（品質門檻）未通過導致 CI 呈現橘色警告** 的完整成因分析、解決方案評估，以及導入 **JaCoCo 測試覆蓋率** 達成 100% 綠燈的完整實踐過程。

---

## 1. 現象與根本原因分析 (Root Cause Analysis)

### 1.1 異常現象：GitLab CI 呈現橘色驚嘆號
在推播包含新 Java 程式碼的 Commit 後，GitLab CI 的 Pipeline 顯示為綠色勾勾，但點進內部的 Job 清單時，發現任務呈現橘色的 **`! Failed (allowed to fail)`**：

```text
Status: ! Failed (allowed to fail)
Job: #13: sonarqube-analysis
Pipeline: #8 created by root
```

### 1.2 根本原因一：SonarQube Quality Gate 門檻攔截
在 `.gitlab-ci.yml` 中配置了 `-Dsonar.qualitygate.wait=true`，這意味著 Maven 靜態掃描外掛在將分析報告上傳給 SonarQube 伺服器後，會**在背景等待品質閘門的裁定結果**。

透過 SonarQube REST API 查詢專案品質閘門狀態：
```bash
curl -u "<SONAR_TOKEN>:" "http://localhost:9000/api/qualitygates/project_status?projectKey=scs-facade"
```

回傳結果顯示：
```json
{
  "projectStatus": {
    "status": "ERROR",
    "conditions": [
      { "status": "OK", "metricKey": "new_reliability_rating", "actualValue": "1" },
      { "status": "OK", "metricKey": "new_security_rating", "actualValue": "1" },
      { "status": "OK", "metricKey": "new_maintainability_rating", "actualValue": "1" },
      { "status": "ERROR", "metricKey": "new_coverage", "errorThreshold": "80", "actualValue": "0.0" },
      { "status": "OK", "metricKey": "new_duplicated_lines_density", "actualValue": "0.0" }
    ]
  }
}
```
* **核心成因**：SonarQube 官方預設的「Sonar way」品質閘門要求 **新程式碼（New Code）之測試覆蓋率必須 $\ge 80.0\%$**。
* 由於先前新增了 Controller、Resource 與 Assembler 等類別，但尚未配置 JaCoCo 產出覆蓋率報告，SonarQube 判定新代碼覆蓋率為 `0.0%`，導致 Quality Gate 狀態判定為 `ERROR (FAILED)`。
* Maven 收到 SonarQube 回傳的失敗判定後，拋出 `BUILD FAILURE`，並以結束碼 `1` 退出。

### 1.3 根本原因二：為什麼是橘色而非紅色？
在 `.gitlab-ci.yml` 中定義了：
```yaml
allow_failure: true
```
GitLab CI 遵循此配置，判定此任務「即使執行失敗，也不會強制中斷整個 Pipeline」，因此畫面以 **橘色驚嘆號（容許失敗）** 警示開發者。

---

## 2. 解決方案對比與決策

| 評估面向 | 方案 A：CI 僅上傳報告，不等待 Quality Gate | 方案 B：落實品質門檻，導入 JaCoCo 與補齊單元測試（本專案採納） |
| :--- | :--- | :--- |
| **作法** | 移除 `.gitlab-ci.yml` 中的 `-Dsonar.qualitygate.wait=true` | 1. `pom.xml` 加入 JaCoCo 外掛<br>2. 撰寫完整 BDD 單元測試<br>3. CI 指令由 `clean compile` 升級為 `clean test` |
| **優點** | 設定最快，任何代碼只要能編譯就一定保證綠燈 | 建立嚴謹工程文化，保證核心商業邏輯與新端點具備足夠測試保護 |
| **缺點** | 品質閘門形同虛設，無法在 CI 階段提早發現覆蓋率或缺陷問題 | 需要投入時間撰寫單元測試與配置覆蓋率報告收集 |
| **最終狀態** | 綠色 Passed（但 SonarQube 後台仍可能亮紅燈） | **GitLab CI 與 SonarQube Quality Gate 雙重全綠燈 (100% Passed)** |

---

## 3. 方案 B 完整落地實作步驟

### 步驟 1：調整 `pom.xml` 整合 JaCoCo 與 Surefire

在 `pom.xml` 中加入 JaCoCo 外掛、指定 UTF-8 編碼，並透過 Surefire 外掛將 JaCoCo JavaAgent 掛載至測試執行環境。

```xml
<properties>
    <java.version>21</java.version>
    <springdoc.version>2.6.0</springdoc.version>
    <!-- 明確指定全域編碼為 UTF-8，避免跨平台與 Windows 繁體中文路徑亂碼 -->
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
</properties>

<build>
    <plugins>
        <!-- Spring Boot Maven Plugin -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>

        <!-- JaCoCo Maven Plugin：代碼覆蓋率分析外掛 -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.12</version>
            <executions>
                <!-- 測試前準備 Agent：掛載 JavaAgent 以統計 bytecode 執行情形 -->
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <!-- 測試後產出報表：於 test 階段產出 XML/HTML 格式分析報告 -->
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>

        <!-- Maven Surefire Plugin：單元測試執行外掛 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <!-- 關鍵：使用 @{argLine} 串接 JaCoCo Agent 參數，並啟用動態 Agent 載入支援 -->
                <argLine>@{argLine} -XX:+EnableDynamicAgentLoading</argLine>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

### 步驟 2：撰寫表現層單元測試 (符合 AGENTS.md 規範)

為新增加的 Controller、Resource 與 Assembler 撰寫完整的單元測試，嚴格遵守專案三大架構規範：
* **純特定 Import**：禁止萬用字元（`*`）或全類名寫法。
* **BDD 與 Gherkin 註解**：在每個測試方法上方以 `Feature / Scenario / Given / When / Then` 註釋業務場景。
* **全分支與邊界覆蓋**：覆蓋正常資料流、空值防護以及非法參數例外。

#### (1) `SonarQubeResourceAssemblerTest.java`
* **路徑**：`src/test/java/com/example/scsfacade/presentation/assembler/SonarQubeResourceAssemblerTest.java`
* **測試情境**：
  1. 手動觸發匯入請求轉命令 (`toCommand(IngestSonarQubeResource)`)
  2. 手動觸發請求傳入 `null` 時安全防護
  3. 原生 Webhook 請求轉換完整酬載 (`toCommand(ReceiveSonarQubeWebhookResource)`)
  4. 原生 Webhook 請求缺少分支與版本時之安全預設值
  5. 缺少專案資訊時拋出 `IllegalArgumentException`
  6. 弱點 DTO 清單轉回應載體 (`toResource`)
  7. 弱點 DTO 清單為空時防護

#### (2) `SonarQubeWebhookControllerTest.java`
* **路徑**：`src/test/java/com/example/scsfacade/presentation/controller/SonarQubeWebhookControllerTest.java`
* **測試技術**：採用 Spring `MockMvcStandaloneSetup`，毫秒級啟動且完整驗證 HTTP 協定細節。
* **測試情境**：
  1. POST `/api/v1/scans/sonarqube/trigger`：驗證接收觸發請求並調用 UseCase 回傳 200 OK。
  2. POST `/api/v1/scans/sonarqube/webhook`：驗證接收原生 Webhook 酬載並調用 UseCase 回傳 200 OK。

---

### 步驟 3：更新 `.gitlab-ci.yml` 收集覆蓋率

將 CI 腳本由 `mvn clean compile` 改為 `mvn clean test`，並明確指定 JaCoCo XML 報告位置：

```yaml
sonarqube-analysis:
  stage: sonarqube-check
  image: maven:3.9-eclipse-temurin-21

  variables:
    SONAR_USER_HOME: "${CI_PROJECT_DIR}/.sonar"
    GIT_DEPTH: "0"
    MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"

  cache:
    key: "${CI_COMMIT_REF_SLUG}"
    paths:
      - .sonar/cache
      - .m2/repository

  script:
    - >-
      mvn clean test
      org.sonarsource.scanner.maven:sonar-maven-plugin:sonar
      -Dspring-boot.repackage.skip=true
      -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
      -Dsonar.host.url="${SONAR_HOST_URL:-http://sonarqube:9000}"
      -Dsonar.token="${SONAR_TOKEN}"
      -Dsonar.login="${SONAR_TOKEN}"
      -Dsonar.projectKey="scs-facade"
      -Dsonar.qualitygate.wait=true

  allow_failure: true
  only:
    - main
    - dev
```

---

## 4. 跨平台相容性與排錯要點 (Windows vs Docker Linux)

在實踐過程中，遇到了跨平台環境特有的陷阱，特別整理如下以供後續專案借鑑：

### 4.1 繁體中文路徑與 Windows CMD 代碼頁編碼衝突
* **現象**：在 Windows 本地端執行時，若專案目錄包含中文（如 `D:\桌面\VibeCodingWorkSpace\...`），Surefire 在 Fork Java 程序時透過 `cmd.exe /C` 啟動，若未指定 UTF-8，路徑中的 `桌面` 會被解析為亂碼字元（如 `D:\ୱ\...`），導致 JaCoCo Agent 找不到路徑而略過寫入 `jacoco.exec`。
* **對策**：
  1. 在 `pom.xml` 中明確宣告 `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>`。
  2. 在 GitLab CI 中（運行於 Linux 容器內），工作目錄為標準 ASCII 路徑 `/builds/scs-group/scs-facade`，天然避開 Windows cmd.exe 代碼頁限制。

### 4.2 JaCoCo Agent Jar 依賴與生命週期
* **現象**：若未在 JaCoCo 外掛中宣告 `<goal>prepare-agent</goal>`，Maven 本地倉庫不會下載 `org.jacoco.agent-runtime.jar`，導致 Surefire 啟動 JVM 時拋出 `Error opening zip file or JAR manifest missing`。
* **對策**：必須同時保留 `prepare-agent` 與 `report` 兩大生命週期，並透過 `@{argLine}` 將引數動態傳遞給 Surefire。

---

## 5. 最終驗證結果

### 5.1 本地單元測試與覆蓋率統計
本地端執行 `.\mvnw.cmd test`：
* **測試執行數**：**16 / 16 passed**（0 failures, 0 errors, 0 skipped）
* **新加入類別覆蓋率**：
  * `SonarQubeWebhookController`：**100%** (49/49 instructions covered)
  * `SonarQubeResourceAssembler`：**100%** (81/81 instructions covered)
  * `ReceiveSonarQubeWebhookResource` 及其所有內部節點：**100%** (63/63 instructions covered)

### 5.2 SonarQube 伺服器指標判定
透過 API 驗證 SonarQube Quality Gate：
* **New Reliability Rating**：`A (OK)`
* **New Security Rating**：`A (OK)`
* **New Maintainability Rating**：`A (OK)`
* **New Duplicated Lines Density**：`0.0% (OK)`
* **New Code Coverage**：**`100.0% (OK)`**（遠超越 80.0% 門檻）
* **總體狀態**：**`OK`**

### 5.3 GitLab CI/CD 執行結果
* **`dev` 分支**：Pipeline `#11` (Job `#16`) $\rightarrow$ **`✔ Passed` (綠色勾勾)**
* **`main` 分支**：Pipeline `#12` (Job `#17`) $\rightarrow$ **`✔ Passed` (綠色勾勾)**

專案成功建立了完整的單元測試覆蓋、JaCoCo 報告生成、SonarQube 門檻審核，以及 Webhook 自動同步的 DevSecOps 自動化閉環！
