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
