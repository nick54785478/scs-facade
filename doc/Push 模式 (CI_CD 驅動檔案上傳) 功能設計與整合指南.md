# Push 模式 (CI/CD 驅動檔案上傳) 功能設計與整合指南

---

## 一、 模組背景與設計初衷 (Background & Motivation)

### 1.1 業務挑戰
在現代 DevSecOps 流程中，企業通常不會只依賴單一靜態掃描工具，而是會組合多種專門的安全檢測工具建立縱深防禦：
- **靜態源碼安全 (SAST)**：SonarQube、Semgrep、CodeQL
- **第三方開源相依套件 (SCA)**：Trivy、Snyk、OWASP Dependency-Check
- **機敏憑證洩漏檢測 (Secret Detection)**：Gitleaks、TruffleHog

然而，除了 SonarQube 有集中託管的伺服器與 Webhook 之外，**Trivy、Gitleaks、Semgrep 大多數以 CLI 工具形式運行於 CI/CD Pipeline 的短暫容器中**。掃描結束後容器隨即銷毀，若無統一的彙整門面（Facade），各工具的掃描數據將嚴重分散、難以追蹤歷史趨勢，更無法在企業層級落實統一的品質門禁（Quality Gate）。

### 1.2 設計目標
本模組實作 **Push 模式（CI/CD 驅動檔案上傳）**，以國際 OASIS 標準 **SARIF 2.1.0 (Static Analysis Results Interchange Format)** 為核心資料交換規格，達成以下三大願景：
1. **多源通用接入（Multi-Engine Ingestion）**：一套標準化端點即可直接接收 Trivy、Gitleaks、Semgrep 等主流 CLI 工具產出的安全報告。
2. **精準 Commit 對齊與指紋去重**：直接在 CI/CD 階段綁定當次掃描之 `commitHash`、`branch` 與 `projectId`，結合 SHA-256 跨掃描指紋去重，解決行號位移導致的誤報。
3. **同輪次多工具生命週期隔離**：多工具於同一 Commit 上傳時，各自的結算與修復判定相互獨立，互不干擾。

---

## 二、 系統架構與六角形分層設計 (Architecture & DDD)

嚴格遵守本專案之 **DDD (領域驅動設計)** 與 **Clean Architecture (六角形架構 / Ports & Adapters)** 分層邊界：

```
 ┌────────────────────────────────────────────────────────────────────────┐
 │                      Presentation Layer (Inbound)                      │
 │  ScanReportUploadController (POST /api/v1/scans/upload)               │
 │  └── ScanReportResourceAssembler (防腐層轉換)                           │
 └───────────────────────────────────┬────────────────────────────────────┘
                                     │ UploadScanReportCommand
                                     ▼
 ┌────────────────────────────────────────────────────────────────────────┐
 │                       Application Layer (Use Case)                     │
 │  UploadScanReportUseCase (Inbound Port)                                │
 │  └── UploadScanReportCommandService (CQRS 流程調度與事務邊界)           │
 │       ├── ScanReportParserPort (Outbound Port)                         │
 │       ├── DistributedLockPort (Redisson 等冪防重鎖)                     │
 │       └── Finding / ScanExecution Repository Ports                     │
 └───────────────────────┬───────────────────────────────┬────────────────┘
                         │                               │
                         ▼                               ▼
 ┌────────────────────────────────┐            ┌──────────────────────────┐
 │ Infrastructure Layer (Outbound)│            │       Domain Layer       │
 │ SarifReportParserAdapter       │            │ ScanExecution (聚合根)   │
 │ (Jackson 串流解析 SARIF 2.1.0) │            │ Finding (聚合根)         │
 └────────────────────────────────┘            │ FindingLifecycle (VO)    │
                                               │ FindingFingerprint (VO)  │
                                               └──────────────────────────┘
```

### 2.1 各層元件職責與契約

| 架構分層 | 關鍵類別 | 命名規範類別 | 核心職責 |
| :--- | :--- | :--- | :--- |
| **Presentation** | `ScanReportUploadController` | Inbound Adapter | 開放 `POST /api/v1/scans/upload`，接收 `multipart/form-data` 請求並校驗參數。 |
| **Presentation** | `UploadScanReportResource` | Request (`V + N + Resource`) | 上傳中繼資料純資料載體（`projectId`, `branch`, `commitHash`, `toolName`, `externalTaskId`）。 |
| **Presentation** | `ScanReportUploadedResource` | Response (`N + Ved + Resource`) | 結算完成之統計指標與品質門禁判定純資料載體。 |
| **Presentation** | `ScanReportResourceAssembler` | Assembler / Mapper | 負責 Presentation Resource 與 Application Command/Result 間之雙向資料映射。 |
| **Application** | `UploadScanReportUseCase` | Inbound Port (`... + UseCase`) | 應用層用例合約，接收 `UploadScanReportCommand` 並回傳 `ScanReportUploadedResult`。 |
| **Application** | `ScanReportParserPort` | Outbound Port (`... + Port`) | 報告解析出站埠，定義 `parse(byte[], defaultTool)` 與 `supports(filename, byte[])` 合約。 |
| **Application** | `UploadScanReportCommandService` | Inbound Adapter (`... + CommandService`) | 流程總指揮：搶奪 Redisson 鎖、調度解析器、比對生命週期狀態、結算 ScanExecution、保存並發布事件。 |
| **Infrastructure** | `SarifReportParserAdapter` | Outbound Adapter (`... + Adapter`) | 實作 `ScanReportParserPort`，解析 SARIF 2.1.0 規格並對齊為領域資料。 |
| **Domain** | `Finding`, `ScanExecution` | Aggregate Roots | 純 Java 領域模型，封裝弱點生命週期變遷、指紋生成、門禁判定與領域事件發布。 |

---

## 三、 標準化映射引擎 (SARIF 2.1.0 Normalization)

SARIF 是由 OASIS 制定的靜態分析標準 JSON 格式。`SarifReportParserAdapter` 依據以下規則將異質工具數據標準化：

### 3.1 工具與弱點類別對齊 (Category Mapping)
系統依據報告中的 `tool.driver.name`、`rule.id`、`tags` 與詳細說明自動歸納為領域的 `FindingCategory`：

| 來源工具特徵與規則特徵 | 領域弱點分類 (`FindingCategory`) | 說明 |
| :--- | :--- | :--- |
| 工具名稱包含 `gitleaks`，或規則包含 `secret`, `private-key`, `password`, `token`, `credential`, `api_key` | **`SECRET_LEAK`** | 原始碼硬編碼之密鑰、API 金鑰或私鑰等憑證洩漏。 |
| 工具名稱包含 `trivy`，或規則代碼以 `CVE-` 或 `GHSA-` 開頭 | **`SCA`** | 第三方開源相依套件庫之已知公開漏洞。 |
| 規則名稱或描述包含 `injection`, `xss`, `rce`, `csrf`, `vulnerability` | **`VULNERABILITY`** | 原始碼本身存在之安全弱點。 |
| 規則描述包含 `bug`, `nullpointer`, `overflow` | **`BUG`** | 程式碼邏輯缺陷或潛在運行時期崩潰錯誤。 |
| 其他代碼品質、風格、未使用引入等問題 | **`CODE_SMELL`** | 影響可維護性與可讀性之代碼壞味道。 |

### 3.2 嚴重等級標準化 (Severity Normalization)
異質工具對嚴重等級的定義五花八門，系統依據以下**兩層判定優先序**將其對齊至五級標準：

```
[ 優先級 1: 檢查 security-severity (CVSS 數值) ]
  ├── CVSS >= 9.0  ────────► CRITICAL (重大危險)
  ├── CVSS >= 7.0  ────────► HIGH (高危險)
  ├── CVSS >= 4.0  ────────► MEDIUM (中危險)
  └── CVSS > 0.0   ────────► LOW (低危險)
         │ (若無 CVSS 數值)
         ▼
[ 優先級 2: 檢查 SARIF level 欄位與工具特性 ]
  ├── level == "error"
  │     ├── 若工具為 Gitleaks  ──► CRITICAL (密鑰洩漏零容忍)
  │     └── 其他工具            ──► HIGH
  ├── level == "warning"        ──► MEDIUM
  ├── level == "note"           ──► LOW
  └── level == "none"           ──► INFO
```

---

## 四、 弱點生命週期跨輪次對齊與多工具隔離機制

### 4.1 生命週期狀態轉換 (Finding Lifecycle)
每次上傳時，系統透過 `findActiveByProjectAndBranch(projectId, branch)` 撈出目前處於未修復狀態（`OPEN`, `CONFIRMED`）的既有弱點，並透過 `FindingFingerprint` 進行對齊：

```
       [ 本次掃描傳入弱點 (Incoming) ]
                     │
        ┌────────────┴────────────┐
        ▼                         ▼
   庫內已存在指紋             庫內不存在指紋
        │                         │
        ▼                         ▼
 呼叫 touchScan(...)       呼叫 Finding.createNew(...)
 刷新最後偵測 Commit        狀態設為 OPEN
 同步更新 Severity          累積 FindingIngestedEvent
 累計 existingCount++       累計 newCount++
```

對於目前庫存中**同工具**但本次掃描**未再出現**的弱點：
- 判定開發者已在本次 Commit 成功修復該弱點。
- 呼叫 `markAsResolved(commitHash, scanTimestamp)`，狀態轉移為 `RESOLVED`。
- 累計 `resolvedCount++`，並發布 `FindingStatusChangedEvent`。

### 4.2 多工具同輪次隔離保護 (Tool Isolation)
在同一次發版或同一次 Commit 中，CI/CD Pipeline 通常會接續上傳 Trivy 與 Gitleaks 報告：
- 若不加隔離，Trivy 報告中自然不會有 Gitleaks 的密鑰弱點，系統若全量比對將會誤把 Gitleaks 的弱點全部標記為「已修復（RESOLVED）」。
- **設計解決方案**：
  ```java
  // 僅比對同工具類別之弱點，杜絕跨工具誤判修復
  Map<FindingFingerprint, Finding> activeMap = activeDbFindings.stream()
          .filter(f -> isSameTool(f.getToolInfo(), toolInfo))
          .collect(Collectors.toMap(Finding::getFingerprint, f -> f, (e1, e2) -> e1));
  ```

---

## 五、 等冪性與併發安全 (Idempotency & Concurrency)

1. **外部任務快取回傳 (Fast Return)**：
   - 上傳時可帶入 `externalTaskId`（例如 CI Pipeline ID 或 Job ID）。
   - 若資料庫中已存在該 `externalTaskId` 且狀態為 `SUCCESS`，Service 會直接回傳歷史結算結果，不重複解析檔案、不重複寫入資料庫、不重複發布領域事件。
2. **Redisson 分散式鎖**：
   - 鎖 Key 設計：`lock:scan:upload:{projectId}:{branch}:{toolName}`（若有 externalTaskId 則為 `lock:scan:upload:task:{externalTaskId}`）。
   - 租期 60 秒，確保網路抖動引發的 CI 重試或高併發情境下，同專案同工具之結算過程完全互斥。

---

## 六、 REST API 規格說明

### 6.1 端點資訊
* **URL**：`POST /api/v1/scans/upload`
* **Content-Type**：`multipart/form-data`
* **Produce**：`application/json`

### 6.2 請求參數 (Form Parameters)

| 參數名稱 | 類型 | 必填 | 說明與範例 |
| :--- | :--- | :---: | :--- |
| **`file`** | File (Binary) | **是** | 掃描報告檔案（支援 `.sarif` 或 `.json` 格式，上限支援數十 MB）。 |
| **`projectId`** | String | **是** | 專案代碼（例如：`payment-service`）。 |
| **`branch`** | String | 否 | 掃描分支名稱（預設為 `main`）。 |
| **`commitHash`** | String | 否 | 對應之 Git Commit 雜湊碼（例如：`a1b2c3d4e5f6...`）。 |
| **`toolName`** | String | 否 | 掃描工具名稱（例如 `Trivy`，若未指定則自動從 SARIF `tool.driver.name` 提取）。 |
| **`externalTaskId`**| String | 否 | 外部任務編號或 CI Pipeline ID（例如：`gitlab-ci-pipeline-1234`，用於等冪防重放）。 |

### 6.3 回應範例 (Response Body - 200 OK)
```json
{
  "scanExecutionId": "7d5e4a3b-2c1f-4e89-a012-3456789abcde",
  "projectId": "payment-service",
  "branch": "main",
  "commitHash": "a1b2c3d4e5f67890123456789abcdef012345678",
  "toolName": "Trivy",
  "status": "SUCCESS",
  "passedQualityGate": true,
  "totalActiveCount": 10,
  "newCount": 2,
  "existingCount": 8,
  "resolvedCount": 1,
  "criticalCount": 0,
  "highCount": 1,
  "completedAt": "2026-10-15T00:30:00Z"
}
```

### 6.4 HTTP 狀態碼
- **`200 OK`**：報告解析、生命週期對齊與品質門禁結算成功。
- **`400 Bad Request`**：未提供檔案、檔案內容為空、或缺少必要之 `projectId`。
- **`409 Conflict`**：相同任務正在處理中未能取得分散式鎖。

---

## 七、 CI/CD Pipeline 整合實踐 (Integration Playbook)

### 7.1 GitLab CI 整合範例 (`.gitlab-ci.yml`)

以下為在 GitLab CI 中同時執行 **Trivy (SCA)** 與 **Gitleaks (Secret)**，並將 SARIF 報告自動推送至 SCS Facade 的完整範例：

```yaml
stages:
  - test
  - security-scan

variables:
  SCS_FACADE_URL: "http://scs-facade:8080"

# 1. Trivy 依賴弱點掃描與上傳
trivy-scan:
  stage: security-scan
  image:
    name: aquasec/trivy:latest
    entrypoint: [""]
  script:
    - trivy fs --format sarif -o trivy-report.sarif .
    - >-
      curl -X POST
      -F "file=@trivy-report.sarif"
      -F "projectId=${CI_PROJECT_NAME}"
      -F "branch=${CI_COMMIT_REF_NAME}"
      -F "commitHash=${CI_COMMIT_SHA}"
      -F "toolName=Trivy"
      -F "externalTaskId=gitlab-trivy-${CI_PIPELINE_ID}"
      "${SCS_FACADE_URL}/api/v1/scans/upload"

# 2. Gitleaks 密鑰洩漏掃描與上傳
gitleaks-scan:
  stage: security-scan
  image:
    name: zricethezav/gitleaks:latest
    entrypoint: [""]
  script:
    - gitleaks dir --report-format sarif --report-path gitleaks-report.sarif .
    - >-
      curl -X POST
      -F "file=@gitleaks-report.sarif"
      -F "projectId=${CI_PROJECT_NAME}"
      -F "branch=${CI_COMMIT_REF_NAME}"
      -F "commitHash=${CI_COMMIT_SHA}"
      -F "toolName=gitleaks"
      -F "externalTaskId=gitlab-gitleaks-${CI_PIPELINE_ID}"
      "${SCS_FACADE_URL}/api/v1/scans/upload"
```

### 7.2 本機 cURL 測試範例

```bash
curl -X POST \
     -F "file=@sample-trivy.sarif" \
     -F "projectId=demo-project" \
     -F "branch=dev" \
     -F "commitHash=8f3b21a" \
     -F "toolName=Trivy" \
     "http://localhost:8080/api/v1/scans/upload"
```

---

## 八、 測試覆蓋率與驗證總結

本模組遵循嚴謹的測試驅動原則，共計新增 **14 項全覆蓋單元測試**，涵蓋：
1. **`SarifReportParserAdapterTest`**：測試 Trivy SCA、Gitleaks Secret 洩漏、Semgrep SAST、CVSS 評分判定、空報告與異常檔案。
2. **`UploadScanReportCommandServiceTest`**：測試新弱點建立、既有弱點更新、已修復弱點判定、多工具隔離、等冪性 fast return 與鎖競爭阻擋。
3. **`ScanReportResourceAssemblerTest`**：測試表現層與應用層雙向防腐轉換。
4. **`ScanReportUploadControllerTest`**：測試 RESTful 端點上傳、參數綁定與空檔案拒絕。

**專案全域指標**：
- **全專案單元測試總數**：**101 / 101 passed** (100% 通過)
- **GitLab CI Pipeline**：`Pipeline #55 / Job #60` $\rightarrow$ **Job succeeded**
- **SonarQube Quality Gate**：**STATUS: OK**（New Code Coverage: 82.9%, Reliability: Rating A, Security: Rating A, Maintainability: Rating A, Duplications: 0.59%）。
