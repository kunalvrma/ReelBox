# Implementation Plan - Share Video Feature

Add a share button to the `ReelBox` app that allows users to share the currently playing video to other applications.

## User Review Required

> [!IMPORTANT]
> The share button will be placed in the HUD (top-right area, next to the timer) to maintain the minimalist aesthetic while ensuring it's easily accessible.

## Proposed Changes

### Assets

#### [NEW] [ic_share.xml](file:///C:/Users/NITRO/KunalV/ReelBox/app/src/main/res/drawable/ic_share.xml)
- Create a new vector drawable for the share icon.

### Logic & UI

#### [MODIFY] [MainActivity.kt](file:///C:/Users/NITRO/KunalV/ReelBox/app/src/main/java/com/reelbox/app/MainActivity.kt)
- **Implement `shareVideo`**: Add a utility function to handle the `ACTION_SEND` intent with `FLAG_GRANT_READ_URI_PERMISSION`.
- **Update `HUD`**:
    - Add `onShare: () -> Unit` parameter.
    - Add a glassmorphic `IconButton` with the share icon.
- **Update `PlayerScreen`**:
    - Pass the share logic to `HUD`.
    - Retrieve the current video URI from `dynamicPlaylist[pagerState.currentPage]`.

## Verification Plan

### Automated Tests
- N/A (UI-heavy feature, will rely on manual verification)

### Manual Verification
1.  Launch the app.
2.  Pick a folder with videos.
3.  Click the new share button in the HUD.
4.  Verify that the system share sheet appears.
5.  Select an app (e.g., Photos or a messaging app) and verify the video URI is passed correctly.
