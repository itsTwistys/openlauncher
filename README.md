<div align="center">
  <img width="192" height="192" alt="Open Launcher icon" src="https://github.com/user-attachments/assets/4c5c4ddb-836d-4c59-8325-76b8c8d78bb3" />
  <h1>Open Launcher</h1>
  <p>A configurable Android home screen for aftermarket car head units.</p>
</div>

## Download the Android preview

<!-- preview-download:start -->
**[Download Open Launcher 0.0.8-preview preview APK](https://github.com/itsTwistys/openlauncher/releases/download/preview-595a3c5/openlauncher-preview.apk)**

[Release notes and checksum](https://github.com/itsTwistys/openlauncher/releases/tag/preview-595a3c5) · [Installation and testing guide](https://github.com/itsTwistys/openlauncher/blob/preview-595a3c5/docs/PREVIEW.md)
<!-- preview-download:end -->

This debug-signed preview installs as **Open Launcher Preview** alongside an existing launcher. The download above identifies its exact release. Source version 0.0.10 focuses on map recovery, readability and a combined Clock + Weather card. It includes the YouTube removal from 0.0.9.

![Earlier Open Launcher dashboard screenshot; current preview layout and controls may differ](https://github.com/user-attachments/assets/a1bc63f3-2d4e-4ac0-bd56-b5d181681658)

## What is built

- **Dashboard:** a map-first default (2 × 2 Map, 1 × 1 Now Playing, 1 × 1 Clock + Weather) and one Edit Dashboard screen for adding, removing, resizing and restoring defaults. Existing layouts are retained except separate Weather cards merge into Clock. 3 × 2 grid with draggable, removable, resizable cards. The resize dialog previews placement and disables sizes that do not fit. Removal offers an eight-second Undo. Save Driving, Parked, Day and Night layouts, select them from Home, and optionally switch Day/Night layouts automatically.
- **Time and display:** combined Clock + Weather card with digital or analog clock, 12- or 24-hour time, optional seconds, compact/full date, light/dark/system/sunrise themes, text and UI scaling, accent and background options, sidebar position, and wallpaper dimming.
- **Media:** Now Playing reads an Android media session through user-granted Notification Access and offers play/pause/skip. FM/AM Radio uses a supported head-unit MCU or assigned radio app; availability and controls depend on the device. The soundboard has configurable pads.
- **Driving:** GPS compass, speedometer, altimeter, trip distance and elapsed time, a 0–100 km/h timer, and head-unit CPU, memory and temperature readings. These depend on available device sensors.
- **Maps and destinations:** an optional interactive OpenStreetMap GPS map with Follow, Recenter, Reload and separate GPS, tile-loading and offline/error states. Bundled assets load before GPS; bounded retries, reconnect and resume recovery handle temporary failures. When navigation runs in Google Maps or Waze **on the head unit**, the map card can display text from that app's navigation notification. Set Home/Work, choose a preferred navigation app and reuse recent destinations. No Google Maps route line or CarPlay/ZLINK route is embedded.
- **Connectivity and weather:** WiFi/mobile and Android-validated internet state, online weather, and Radar/Traffic cards that open external views. The dashboard and local controls work without WiFi; map tiles and weather require internet.
- **Apps and recovery:** installed-app library, configurable shortcuts, first-run permission setup, and local versioned JSON settings backup/restore.

## Set up a preview

1. Install the APK and open **Open Launcher Preview**. Select it as Home when ready.
2. Grant Location Access for GPS cards. Enable **Settings > Online Map > Show Embedded Map** only if you want the online map; while visible it sends precise GPS coordinates and the viewed map area to OpenStreetMap.
3. Grant Notification Access to show media sessions and navigation notification text. Start Google Maps or Waze navigation on the head unit, then return Home for directions text.

See the [preview guide](docs/PREVIEW.md) for update, permission and troubleshooting details. Draw Over Other Apps permission does not embed another app's entire window. An external YouTube floating window must be closed in that app.

## Status and development

This is a development preview. The map follows the head unit’s GPS location using OpenStreetMap tiles; it does not mirror Google Maps or import its route line. Earlier device testing showed a blank map area. The map uses Android’s local asset loader with automatic retry and resume recovery. Successful rendering and ignition/sleep behavior on the QUZHIDA unit still need confirmation. Embedded YouTube playback has been removed; Now Playing retains media-session artwork and controls.

Use JDK 17 and Android SDK 36.1 to build from source:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Passing pushes on main, feature and fix branches publish preview APKs with SHA-256 checksums. Pull requests run checks without publishing. Passing main builds update the download link above automatically.

This public fork does not currently include a repository-level license file. The Leaflet map library retains its own bundled license in `app/src/main/assets/map/LICENSE`.
