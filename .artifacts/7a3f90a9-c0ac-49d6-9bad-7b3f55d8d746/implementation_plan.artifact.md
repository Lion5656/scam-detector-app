# 族譜功能術語更新：集團 -> 標籤

將族譜功能（Genealogy）中的「所屬集團」術語統一改為「所屬標籤」，以符合新的產品定義。

## 使用者確認事項

> [!NOTE]
> 除了 UI 介面上的文字修改，建議同步將程式碼中的 `clusterId` 變數名稱改為 `tagId`，以保持前後端命名一致性。

## 開放性問題

1. 除了族譜介面外，掃描結果介面（`MainViewModel` 模擬數據）中提到的「詐騙集團特徵」是否也需要同步改為「詐騙標籤特徵」？

## 預計變更

### 簡報層 (Presentation Layer)

#### [MODIFY] [PhoneGenealogyModel.kt](file:///C:/Users/user/scam-detector-app/app/src/main/java/com/example/scamdetectorapp/presentation/model/PhoneGenealogyModel.kt)
- 將 `PhoneGenealogyData` 資料類別中的 `clusterId` 屬性重新命名為 `tagId`。

#### [MODIFY] [PhoneGenealogyScreen.kt](file:///C:/Users/user/scam-detector-app/app/src/main/java/com/example/scamdetectorapp/presentation/screens/detection/PhoneGenealogyScreen.kt)
- 將 UI 文字「號碼所屬集團：」修改為「號碼所屬標籤：」。
- 將模擬數據中的「無分群資料」修改為「無標籤資料」。
- 更新所有引用 `clusterId` 的程式碼為 `tagId`。

#### [OPTIONAL] [MainViewModel.kt](file:///C:/Users/user/scam-detector-app/app/src/main/java/com/example/scamdetectorapp/presentation/viewmodel/MainViewModel.kt)
- 若使用者同意，將 `偵測到號碼具備多個詐騙集團特徵` 修改為 `偵測到號碼具備多個詐騙標籤特徵`。

## 驗證計畫

### 手動驗證
1. 進入「號碼族譜」功能，確認底部文字顯示為「號碼所屬標籤：[標籤名稱]」。
2. 在無資料狀態下，確認顯示「無標籤資料」。
3. 確保專案編譯通過，無變數命名衝突。
