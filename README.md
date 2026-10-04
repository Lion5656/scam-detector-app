# Scam Guard（AntifraudApp）

一款 Android 防詐騙 App，可檢測電話、網址、簡訊與購物價格的詐騙風險，並用視覺化的號碼關聯族譜提升使用者的防詐意識。

## ✨ 專案特色

- **多管道檢測**：電話、網址、簡訊文字、商品價格四種檢測模式，統一顯示風險分數與建議。
- **號碼關聯族譜**：高風險號碼可展開衛星式關聯圖，點擊節點查看關聯原因，或以該號碼重新分析。
- **科技感視覺**：Jetpack Compose + Canvas 動畫、Lottie 動畫，深色介面。
- **金鑰保護**：API Key 由 `local.properties` 傳入 NDK (C++) 編譯，不寫在 Kotlin 程式碼中。

## 📱 主要功能

| 功能 | 說明 |
| :--- | :--- |
| 電話檢測 | 查詢黑／白名單、詐騙類型與回報次數，可直接回報可疑號碼 |
| 網址檢測 | 分析 URL 是否為釣魚或惡意網站 |
| 簡訊檢測 | AI 分析簡訊內容是否含詐騙話術 |
| 價格檢測 | 上傳商品截圖，比對市場價格判斷是否異常 |
| 號碼族譜 | 顯示高風險號碼的關聯號碼網路 |
| 分享檢測 | 從其他 App 分享文字或網址直接檢測 |
| 儀表板／歷史 | 7 日風險趨勢、詐騙類型統計與本機檢測紀錄（Room） |
| 防詐新聞 | 最新詐騙新聞與 165 闢謠資訊 |
| 設定 | 讀取聯絡人、App 管理、保護名單 |

## 🛠 技術棧

Kotlin · Jetpack Compose (Material 3) · MVVM / Clean Architecture · Retrofit + OkHttp · Coroutines + Flow · Room · Lottie · NDK (C++) / CMake · 後端 Python FastAPI

| 目錄 | 說明 |
| :--- | :--- |
| `app/src/main/cpp/` | JNI：API Key 保護 |
| `.../scamdetectorapp/data/` | Retrofit API、Repository、Room |
| `.../scamdetectorapp/domain/` | 領域模型 |
| `.../scamdetectorapp/presentation/` | Compose 畫面、ViewModel |
| `.../scamdetectorapp/util/` | 工具類 |

## 💻 使用環境

| 項目 | 版本 |
| :--- | :--- |
| Android Studio | 支援 AGP 9.2 的版本 |
| JDK | 17 |
| Gradle / AGP / Kotlin | 9.7.1 / 9.2.1 / 2.0.21 |
| Android SDK | minSdk 24（Android 7.0）、compileSdk 36 |
| NDK / CMake | 27.0.12077973 / 3.22.1（可由 SDK Manager 安裝） |

## 🚀 如何啟動

1. **取得專案**
   ```bash
   git clone <repo-url>
   ```
2. **設定 API Key**：在專案根目錄的 `local.properties`（不進版控）加入：
   ```properties
   CLOUDFLARE_API_KEY=你的_API_Key
   ```
3. **啟動後端**：先啟動 FastAPI 後端服務。
4. **建置與執行**：Android Studio 執行 *Sync Project with Gradle Files* 後按 Run，或使用指令：
   ```bash
   ./gradlew :app:installDebug
   ```

## ⚠️ 注意事項

- 初次編譯需建置 NDK，時間會稍長。
- 若出現 `UnsatisfiedLinkError` 或 API 驗證失敗，請確認 `local.properties` 的 Key 後執行 Clean & Rebuild。
