# AGENTS.md — General Project Agent Guidelines

> **說明**：本文件定義 AI Agents 在此專案中協同開發的規範、角色分工、編程標準與執行流程。所有參與專案開發與維護的 Agent 均須嚴格遵守以下準則。

---

## 1. 專案概覽與環境 (Project Context)

* **專案名稱**：`Scam Detector App (Scam Guard)`
* **專案定位**：`開發一款具備電話、網址、簡訊檢測功能的防詐騙 App，並提供視覺化的號碼關聯族譜與 AI 助手，以科技感視覺提升使用者防詐意識。`
* **核心技術棧 (Tech Stack)**：
  * **Frontend / Mobile**：`Kotlin, Jetpack Compose, Material 3, Android SDK (API 24+)`
  * **Backend / API**：`Python (FastAPI) - 透過 Retrofit 串接`
  * **視覺與動畫**：`AGSL Shader, Lottie Compose, Canvas API`
  * **Infrastructure / CI/CD**：`Gradle (KTS), Git, JNI/NDK (C++)`

---

## 2. Agent 角色矩陣與分工 (Role Matrix)

在多 Agent 協作或不同任務場景下，各 Agent 應專注於其核心邊界：

| 角色 (Role) | 核心職責 (Core Responsibility) | 輸入與觸發 (Trigger) | 產出目標 (Deliverable) |
| :--- | :--- | :--- | :--- |
| **Architect Agent** | 系統設計、目錄結構規劃、技術選型評估 | 需求規格 (PRD)、架構重構請求 | 技術設計文件 (RFC)、架構圖、介面定義 |
| **Feature Developer** | 實作業務功能、API 端點、UI 元件 | Task Ticket、Feature Branch | 功能代碼、型別定義、基本單元測試 |
| **Reviewer & Auditor** | Code Review、資安掃描、效能與壞味道檢查 | Pull Request / Git Diff | 審查回饋、重構建議、修復補丁 |
| **QA & Test Agent** | 撰寫單元測試、整合測試、邊界案例驗證 | 功能模組、測試失敗日誌 | 測試用例 (Test Suite)、覆蓋率報告 |
| **Docs & DevOps Agent** | 維護 API 文件、更新 CHANGELOG、配置 CI/CD | 版本發布、環境配置變更 | README 更新、OpenAPI/Swagger、CI YAML |

---

## 3. 核心開發原則 (Core Operating Principles)

### 3.1 漸進式變更 (Minimal & Atomic Changes)
* **單一職責**：每次修改僅專注於解決一個特定問題或實作單一功能。
* **避免無關改動**：不得隨意重構未受影響的檔案或變更無關的程式碼格式。

### 3.2 驗證優先 (Test-Driven & Verification)
* 任何程式碼變更前，先確認現有測試能否通過。
* 交付程式碼後，**必須附帶驗證方式**（單元測試、Logcat 觀察點或 UI 截圖證明）。

### 3.3 隱私與安全防護 (Security & Privacy)
* **禁止寫死金鑰**：API Key、密碼、Token 一律透過 `local.properties` 並由 JNI 讀取，嚴禁直接 Hardcode 於 Kotlin 代碼。
* **DEBUG 判斷**：所有測試/模擬數據必須被 `if (BuildConfig.DEBUG)` 保護。

---

## 4. 程式碼規範與品質要求 (Code Standards)

### 4.1 通用原則
1. **強型別優先**：充分利用 Kotlin 的型別系統，嚴禁使用 `!!` (除非必要)，優先使用安全呼叫 `?.` 或 `requireNotNull`。
2. **錯誤處理**：所有網路請求、I/O 必須有明確的 Exception Handling (Result 封裝)，不可靜默忽略。
3. **命名一致性**：
   * 變數 / 函式：`camelCase`
   * 類別 / 型別：`PascalCase`
   * 常數 / 環境變數：`UPPER_SNAKE_CASE`
   * 檔案命名：`PascalCase` (用於 Kotlin Class)

### 4.2 專案目錄結構慣例
```text
├── app/src/main/java/com/example/scamdetectorapp/
│   ├── data/           # 遠端 API、Repository 與 Local 資料源
│   ├── domain/         # UseCase 與領域模型
│   ├── presentation/   # UI 組件、ViewModel 與畫面 (Compose)
│   └── util/           # 工具類與擴充函式
├── app/src/main/cpp/   # JNI (C++) 密鑰保護邏輯
└── app/build.gradle.kts # 依賴與編譯配置
```

---

## 5. 任務執行週期 (Task Lifecycle)

當 Agent 接收到開發或修復任務時，應依循下列四步週期執行：

1. **探索與分析 (Analyze)**
   ├── 閱讀相關檔案與上下文
   └── 定位關鍵邏輯與依賴關係
          │
          ▼
2. **提出計畫 (Plan)**
   ├── 列出預計變更的檔案清單
   └── 簡述實作思路與潛在風險
          │
          ▼
3. **實作修改 (Execute)**
   ├── 編寫符合風格規範的程式碼
   └── 同步撰寫/更新對應的測試用例
          │
          ▼
4. **驗證與總結 (Verify & Deliver)**
   ├── 執行 Gradle Sync 與 Build 確保通過
   └── 提供精簡的改動摘要與驗證指令

---

## 6. Git 與 Commit 規範 (Commit Conventions)

所有由 Agent 產生的 Commit 訊息必須遵循 Conventional Commits：
- `feat: <description>` — 新增功能
- `fix: <description>` — 修復 Bug
- `refactor: <description>` — 程式碼重構
- `test: <description>` — 新增測試
- `docs: <description>` — 文件調整
- `chore: <description>` — 建置流程、依賴更新

---

## 7. 應急與降級處理 (Error Escalation)

* 若任務需求模糊，Agent 應主動列出選項與優缺點，向人類開發者確認。
* 若 API 連續失敗，需自動切換至 `MOCK` 模式（限 DEBUG 版）並記錄 Log。

---

### 如何使用與維護：
1. **放置位置**：置於專案根目錄下的 `AGENTS.md`。
2. **客製化維護**：當專案架構有重大變動時，必須優先更新此文件。
