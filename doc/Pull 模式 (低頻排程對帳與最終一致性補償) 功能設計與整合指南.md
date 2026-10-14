# Pull 模式 (低頻排程對帳與最終一致性補償) 功能設計與整合指南

---

## 1. 模組背景與架構定位 (Background & Architecture Positioning)

在源碼掃描整合門面系統（SCS Facade）中，我們確立了「**雙軌制架構 (Dual-Track Architecture)**」的架構定位：

```
 ┌────────────────────────────────────────────────────────────────────────┐
 │ 【軌道一：即時主力 (Primary - Realtime)】                              │
 │   - Webhook 即時推播 / SARIF Push 上傳 (秒級反應、Commit 精確綁定)      │
 └───────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
                     ┌──────────────────────────────┐
                     │   SCS Facade 核心領域資料庫   │
                     │   (Finding & ScanExecution)  │
                     └───────────────▲──────────────┘
                                     │
 ┌───────────────────────────────────┴────────────────────────────────────┐
 │ 【軌道二：對帳補償 (Secondary - Reconciliation)】                      │
 │   - 每天凌晨 (例如 02:00) 執行一次輕量 Pull 狀態對帳                    │
 │   - 專門修補：Webhook 遺失、網路斷線、人員在 SonarQube UI 手動標記修復 │
 └────────────────────────────────────────────────────────────────────────┘
```

### 1.1 為什麼不以高頻 Pull 作為即時更新的主力？
1. **缺乏即時 Commit 綁定**：SonarQube 的 Issue Search API 呈現的是專案當前最新狀態，無法精確對齊每次 CI/CD Pipeline 的 Git Commit 雜湊與快照。
2. **伺服器負載與 API 頻寬**：當納管專案達數百個時，若每隔幾分鐘輪詢一次，將引發 API 頻繁調用，造成資料庫密集查詢與網路負載。
3. **Webhook 的即時優勢**：在代碼掃描結束後由工具主動發送推播，延遲低至秒級且精準攜帶當次掃描的分析 Task ID。

因此，**Pull 模式定位為「低頻對帳機制與最終一致性補償」**，與 Webhook/Push 形成黃金互補組合。

---

## 2. 差異對帳演算法 (Reconciliation Diff Engine)

對帳引擎核心邏輯在於比對 **「SonarQube 伺服器最新未解決弱點」** 與 **「本地 Facade 資料庫目前活躍弱點」** 之差異：

```
 外部伺服器未修復清單 (Remote Active)  VS  本地庫存活躍弱點 (Local Active)
                      │
   ┌──────────────────┼──────────────────┐
   ▼                                     ▼
【情境 A：本地存在，遠端已消失】       【情境 B：遠端存在，本地未收錄】
   - 開發者在 SonarQube UI 手動設為        - 當時 Webhook 傳輸失敗或網路斷線
     "Won't Fix" 或已標記修復              - 處置動作：自動補錄弱點入庫
   - 處置動作：標記本地為 RESOLVED           標記 CommitHash 為 "RECONCILED"
```

### 2.1 詳細差異處置規格

| 差異場景 | 條件特徵 | 補償動作 (Action) | 發布領域事件 |
| :--- | :--- | :--- | :--- |
| **情境 A：遠端已手動修復/關閉** | 本地處於 `OPEN`，但遠端已無此 Issue。 | 呼叫 `existing.markAsResolved("RECONCILED", Instant.now())`，狀態轉為 `RESOLVED`。 | `FindingStatusChangedEvent` |
| **情境 B：遠端遺漏補登入庫** | 遠端有此 Issue，但本地從無此指紋紀錄。 | 呼叫 `Finding.createNew(...)` 補入庫，Commit 記錄為 `"RECONCILED"`。 | `FindingIngestedEvent` |
| **情境 C：嚴重度外部異動** | 雙方皆存在，但遠端 Severity 在管理介面被調整。 | 呼叫 `existing.adjustSeverity(remoteSeverity)` 同步嚴重等級。 | 屬性刷新 |
| **情境 D：狀態完全吻合** | 雙方皆存在且等級一致。 | 僅刷新 `touchScan("RECONCILED", Instant.now())`。 | 無 |

---

## 3. 防護機制與生產級設計 (Production Protections)

### 3.1 多工具隔離保護 (Tool Isolation)
在 SCS Facade 中，同一個專案可能同時接收來自 **Trivy (SCA/Container)**、**Gitleaks (Secrets)**、**Semgrep (SAST)** 與 **SonarQube (SAST/Code Quality)** 的多源弱點。
- **隔離策略**：對帳比對本地活躍弱點時，**嚴格過濾 `f.getToolInfo().getName().equalsIgnoreCase("SonarQube")`**。
- **避免誤傷**：絕不將 Trivy、Gitleaks 等其他工具的活躍弱點誤判為「遠端已消失」而錯誤標記為修復。

### 3.2 雙層分散式鎖防併發 (Redisson Dual Locks)
1. **全域排程鎖 (`lock:reconciliation:global`)**：
   - 多節點叢集部署時，確保同一時間只有一個節點能取得排程執行權，其餘節點自動略過。
2. **專案級對帳鎖 (`lock:scan:reconcile:{projectId}:{branch}`)**：
   - 每個專案執行對帳時獨立加鎖（Lease Time 30 秒），防止與該專案同時進行中的 Webhook 或 SARIF 上傳產生寫入衝突。

### 3.3 平滑流量控制 (Pacing / Rate Limiting)
- 當納管專案達數十或上百個時，排程迴圈在每處理完一個專案後加入 **100 毫秒** 的平滑暫停（Pacing），徹底避免在凌晨對 SonarQube 伺服器造成突發性高負載或被 API Rate Limit 封鎖。

---

## 4. 領域驅動與六角形架構實作 (DDD & Hexagonal Architecture)

嚴格遵守 `.agents/AGENTS.md` 之 9 大類別命名規範與分層隔離：

```
src/main/java/com/example/scsfacade/
├── domain/scanexecution/aggregate/vo/
│   └── ScanTriggerType.java                    # 擴充 RECONCILIATION 觸發類型
│
├── application/
│   ├── command/
│   │   ├── ReconcileProjectScanCommand.java    # [3-4. Command] 單專案對帳命令
│   │   └── ReconcileAllProjectsCommand.java    # [3-4. Command] 全專案批次對帳命令
│   ├── dto/
│   │   └── ScanReconciliationGottenResult.java # [3-6. Dto] 純資料載體
│   ├── port/
│   │   ├── in/
│   │   │   ├── ReconcileProjectScanUseCase.java# [3-8. Port] Inbound Port 合約
│   │   │   └── ReconcileAllProjectsUseCase.java# [3-8. Port] Inbound Port 合約
│   │   └── out/
│   │       └── ProjectDiscoveryPort.java       # [3-8. Port] Outbound Port 合約
│   └── service/
│       ├── ReconcileProjectScanCommandService.java # [3-9. Adapter In] 單專案 Diff 引擎實作
│       └── ReconcileAllProjectsCommandService.java # [3-9. Adapter In] 批次調度與流控服務
│
├── infrastructure/
│   ├── scheduler/
│   │   └── ScanReconciliationScheduler.java   # Spring @Scheduled 排程 Worker
│   └── adapter/
│       └── ConfigProjectDiscoveryAdapter.java  # [3-9. Adapter Out] 讀取環境組態納管專案
│
├── presentation/
│   ├── resource/
│   │   ├── in/ReconcileProjectScanResource.java   # [3-1. Request] 純資料載體
│   │   └── out/ScanReconciledResource.java        # [3-2. Response] 純資料載體
│   ├── assembler/
│   │   └── ScanReconciliationResourceAssembler.java # 防腐層組裝器 (ACL)
│   └── controller/
│       └── ScanReconciliationController.java  # REST API 控制器 (手動/隨選觸發)
│
└── config/
    └── SchedulingConfiguration.java            # @EnableScheduling 全域排程配置
```

---

## 5. API 介面與排程組態使用說明

### 5.1 手動發起單一專案對帳
- **端點**：`POST /api/v1/scans/reconcile`
- **Content-Type**：`application/json`
- **請求範例**：
  ```bash
  curl -X POST "http://localhost:8080/api/v1/scans/reconcile" \
       -H "Content-Type: application/json" \
       -d '{
             "projectId": "payment-service",
             "branch": "main",
             "forceSync": false
           }'
  ```
- **回應範例 (`ScanReconciledResource`)**：
  ```json
  {
    "projectId": "payment-service",
    "branch": "main",
    "toolName": "SonarQube",
    "remoteActiveCount": 15,
    "localActiveCount": 16,
    "reconciledResolvedCount": 2,
    "reconciledAppendedCount": 1,
    "severityAdjustedCount": 0,
    "passedQualityGate": true,
    "executedAt": "2026-10-15T02:00:15Z"
  }
  ```

### 5.2 手動發起全專案批次對帳
- **端點**：`POST /api/v1/scans/reconcile-all?forceSync=false`
- **回應範例**：回傳所有納管專案之對帳結算陣列清單 (`List<ScanReconciledResource>`)。

### 5.3 自動定時排程組態 (`application.yml`)
可透過環境變數或組態檔調整排程週期與納管專案代碼：
```yaml
scs:
  reconciliation:
    enabled: ${SCS_RECONCILIATION_ENABLED:true}      # 是否啟用定時對帳排程 (預設 true)
    cron: ${SCS_RECONCILIATION_CRON:0 0 2 * * ?}     # Cron 表達式 (預設每日凌晨 02:00)
    managed-projects: ${SCS_MANAGED_PROJECTS:scs-facade,payment-service,order-service} # 納管專案代碼清單
```

---

## 6. 品質門檻與測試驗證 (Verification Results)

在本地與 GitLab CI Pipeline #58 驗證中，所有項目皆以最高標準通過：

- **單元測試套件**：**123 項測試全數通過 (BUILD SUCCESS, 0 Failures, 0 Errors)**。
- **BDD Gherkin 覆蓋**：所有 Test Method 皆具備 Feature, Scenario, Given-When-Then 完整文件註解。
- **SonarQube Quality Gate**：狀態 **`OK`**。
  - 新增代碼覆蓋率：**`84.2%`**（門檻值 $\ge 80.0\%$）。
  - 可靠度等級 (Reliability)：**Rating A** (0 Bugs)。
  - 安全性等級 (Security)：**Rating A** (0 Vulnerabilities)。
  - 可維護性等級 (Maintainability)：**Rating A** (0 Code Smells)。
  - 代碼重複率：**`0.4957%`**（門檻值 $< 3\%$）。
  - Clean As You Code 狀態：**`compliant`**。
