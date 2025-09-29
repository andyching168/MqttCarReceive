# MqttCarAction (車載 MQTT 自動化助理) 🚗💨

[![Kotlin 版本](https://img.shields.io/badge/Kotlin-1.9.22-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![平台](https://img.shields.io/badge/Platform-Android-brightgreen.svg)]()
[![MAD](https://img.shields.io/badge/MAD-Modern%20Android%20Development-blue.svg)]()

**MqttCarAction** 是一款專為 Android 車機（或任何 Android 設備）設計的輕量級、自動化消息處理工具。它透過 MQTT 協定，無縫接收來自您手機或其他裝置的文字或連結，並根據內容自動執行相應操作。本專案旨在為駕駛等場景提供一個“一次設定，永久運作”的無感資訊通道。

這個專案的初衷是為了替代 Tasker 腳本，旨在提供一個更穩定、更可靠且完全開源的解決方案。

<!-- 建議在此處替換成你的 App 截圖 -->
![App 畫面](https://via.placeholder.com/800x450.png?text=App+UI+截圖)

---

## ✨ 核心功能

*   **智慧動作 (Smart Actions):**
    *   **自動開啟連結:** 接收到的訊息若為 URL，將立即呼叫系統預設瀏覽器開啟。
    *   **自訂浮動視窗:** 若為一般文字，則會彈出一個半透明、可互動的浮動視窗進行顯示。
*   **專為駕駛情境優化的 UI:**
    *   浮動視窗內的文字支援**長按選取覆制**。
    *   **互動重置計時器:** 在與浮動視窗進行任何觸摸互動時（如滾動或選取文字），15 秒自動關閉的倒數計時會重置，有效避免誤觸導致視窗消失。
    *   提供“OK”按鈕，可立即關閉視窗。
*   **極致可靠 (Rock-Solid Reliability):**
    *   **開機自啟:** 車機啟動後，核心服務將自動在背景執行。
    *   **背景保活:** 作為前台服務 (Foreground Service) 運作，擁有極高的背景存活優先權。
    *   **斷線重連:** 內建 MQTT 斷線自動重連機制，確保連線不中斷。
*   **完整可配置:**
    *   提供設定頁面，可自訂 MQTT Broker 的位址、連接埠、Topic、使用者名稱和密碼。
*   **Root 優化 (選配):**
    *   對於已 Root 的設備，可將其安裝為**系統應用 (System App)**，實現終極的背景保活能力，杜絕被任何系統機制終止的可能性。

---

## 🛠️ 技術棧與架構

本專案完全采用**現代 Android 開發 (Modern Android Development, MAD)** 最佳實踐建構。

*   **語言:** [Kotlin](https://kotlinlang.org/)
*   **架構:** MVVM (Model-View-ViewModel)
*   **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
*   **非同步處理:** [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://developer.android.com/kotlin/flow)
*   **依賴注入:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
*   **注解處理:** [KSP (Kotlin Symbol Processing)](https://kotlinlang.org/docs/ksp-overview.html)
*   **資料持久化:** [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
*   **MQTT 客戶端:** [HiveMQ MQTT Client](https://github.com/hivemq/hivemq-mqtt-client) (一個現代、純 Kotlin 的 MQTT 5/3 客戶端)

---

## 🚀 如何使用

### 1. 安裝與設定

1.  從 [Releases 頁面](https://github.com/YOUR_USERNAME/YOUR_REPO/releases) 下載最新的 `app-release.apk` 檔案。
2.  將 APK 安裝到您的 Android 車機或設備上。
3.  **務必手動啟動一次 App** 以“喚醒”它，這是 Android 系統的標準要求，否則開機自啟可能不會生效。
4.  點選 **"Settings"** 按鈕，填入您的 MQTT Broker 連線資訊。
5.  點選 **"Save and Restart Service"**。
6.  在系統的**設定**中，找到本 App，並授予**“忽略電池最佳化”**和**“自啟動”**的權限，以確保其能穩定在背景運作。

### 2. 運作方式
設定完成後，App 會在背景默默運作。當您指定的 MQTT Topic 收到符合格式的訊息時，App 會自動根據內容執行動作。

**接收的訊息格式 (Payload Format):**
```json
{
  "text": "這是您分享的內容，可以是一個連結 https://www.google.com 或一般文字",
  "timestamp": 1672531200000 
}
```
*   `text`: (String) 訊息的文字內容。
*   `timestamp`: (Long) **毫秒級**的 Unix 時間戳。App 會用它來判斷訊息是否過期（預設 60 秒）。

---

## 👑 進階：安裝為系統應用 (需要 Root)

為了獲得終極的穩定性和後台保活能力，您可以將 MqttCarAction 安裝為系統應用。

1.  **安裝 Magisk:** 確保您的設備已經透過 Magisk 取得 Root 權限。
2.  **下載 Magisk 模組:**
    *   從[Releases 頁面](https://github.com/YOUR_USERNAME/YOUR_REPO/releases)中下載MqttCarActionSystemizer.zip
3.  **安裝模組:**
    *   在 Magisk Manager 中，選擇“從本機安裝”。
    *   選擇 ZIP 檔案並刷入。
4.  **重新啟動設備。**

重新啟動後，MqttCarAction 將作為系統應用運作，您會在系統應用清單中發現它的“解除安裝”按鈕是灰色的。

---

## 📜 授權

本專案在 [MIT License](LICENSE) 下授權。