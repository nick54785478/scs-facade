# 自建 GitLab 容器首次登入與初始設定指南

本指南說明如何在本地 Docker 環境下完成 **GitLab CE (`scs-gitlabci-web-1`)** 的首次管理員登入、密碼變更及關鍵安全性配置。

---

## 1. 登入基本資訊與密碼取得

### 1.1 服務連線網址
* **Web 介面網址**：[http://localhost:8888](http://localhost:8888)
* **預設最高管理員帳號**：`root`

### 1.2 查詢初始密碼
GitLab 容器首次啟動時會自動產生一組高強度初始密碼，並保存在容器內的 `/etc/gitlab/initial_root_password` 檔案中。

在終端機（PowerShell 或 CMD）執行以下指令即可查閱密碼：
```bash
docker exec scs-gitlabci-web-1 cat /etc/gitlab/initial_root_password
```

輸出範例：
```text
Password: 399OmqT7Bzvna9ma1f2Lhd/Etq/pCJJKGJ1iPjGsarU=
```

> ⚠️ **重要注意事項**：
> * 該初始密碼檔案會在 GitLab 容器首次啟動 **24 小時後自動被系統刪除**！
> * 請務必在首次登入後**立即修改密碼**。

---

## 2. 首次登入操作流程

1. 開啟瀏覽器，造訪 [http://localhost:8888](http://localhost:8888)。
2. 在登入表單輸入：
   * **Username or email**：`root`
   * **Password**：貼上剛才查詢到的初始密碼（如 `399OmqT7Bzvna9ma1f2Lhd/Etq/pCJJKGJ1iPjGsarU=`）
3. 點擊 **Sign in** 登入系統。

---

## 3. 首次登入「必做」核心設定

### 步驟一：修改 Root 管理員密碼 (強烈建議)
將預設的隨機字串改為您自訂好記的密碼：

1. 點擊畫面右上角個人頭像 $\rightarrow$ 點選 **Preferences**（或 **Edit profile**）。
2. 在左側導覽列中點選 **Password**。
3. 欄位填寫說明：
   * **Current password**：輸入目前的初始密碼。
   * **New password**：輸入新密碼（至少 8 碼）。
   * **Password confirmation**：再次確認新密碼。
4. 點擊 **Save password** 儲存。
5. 儲存成功後，系統會要求您使用新密碼重新登入一次。

> 💡 **進階技巧：如果忘記密碼或檔案已被刪除怎麼辦？**
> 任何時候都可以透過 Docker 終端機強制重設 root 密碼（不需舊密碼）：
> ```bash
> docker exec -it scs-gitlabci-web-1 gitlab-rake "gitlab:password:reset[root]"
> ```
> 輸入新密碼並確認即可立即生效。

---

### 步驟二：關閉公開註冊功能 (防止未授權帳號建立)
自建 GitLab 預設可能會開放任何人皆可自由註冊新帳號，建議將其關閉以確保安全：

1. 點擊頂部或左側選單的 **Admin Area**（板手圖示 $\rightarrow$ 或造訪 `http://localhost:8888/admin`）。
2. 在左側選單進入 **Settings $\rightarrow$ General**。
3. 展開 **Sign-up restrictions** 區塊。
4. **取消勾選** `Sign-up enabled`。
5. 滾動至該區塊最下方，點擊 **Save changes** 儲存。

---

### 步驟三：建立第一個群組 (Group) 與專案 (Project)

在 GitLab 中，良好的習慣是將專案歸類在群組下：

1. 點擊左上角 **+** 按鈕 $\rightarrow$ 選擇 **New group**。
2. 點選 **Create group**：
   * **Group name**：輸入群組名稱（例如 `mygroup` 或 `devsecops`）。
   * **Visibility level**：選擇 **Private** 或 **Internal**。
3. 點擊 **Create group**。
4. 進入剛建立的群組頁面，點擊 **New project** $\rightarrow$ **Create blank project**：
   * **Project name**：輸入 `scs-facade`。
   * **Project URL**：確認所屬群組路徑為 `mygroup / scs-facade`。
   * 取消勾選 `Initialize repository with a README`（因為我們本地已有完整的程式碼專案）。
5. 點擊 **Create project**。

---

## 4. 本地端 Git 遠端關聯與推送代碼

建立完 `scs-facade` 專案後，將本地的工作區推送到自建 GitLab：

### 4.1 方式 A：透過 HTTP 協定推播 (最簡單、免設金鑰)

開啟終端機（PowerShell），進入本專案根目錄 `d:\桌面\VibeCodingWorkSpace\scs-facade`：

```bash
# 1. 檢查目前 remote 狀態
git remote -v

# 2. 加入自建 GitLab 遠端儲存庫
git remote add gitlab http://localhost:8888/mygroup/scs-facade.git

# 3. 推送 main 分支 (系統會提示輸入 root 帳號與新密碼)
git push -u gitlab main
```

### 4.2 方式 B：透過 SSH 協定推播 (免每次輸入密碼)

因 `docker-compose.yml` 中映射的 SSH Port 為 **`2222:22`**，若使用 SSH 協定需特別注意 Port 設定：

1. 本機生成 SSH Key（若已有可跳過）：
   ```bash
   ssh-keygen -t ed25519 -C "admin@example.com"
   ```
2. 複製公鑰內容（位於 `~/.ssh/id_ed25519.pub`）。
3. 登入 GitLab $\rightarrow$ 右上角頭像 $\rightarrow$ **Preferences $\rightarrow$ SSH Keys $\rightarrow$ Add new key**，貼上並儲存。
4. 本機 Git Remote 網址需明確標註 SSH 2222 埠號：
   ```bash
   git remote add gitlab ssh://git@localhost:2222/mygroup/scs-facade.git
   git push -u gitlab main
   ```

---

## 5. 後續步驟引導

完成首次登入與專案建立後，請依序參閱以下文件完成自動化流水線配置：

1. **註冊 GitLab Runner 與設置 SonarQube**：
   * 請參閱專案文件：[建立 GitLab CI.md](file:///d:/桌面/VibeCodingWorkSpace/scs-facade/doc/建立%20GitLab%20CI.md)
2. **端到端 CI/CD 門禁與 SCS Facade 整合**：
   * 請參閱專案文件：[ci_cd_integration_architecture.md](file:///d:/桌面/VibeCodingWorkSpace/scs-facade/doc/ci_cd_integration_architecture.md)
