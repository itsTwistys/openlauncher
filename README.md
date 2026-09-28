<div align="center">
  <img width="192" height="192" alt="Open Launcher icon" src="https://github.com/user-attachments/assets/4c5c4ddb-836d-4c59-8325-76b8c8d78bb3" />
  <h1>Open Launcher</h1>
  <p>A configurable Android home screen for aftermarket car head units.</p>
</div>

## Download the Android preview

<!-- preview-download:start -->
**[Download Open Launcher 0.0.8-preview APK](https://github.com/itsTwistys/openlauncher/releases/download/preview-c24b872/openlauncher-preview.apk)**

[Release notes and checksum](https://github.com/itsTwistys/openlauncher/releases/tag/preview-c24b872) · [Installation and testing guide](https://github.com/itsTwistys/openlauncher/blob/preview-c24b872/docs/PREVIEW.md)
<!-- preview-download:end -->

This debug-signed preview installs as **Open Launcher Preview** alongside an existing launcher. Android build and 13 unit tests passed. The map and YouTube rendering changes still need confirmation on the QUZHIDA head unit. This 0.0.8 preview comes from [PR #4](https://github.com/itsTwistys/openlauncher/pull/4); the code on `main` is still 0.0.7 until that PR is merged.

![Earlier Open Launcher dashboard screenshot; current preview layout and controls may differ](https://github.com/user-attachments/assets/a1bc63f3-2d4e-4ac0-bd56-b5d181681658)

## What is built

- **Dashboard:** 3 × 2 grid with draggable, removable, resizable cards. The resize dialog previews placement and disables sizes that do not fit. Removal offers an eight-second Undo. Save Driving, Parked, Day and Night layouts, select them from Home, and optionally switch Day/Night layouts automatically.
- **Time and display:** digital or analog clock, 12- or 24-hour time, optional seconds, compact/full date, light/dark/system/sunrise themes, text and UI scaling, accent and background options, sidebar position, and wallpaper dimming.
- **Media:** Now Playing reads an Android media session through user-granted Notification Access and offers play/pause/skip. FM/AM Radio uses a supported head-unit MCU or assigned radio app; availability and controls depend on the device. The soundboard has configurable pads.
- **YouTube:** save a specific video or live-stream URL and choose **YouTube video** from Now Playing's three-dot source menu, or add the separate YouTube card. The official embedded player loads on demand for parked use. Playback depends on the device WebView and whether the video allows embedding; it does not mirror the installed YouTube app.
- **Driving:** GPS compass, speedometer, altimeter, trip distance and elapsed time, a 0–100 km/h timer, and head-unit CPU, memory and temperature readings. These depend on available device sensors.
- **Maps and destinations:** an optional interactive OpenStreetMap GPS map with Follow, Recenter, Reload and offline/error states. When navigation runs in Google Maps or Waze **on the head unit**, the map card can display text from that app's navigation notification. Set Home/Work, choose a preferred navigation app and reuse recent destinations. No Google Maps route line or CarPlay/ZLINK route is embedded.
- **Connectivity and weather:** WiFi/mobile and Android-validated internet state, online weather, and Radar/Traffic cards that open external views. The dashboard and local controls work without WiFi; map tiles, weather and YouTube require internet.
- **Apps and recovery:** installed-app library, configurable shortcuts, first-run permission setup, and local versioned JSON settings backup/restore.

## Set up a preview

1. Install the APK and open **Open Launcher Preview**. Select it as Home when ready.
2. Grant Location Access for GPS cards. Enable **Settings > Online Map > Show Embedded Map** only if you want the online map; while visible it sends precise GPS coordinates and the viewed map area to OpenStreetMap.
3. Grant Notification Access to show media sessions and navigation notification text. Start Google Maps or Waze navigation on the head unit, then return Home for directions text.
4. For YouTube, save a specific video Share link in **Settings > YouTube**. Use Now Playing's source menu, remove the separate YouTube card if combining them, enlarge the card if prompted, and load the player while parked.

See the [preview guide](docs/PREVIEW.md) for update, permission and troubleshooting details. Draw Over Other Apps permission does not embed another app's entire window. An external YouTube floating window must be closed in that app.

## Status and development

This is a development preview. Android build and unit tests passed; browser checks used simulated map tiles and video responses. On one QUZHIDA head unit, Google Maps navigation text appeared but the embedded map remained blank and video appeared in an external floating window. Version 0.0.8 adds native WebView sizing and clipping, map diagnostics and Now Playing's YouTube mode. Confirm these rendering changes on hardware before treating them as resolved.

Use JDK 17 and Android SDK 36.1 to build from source:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Preview builds on the active fix branch publish an APK and SHA-256 checksum after the Android build and unit tests pass. Pull requests run checks without publishing an APK. [PR #4](https://github.com/itsTwistys/openlauncher/pull/4) also changes the workflow so future passing `main` builds will update this README download link automatically after merge.

This public fork does not currently include a repository-level license file. The Leaflet map library retains its own bundled license in `app/src/main/assets/map/LICENSE`.
