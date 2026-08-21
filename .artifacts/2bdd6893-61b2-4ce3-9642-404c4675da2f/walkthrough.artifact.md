# Walkthrough - Share Video Feature

I have added a share button to the `ReelBox` app, allowing users to share the currently playing video with other apps on the device.

## Changes Made

### UI Enhancements
- **HUD Update**: Added a glassmorphic share button in the top-right corner of the player screen, positioned next to the session timer.
- **Iconography**: Created a new vector drawable `ic_share.xml` for the share button.

### Core Logic
- **`shareVideo` Function**: Implemented a utility function that creates an `ACTION_SEND` intent. It includes the `FLAG_GRANT_READ_URI_PERMISSION` to ensure other apps can access the video file shared via the Storage Access Framework (SAF).
- **Integration**: Connected the `VerticalPager` state in `PlayerScreen` to the `HUD` so that clicking the share button correctly identifies and shares the `Uri` of the video currently on screen.

## Verification Results

### Automated Tests
- **Build Status**: `:app:assembleDebug` completed successfully, confirming no compilation errors or unresolved references.

### Manual Verification Steps
1.  Open the app and select a video folder.
2.  Locate the new share icon (pill-shaped glass button) in the top-right HUD.
3.  Clicking the button opens the Android System Share Sheet with the video ready to be shared.

> [!TIP]
> The share button follows the existing glassmorphic design system, ensuring it looks native to the ReelBox aesthetic.
