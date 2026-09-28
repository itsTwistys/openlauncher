# Open Launcher 0.0.6 preview

## Install

Download **openlauncher-preview.apk** from the preview release assets. This build uses the separate package `com.openlauncher.app.preview` and appears as **Open Launcher Preview**, so your existing launcher can remain installed.

1. Open the APK on the Android head unit and allow installation from the app used to open it.
2. Launch Open Launcher Preview and grant the permissions you want its widgets to use.
3. Select it as the home app when ready. You can switch back in Android's default home-app settings.

This is a development build signed with a debug certificate. It has not been tested on a physical head unit. Back up settings before updating previews; a changed debug certificate can require reinstalling the preview app.

## New controls

- **Map:** enable Online Map in Settings, then add Map through the widget library. The marker updates without reloading the page. Dragging stops Follow; Recenter resumes it. Map tiles need internet and share the viewed area with OpenStreetMap.
- **Resize:** long-press a widget, choose Resize, and select an available size. Disabled sizes cannot fit. The grid preview shows placement before Apply.
- **Remove:** use the widget menu or library; Undo is available for eight seconds.
- **Layouts:** save Driving, Parked, Day, and Night in Settings. Switch saved layouts from the home header. Automatic Day/Night switching becomes available after both layouts are saved; it follows the display mode.
- **Navigation:** enter Home and Work, select Default, Google Maps, or Waze, then use Destinations. Successful navigation handoffs appear in the recent list.
- **Network:** the widget distinguishes the connected network from Android's verified internet state. Android 5 cannot verify internet using this API.
- **Backup:** export or restore a versioned JSON file from Settings. Destination addresses are included. Local wallpaper/audio files and their access grants are not included; assign those again after restoring. Online maps need a fresh opt-in after restore.

Radar and Traffic remain links that open external live views. ETA is displayed by the navigation app.

## Build

Use JDK 17, Android SDK 36.1, and the included Gradle wrapper:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The GitHub workflow runs the same checks and publishes a development preview only after both succeed. Leaflet 1.9.4 is bundled with its license; no remote JavaScript is loaded by the map.

## 0.0.7: overlay, navigation and YouTube

- Update the installed **Open Launcher Preview** APK. In Settings > Permissions > Draw Over Other Apps, select **Open Launcher Preview** in Android's list and allow it. The manifest now declares the missing overlay permission. The status refreshes when you return. Vendor ROMs may expose a different settings page.
- Overlay permission allows drawing the launcher's own UI above another app. It does not grant permission to embed another app's full screen. This build does not add privileged app embedding or change the head unit's system settings automatically.
- For directions, grant **Notification Access**, start a Google Maps or Waze trip on the head unit, then return Home. The Map widget displays the title and text exposed by its navigation notification, alongside the existing interactive OpenStreetMap GPS map. Text varies by app version. No route polyline is imported. Directions running only on an iPhone through CarPlay/ZLINK are not available through this integration. Navigation text remains in memory and clears when its notification is removed or access disconnects.
- In Settings > YouTube, paste a specific video/live-stream link and save it. Add **YouTube** from the widget library. Enlarge the card if prompted; the embedded player requires a minimum 200 x 200 viewport. Tap **I'm parked · Load player**, then use the YouTube play control. Channel /live links are not supported; use the stream's Share link.
- YouTube uses its official web player with app identity supplied through its referrer. Network traffic goes to YouTube only after loading the player. Unavailable, private, age-restricted or non-embeddable videos may show a YouTube error; use **Open YouTube**. **Reload** retries a failed player. This does not mirror the installed YouTube app or its login.
- Playback stops on leaving the launcher, entering edit mode, losing connectivity, or GPS detecting movement above approximately 5 km/h. GPS is not a parking-brake interlock; use video only while parked. No autoplay or background playback is added.
- Physical-device verification is still required for the ROM's permission screen, Google Maps/Waze notification fields, WebView/YouTube playback and audio behavior.
