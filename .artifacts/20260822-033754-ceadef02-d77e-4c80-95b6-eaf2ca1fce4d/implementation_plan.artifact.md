# Dashboard Simplification Plan

This plan outlines the removal of the "Knowledge Card" tab to simplify the dashboard into a single-view "Risk Dashboard".

## Proposed Changes

### Presentation Layer

#### [DashboardScreen.kt](file:///D:/Android Studio/scam-detector-app/app/src/main/java/com/example/scamdetectorapp/presentation/screens/dashboard/DashboardScreen.kt)

- Remove `selectedTab` state and Tab row.
- Set `RiskDashboardTab` as the root content of `DashboardScreen`.
- Delete `KnowledgeCardTab`, `CoolTabButton`, `KnowledgeGridItem`, `PromoBannerCard`, `KnowledgeListItem`, `KnowledgeDetailDialog`, `QuizDialog` and related data structures.

#### [DashboardData.kt](file:///D:/Android Studio/scam-detector-app/app/src/main/java/com/example/scamdetectorapp/presentation/model/DashboardData.kt)

- (Optional cleanup) Remove `KnowledgeCard` and `Quiz` data classes if they are not used elsewhere.

## Verification Plan

### Manual Verification
1.  **UI Check**: Verify the dashboard now directly shows the risk statistics and the new trend chart without any tab buttons.
2.  **Stability**: Ensure no references to deleted components remain.
