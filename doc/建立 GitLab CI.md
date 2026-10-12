# 建立 GitLab CI 與 SonarQube 整合指南

配置本機 GitLab CI/CD 包含四個階段：
1. **註冊 Runner**
2. **在 SonarQube 產生 Token**
3. **在 GitLab 設定環境變數**
4. **撰寫與推送 `.gitlab-ci.yml`**

---

## 步驟 1：註冊 GitLab Runner

Runner 容器需要與 GitLab 實例配對才能接收並執行 Job。

### 1.1 取得 Runner Registration Token
1. 進入 GitLab 管理者頁面：`http://localhost:8888/admin`。
2. 點擊 **Admin Area（板手圖示） > CI/CD > Runners**（或直接在任意新建的專案進入 **Settings > CI/CD > Runners**）。
3. 建立一個新的 **Instance runner** 或 **Project runner**，取得系統產生的 `glrt-...` 註冊 Token。
   > 💡 **提示**：畫面上的 Token 已經由系統自動產生完畢，不需要再做額外挑選或配置。
   > ⚠️ **注意**：此 Token 只會在建立時暫時顯示一次，建議先複製保存。

---

### 補充：Tag 標籤機制說明
在 GitLab CI/CD 中，Tag 主要用來指定「哪一台 Runner 負責執行哪一個 Job」，其核心差異在於工作派送的精準度與彈性：

#### (1) 寫 Tag（指定標籤）
* **運作機制**：當在 Runner 上設定了標籤（例如 `docker`、`gpu`、`k8s`），在 `.gitlab-ci.yml` 中宣告相同 `tags` 的 Job，就只會被擁有該標籤的 Runner 接收。
* **適用情境**：
  * **硬體 / 環境隔離**：某些 Job 需要特殊環境（如 iOS 編譯需要 macOS 實體機、AI 運算需要 GPU 機器、特定部署需要內部網路存取權限）。
  * **正式環境部署隔離**：將 Production 部署 Job 標記 `prod-deployer`，僅交給具有生產環境密鑰的高權限 Runner。

```yaml
build-job:
  stage: build
  tags:
    - docker    # 只有帶有 "docker" 標籤的 Runner 才會抓這個 Job
  script:
    - docker build -t my-app .
```

#### (2) 不寫 Tag（留空 / 搭配 Run untagged jobs）
* **運作機制**：當 Runner 勾選了「**Run untagged jobs**」，它會自動接收任何沒有宣告 `tags` 屬性的 Job。專案內所有普通的 Job 都可以共用這台 Runner。
* **適用情境**：
  * **本機開發與測試（目前的狀況）**：只有單一 Runner，且不需要區分不同主機環境，所有測試、靜態分析、編譯都在同一台 Docker 上跑。
  * **通用的共用池（Shared Pool）**：一般的單元測試或程式碼排版檢查，不需要綁定特定機器。

---

### 1.2 在終端機註冊 Runner
因為 GitLab Runner 是以 Docker 容器方式運行，**不能直接在 Windows 終端機照抄畫面上的命令**（因為畫面的 URL 寫著 `localhost:8888`，容器內無法透過 `localhost` 找到宿主機的 GitLab）。

執行以下指令（請將 `<YOUR_TOKEN>` 替換為剛才取得的 Token）：

```bash
docker exec -it gitlabci-runner-1 gitlab-runner register `
  --non-interactive `
  --url "http://web:8888" `
  --token "<YOUR_TOKEN>" `
  --executor "docker" `
  --docker-image "docker:latest" `
  --docker-network-mode "gitlabci_devops-net"
```

> ⚠️ **注意事項**：
> * `--url` 請填入 Docker 容器內網路名稱 `http://web:8888`，而非 `localhost`。
> * `--docker-network-mode` 請指定為 Compose 建立的網路名稱（可用 `docker network ls` 確認，通常為 `<目錄名>_devops-net` 或自訂網路名稱），這樣 CI 啟動的測試容器才能連到 SonarQube。

**執行範例**：
```bash
docker exec -it gitlabci-runner-1 gitlab-runner register `
  --non-interactive `
  --url "http://web:8888" `
  --token "glrt-dcTcpDOCrkoVZ7wPsjfg4m86MQp0OjEKdToxCw.01.120t10yq1" `
  --docker-image "docker:latest" `
  --docker-network-mode "gitlabci_devops-net" `
  --executor "docker"
```

### 1.3 驗證方式
回到 GitLab 網頁的 **Runners 清單**，確認該 Runner 狀態為綠色圓點（**Online**）。

---

## 步驟 2：為 Project 配置 SonarQube

SonarQube 需要一把具備 `api` 權限的 GitLab Token 才能與 GitLab 進行雙向溝通。

### 2.1 到 GitLab 產生「Personal Access Token」
1. 點擊右上角個人頭像：**Profile > Access Tokens > Personal access tokens > Generate Token**。
2. 在下拉清單中點選 **Legacy token**。
3. 頁面會跳轉或展開建立 Token 的表單，填寫以下欄位：
   * **Token name**：輸入名稱，例如 `sonarqube-integration`。
   * **Expiration date**：可選擇較遠的日期或刪除日期（依需求設定）。
   * **Scopes（最關鍵）**：務必勾選 **`api`**（SonarQube 需要存取 API 的讀寫權限來讀取專案並回傳 MR 狀態）。
4. 點擊建立並保存產生的 Token。

### 2.2 為 SonarQube 配置專案
1. 進入 SonarQube 首頁（`http://localhost:9000`）。
2. 設定 GitLab 相關配置，貼上剛才產生的 GitLab Personal Access Token 並按下 **Save**。
3. 畫面會自動載入並列出在 GitLab 上建立的所有專案（Repositories）。
4. 找到目標專案（如 `scs-facade`），點擊 **「Set up」** 匯入 SonarQube。
5. 設置 **Project Key**，選擇 **Maven** 建置工具進入下一步。

### 2.3 在 SonarQube 產生 Token
可依需求選擇以下任一種 Token：

* **選項 A：產生 Project Token**
  1. 在畫面上點擊 **「Generate a token」**。
  2. 畫面會產生一串字串，點擊旁邊的複製圖示複製該 Token。
* **選項 B：建立全域 Token (Global Analysis Token，推薦)**
  > 註：此類型的 Token 擁有分析所有專案的權限，不再受特定 Project Key 限制。
  1. **Name**：輸入 `gitlab-global-token`。
  2. **Type**：下拉選單切換為 **Global Analysis Token**（或 User Token）。
  3. **Expires in**：可選 30 days 或 No expiration。
  4. 點擊 **Generate** 並複製產生的 Token。

---

## 步驟 3：在 GitLab 專案中設定 CI 變數

1. 在 GitLab 建立或進入測試專案（例如 `scs-facade`）。
2. 進入該專案左側選單的 **Settings > CI/CD > 展開 Variables**。
3. 點擊 **Add variable** 新增以下三組變數：

| 變數名稱 (Key) | 建議值 (Value) | Protect 設定 | Mask 設定 | 說明 |
| :--- | :--- | :---: | :---: | :--- |
| `SONAR_HOST_URL` | `http://sonarqube:9000` | 取消勾選 | 否 | SonarQube 伺服器容器內網位址 |
| `SONAR_TOKEN` | `<剛才產生的 SonarQube Token>` | 取消勾選 | **勾選** | 分析認證密鑰 |
| `CI_SERVER_URL` | `http://web:8888` | 取消勾選 | 否 | GitLab 內網位址 |

> 💡 **注意**：建議取消勾選 **"Protect variable"**，以確保在非 protected 的分支（如 `dev` 分支）執行 CI 時亦能正常讀取該變數。

4. 驗證方式：Variables 清單中可見 `CI_SERVER_URL`、`SONAR_HOST_URL` 與 `SONAR_TOKEN`。
5. 回到 SonarQube 分頁，點擊左下角的 **Continue** 按鈕完成配置。

---

## 步驟 4：建立 `.gitlab-ci.yml`

在專案根目錄建立 `.gitlab-ci.yml` 檔案。以下提供兩種常見情境的範本：

* **情境 A**：通用專案（使用 Sonar Scanner CLI 掃描任何語言）$\rightarrow$ 請參考 [附錄 A](#附錄-a通用專案範本-sonar-scanner-cli)
* **情境 B**：Java / Maven 專案（直接整合 Sonar Maven Plugin）$\rightarrow$ 請參考 [附錄 B](#附錄-bjava--maven-專案範本-sonar-maven-plugin)

---

## 步驟 5：建立 dev 分支並設為預設分支

### 5.1 建立 dev 分支
1. 開啟 GitLab 專案頁面（`http://localhost:8888/mygroup/scs-facade`）。
2. 在分支選單旁邊點擊 **「+」** 按鈕 > 選擇 **New branch**。
3. **Branch name** 輸入 `dev`，**Create from** 選擇 `main`。
4. 點擊 **Create branch**。
5. 驗證方式：專案分支清單中出現 `dev` 分支。

### 5.2 設置 dev 為預設主分支 (Default Branch)
1. 進入 GitLab 專案頁面。
2. 點擊左側選單最下方的 **Settings > Repository**。
3. 展開 **Branch defaults** 區塊。
4. 在 **Default branch** 下拉選單中，將預設的 `main`（或 `master`）切換選為 `dev`。
5. 點擊 **Save changes**。

### 5.3 本地端切換分支並推送 `.gitlab-ci.yml`
開啟終端機（PowerShell 或 Bash）進入專案根目錄，執行以下指令：

```bash
# 1. 執行 Clone 指令（指定 dev 分支）
git clone -b dev http://localhost:8888/mygroup/scs-facade.git

# 2. 進入專案目錄
cd scs-facade

# 3. 檢查當前所在分支
git branch

# 4. 查看 git 狀態
git status

# 5. 將檔案加入暫存區
git add .gitlab-ci.yml

# 6. 提交變更
git commit -m "ci: add gitlab-ci configuration"

# 7. 推送到 GitLab 的 dev 主分支
git push origin dev
```

---

## 步驟 6：觸發並驗證 Pipeline

1. 進入 GitLab 專案的 **Build > Pipelines**，觀察 Pipeline 執行過程。
2. **驗證方式**：
   * GitLab Pipeline 顯示綠色勾勾（**Passed**）。
   * 打開 `http://localhost:9000`，首頁即可看到該專案的程式碼品質分析報告與 **Quality Gate** 狀態。

---

## 步驟 7：設定 SonarQube Webhook 接回 scs-facade (自動化弱點匯入)

當 GitLab CI 執行完 Maven 靜態掃描後，SonarQube 會在背景處理分析報告。分析完成後，SonarQube 能透過 **Webhook** 自動將分析結果事件回傳給 `scs-facade`，由後者自動調用 SonarQube OpenAPI 擷取 Issues/Hotspots 並同步為系統標準弱點模型。

### 7.1 核心連線架構須知 (Docker 網路位址)
* **SonarQube 運行於 Docker 容器** (`scs-gitlabci-sonarqube-1`)。
* **scs-facade 運行於本機 Windows 宿主機**（預設連接埠 `8080`）。
* ⚠️ **重要**：SonarQube Webhook 的 URL **不能寫 `http://localhost:8080`**，因為在容器內 `localhost` 是 SonarQube 容器自身，無法連到宿主機上的 `scs-facade`。
* **正確 URL**：
  * **當 scs-facade 在宿主機本機（IntelliJ / 本地終端）運行時**：請填寫 `http://host.docker.internal:8080/api/v1/scans/sonarqube/webhook`
  * **若未來 scs-facade 也打包為 Docker 容器並加入 `devops-net` 網路時**：請填寫 `http://scs-facade:8080/api/v1/scans/sonarqube/webhook`

### 7.2 Webhook 設定步驟 (兩種方式二選一)

#### 方式 A：針對特定專案設定 (Project Webhook，推薦)
1. 進入 SonarQube 網頁介面 (`http://localhost:9000`)。
2. 進入目標專案（如 `scs-facade`）。
3. 點選專案頂部選單的 **Project Settings > Webhooks**。
4. 點選右上角的 **Create** 按鈕。
5. 填寫以下欄位：
   * **Name**：`scs-facade-webhook`（自訂可識別名稱）。
   * **URL**：`http://host.docker.internal:8080/api/v1/scans/sonarqube/webhook`。
   * **Secret**：留空（除非有設定驗證金鑰）。
6. 點選 **Create** 儲存。

#### 方式 B：全域 Webhook (Global Webhook，所有專案皆會回呼)
1. 進入 SonarQube 網頁介面 (`http://localhost:9000`)。
2. 以管理員身分點選頂部選單 **Administration > Configuration > Webhooks**。
3. 點選右上角 **Create**。
4. 填寫相同名稱與 URL (`http://host.docker.internal:8080/api/v1/scans/sonarqube/webhook`) 並儲存。

### 7.3 驗證 Webhook 是否成功觸發
1. 啟動本機的 `scs-facade` 應用程式 (`mvn spring-boot:run` 或在 IDE 中啟動)。
2. 推送程式碼觸發 GitLab CI Pipeline，或在 SonarQube 上手動觸發一次分析。
3. 分析完成後，回到 SonarQube 的 **Project Settings > Webhooks** 頁面。
4. 在剛才建立的 Webhook 右側會出現 **Latest delivery** 紀錄：
   * 點選查看歷史紀錄，確認 HTTP 狀態碼為 **`200 OK`**。
   * 展開可看到 SonarQube 發送的原生 JSON 酬載以及 `scs-facade` 回應的 `SonarQubeIngestedResource` 弱點統計與結果。
5. 在 `scs-facade` 的後端 Console 日誌中，即可看到自動匯入並觸發領域事件的相關紀錄。

---

## 常見問題與調整彙總表 (Troubleshooting)

| 階段 | 錯誤訊息 / 現象 | 根本原因 | 調整方案 |
| :--- | :--- | :--- | :--- |
| **1. 網路解析** | `Failed to connect to localhost:8888 after 0 ms` | Runner 執行建置容器時，預設讀取 GitLab 給的外部網址 `localhost:8888`，而在 Docker 容器內 `localhost` 是容器自己，連不到宿主機/GitLab。且 Runner 設定檔內多個 runner 未全部配置。 | 在 Runner 容器的 `config.toml` 中，為正在執行的 Runner（如 `id = 3`）加入 `clone_url = "http://web:8888"`，強制建置容器使用 Docker 內網容器名稱 `web` 拉取代碼。 |
| **2. Maven 專案結構** | `there is no POM in this directory` | 專案根目錄尚未推送 `pom.xml`，或在 Windows 下新增檔案時副檔名被隱藏導致真實檔名為 `pom.xml.txt`。 | 在專案根目錄補上標準命名的 `pom.xml`，並透過 `git add`、`git commit`、`git push` 推送至 GitLab `dev` 分支。 |
| **3. 外掛解析** | `No plugin found for prefix 'sonar'` | Maven 預設只自動尋找 Apache 與 Codehaus 官方插件前綴，Sonar 外掛屬於 `org.sonarsource.scanner.maven`，因此無法直接用 `sonar:sonar` 解析。 | 將 Maven 執行命令中的簡寫改為完整外掛座標：`org.sonarsource.scanner.maven:sonar-maven-plugin:sonar`。 |
| **4. 應用打包** | `Execution repackage ... failed: Unable to find main class` | Spring Boot 外掛在 `verify` / `package` 階段自動觸發打包，但當前專案未含 `@SpringBootApplication` 主程式類別（或專案僅為模組庫）。 | 在 Maven 命令加入 `-Dspring-boot.repackage.skip=true`，分析靜態代碼時只需 `compile` 產生 `.class` 檔，跳過 fat JAR 打包。 |
| **5. 伺服器網址格式** | `Failed to query server version: URI with undefined scheme` | 傳入 `-Dsonar.host.url` 的變數為空值，或缺少 `http://` 協定前綴。 | 在 `.gitlab-ci.yml` 加上預設 fallback：`-Dsonar.host.url="${SONAR_HOST_URL:-http://sonarqube:9000}"`。 |
| **6. 權限認證** | `You're not authorized to run analysis` | 1. 建立的 Token 屬於 Project Token 且綁定特定 Key，但 Maven 預設使用 `groupId:artifactId` 送出導致 mismatch。<br>2. GitLab 變數勾選了 `Protect variable`，未保護分支執行時變數會變為空值。 | 1. 在 SonarQube 改產生 **Global Analysis Token** 或在命令中明確鎖定 `-Dsonar.projectKey="scs-facade"`。<br>2. GitLab CI 變數**取消勾選 Protect variable**。<br>3. 在命令中補上相容參數 `-Dsonar.login="${SONAR_TOKEN}"`。 |

---

## 附錄 A：通用專案範本 (Sonar Scanner CLI)

適用於各類多語言或非 Maven 專案：

```yaml
stages:
  - test
  - sonarqube-check

unit-test:
  stage: test
  image: alpine:latest
  script:
    - echo "Running unit tests..."
    - echo "Tests passed!"

sonarqube-analysis:
  stage: sonarqube-check
  image:
    name: sonarsource/sonar-scanner-cli:latest
    entrypoint: [""]
  variables:
    SONAR_USER_HOME: "${CI_PROJECT_DIR}/.sonar"
    GIT_DEPTH: "0"
  cache:
    key: "${CI_JOB_NAME}"
    paths:
      - .sonar/cache
  script:
    - >-
      sonar-scanner
      -Dsonar.host.url="${SONAR_HOST_URL}"
      -Dsonar.token="${SONAR_TOKEN}"
      -Dsonar.projectKey="${CI_PROJECT_PATH_SLUG}"
      -Dsonar.projectName="${CI_PROJECT_NAME}"
      -Dsonar.sources="."
      -Dsonar.qualitygate.wait=true
  allow_failure: true
  only:
    - main
    - dev
```

---

## 附錄 B：Java / Maven 專案範本 (Sonar Maven Plugin)

適用於 Spring Boot / Java Maven 專案：

```yaml
# ==========================================
# 定義 CI/CD Pipeline 階段流程
# ==========================================
stages:
  - sonarqube-check # 程式碼品質與靜態檢測階段

# ==========================================
# SonarQube 分析任務定義
# ==========================================
sonarqube-analysis:
  stage: sonarqube-check # 綁定上述定義的 sonarqube-check 階段
  image: maven:3.9-eclipse-temurin-17 # 建置容器環境：Maven 3.9 與 Eclipse Temurin JDK 17

  variables:
    # 將 Sonar 快取資料目錄導向專案工作目錄下，以便 GitLab Cache 能正確打包保存
    SONAR_USER_HOME: "${CI_PROJECT_DIR}/.sonar"
    # 關閉 Git 淺層複製（Fetch full history），讓 SonarQube 可以分析 Git Blame 歷史以追蹤代碼作者
    GIT_DEPTH: "0"
    # 指定 Maven 本地倉庫路徑，使其能被 GitLab CI 快取重複使用，大幅加快後續建置速度
    MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"

  # ==========================================
  # 快取機制設定（跨 Pipeline 保留下載的依賴）
  # ==========================================
  cache:
    # 根據分支名稱動態隔離快取，不同分支各自維護一份快取
    key: "${CI_COMMIT_REF_SLUG}"
    paths:
      - .sonar/cache # 快取 SonarQube 下載的分析插件與規則包
      - .m2/repository # 快取 Maven 下載的 Jar 依賴套件

  # ==========================================
  # 實際執行的建置與掃描指令
  # ==========================================
  script:
    - >-
      mvn clean compile
      org.sonarsource.scanner.maven:sonar-maven-plugin:sonar
      -Dspring-boot.repackage.skip=true
      -Dsonar.host.url="${SONAR_HOST_URL:-http://sonarqube:9000}"
      -Dsonar.token="${SONAR_TOKEN}"
      -Dsonar.login="${SONAR_TOKEN}"
      -Dsonar.projectKey="scs-facade"
      -Dsonar.qualitygate.wait=true
      # 說明：
      # 1. mvn clean compile: 清理並重新編譯原始碼產出 .class 檔供靜態分析使用
      # 2. org.sonarsource.scanner.maven:sonar-maven-plugin:sonar: 呼叫 Sonar 官方外掛（使用完整座標以防 prefix 找不到）
      # 3. -Dspring-boot.repackage.skip=true: 跳過 Spring Boot 打包以避免缺少 Main Class 導致 repackage 報錯
      # 4. -Dsonar.host.url: SonarQube 伺服器位址，若未在 GitLab 變數設定則預設使用 http://sonarqube:9000
      # 5. -Dsonar.token / -Dsonar.login: 傳入認證 Token（同時指定確保不同 scanner 版本相容性）
      # 6. -Dsonar.projectKey="scs-facade": 明確指定與 SonarQube 介面一致的專案 Key，避免名稱不符遭拒
      # 7. -Dsonar.qualitygate.wait=true: 等待 SonarQube Quality Gate 判定結果回傳後才結束 Job

  # 允許此任務失敗：若 Quality Gate 未通過，Pipeline 呈現黃色 Warning 狀態而非中斷阻擋（紅燈）
  allow_failure: true

  # ==========================================
  # 觸發條件
  # ==========================================
  only:
    - main # 僅在推送至 main 分支時觸發
    - dev  # 僅在推送至 dev 分支時觸發
```
