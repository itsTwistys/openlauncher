<div align="center">
  <img width="192" height="192" alt="Open Launcher icon" src="https://github.com/user-attachments/assets/4c5c4ddb-836d-4c59-8325-76b8c8d78bb3" />
  <h1>Open Launcher</h1>
  <p>A configurable Android home screen for aftermarket car head units.</p>
</div>

## Download the Android preview

<!-- preview-download:start -->
**[Download Open Launcher 0.0.19-preview preview APK](https://github.com/itsTwistys/openlauncher/releases/download/preview-3f4a021/openlauncher-preview.apk)**

[Release notes and checksum](https://github.com/itsTwistys/openlauncher/releases/tag/preview-3f4a021) · [Installation and testing guide](https://github.com/itsTwistys/openlauncher/blob/preview-3f4a021/docs/PREVIEW.md)
<!-- preview-download:end -->

This permanently signed preview installs as **Open Launcher Preview** alongside an existing launcher. Source version 0.0.20 fixes missing GPS course with a guarded position-based fallback, retains trusted heading for up to 30 seconds in slow traffic, and avoids overlapping rotated pan animations. It includes a single theme-aware controls flyout for Wi-Fi status/settings, dashboard editing, tools and saved layouts, plus matching YouTube, YouTube Music and Waze shortcut icons. It consolidates dashboard controls into the app rail, adds a Top rail option, compact next-turn cues and map controls that preserve tile space. It also includes richer weather, icon-only media actions, automatic/manual dark maps and steadier map updates with a fixed directions banner. It includes full-card media, local-time overrides, smoother GPS markers and matching app icons. It retains the compact widget editor and adds dashboard controls, saved trips, expanded forecast, nearby stops, diagnostics, updates and layout recovery. It includes saved media-app selection, temporary card expansion, map orientation/automatic zoom, map recovery and the combined Clock + Weather card. It includes the YouTube removal from 0.0.9.

![Earlier Open Launcher dashboard screenshot; current preview layout and controls may differ](https://github.com/user-attachments/assets/a1bc63f3-2d4e-4ac0-bd56-b5d181681658)

## What is built

- **Dashboard:** a map-first default (2 × 2 Map, 1 × 1 Now Playing, 1 × 1 Clock + Weather) and a compact Edit Dashboard grid for adding, removing, resizing and restoring defaults. A proportional preview shows each spanning card once; select cards to resize or reposition, then Apply or Cancel. The previous eight layouts are saved automatically and can be previewed in Recovery. Maps and Now Playing have a one-tap Expand/collapse controls; Home also returns to the dashboard. Expansion never changes saved positions or sizes. Existing layouts are retained except separate Weather cards merge into Clock. 3 × 2 grid with draggable, removable, resizable cards. The resize dialog previews placement and disables sizes that do not fit. Long-press removal offers an eight-second Undo. Save Driving, Parked, Day and Night layouts, select them from Home, and optionally switch Day/Night layouts automatically.
- **Time and display:** combined Clock + Weather card with digital or analog clock, 12- or 24-hour time, prominent AM/PM, weather-local/system/manual timezone selection, optional seconds, compact/full date, light/dark/system/sunrise themes, text and UI scaling, accent and background options, sidebar position, and wallpaper dimming.
- **Media:** choose Automatic, FM/AM Radio, or an installed app in Now Playing; the choice survives restarts and backups. An unavailable chosen app shows a disconnected/missing-app state instead of switching to another player. Playback buttons have larger touch areas. Now Playing reads an Android media session through user-granted Notification Access and offers play/pause/skip, seeking and custom save/like/rating actions when exposed by the selected app. Artwork fills the card under one source header; compact cards retain transport buttons. FM/AM Radio uses a supported head-unit MCU or assigned radio app; availability and controls depend on the device. The soundboard has configurable pads.
- **Driving:** GPS compass, speedometer, altimeter, trip distance and elapsed time, a 0–100 km/h timer, and head-unit CPU, memory and temperature readings. These depend on available device sensors.
- **Maps and destinations:** an optional interactive OpenStreetMap GPS map with Auto/Dark/Light appearance, Follow, Recenter, Reload and separate GPS, tile-loading and offline/error states. A blue position dot and accuracy circle animate between nearby fresh fixes; stale positions are explicit. Auto zoom widens the view at higher GPS speeds, with hysteresis to avoid repeated switching. Manual zoom pauses automation until Recenter. Choose North up or Heading up; direction-of-travel rotation requires a fresh, sufficiently accurate GPS fix while moving and returns north-up otherwise. Bundled assets load before GPS; bounded retries, reconnect and resume recovery handle temporary failures. When navigation runs in Google Maps or Waze **on the head unit**, the map card displays a compact next-turn cue from that app's navigation notification. Tap it for trip details and Open navigation. Set Home/Work, choose a preferred navigation app and reuse recent destinations. No Google Maps route line or CarPlay/ZLINK route is embedded.
- **Connectivity and weather:** WiFi/mobile and Android-validated internet state, online weather, and Radar/Traffic cards that open external views. The dashboard and local controls work without WiFi; map tiles and weather require internet.
- **Dashboard controls:** tap the sliders icon for a compact flyout with connection status, Wi-Fi settings, Edit Dashboard, saved layouts and Dashboard tools. Open Dashboard tools for media volume/mute, launcher-only brightness, navigation and Wi-Fi shortcuts, nearby gas/parking/coffee/food/charging/rest-area searches, diagnostics/export and a manual GitHub update check. The update check offers release notes, APK and checksum links, never silent installation.
- **Trips and forecast:** trip totals survive restarts; save up to 200 trips and export CSV. Recording runs while the launcher is visible, excludes stale/inaccurate GPS and sleep gaps, and restores paused after a restart. Clock + Weather shows feels-like, high/low, next-hour rain and sunset when space permits. Tap for the next 12 hours, wind, humidity and sunrise. Optional weather background tint is off by default and preserves the existing dashboard styling.
- **Apps and recovery:** installed-app library, configurable shortcuts with matching Chrome/Spotify/Maps/YouTube/YouTube Music/Waze symbols and consistently sized native app icons, first-run permission setup, and local versioned JSON settings backup/restore.

## Update without repeating setup

Version 0.0.18 starts a new permanent-key channel after the old build process lost its signing key. Export your settings and trip CSV before the one-time reinstall; follow [the migration guide](docs/SIGNING-MIGRATION.md). Later builds signed with this permanent key can update the new installation without clearing its data. The app ID, home-screen appearance and settings format are unchanged. GitHub signing requires the backed-up keystore and password secrets; missing or mismatched keys block signed publication. An unsigned recovery artifact is a build input, not an installable APK.

## Set up a preview

1. Install the APK and open **Open Launcher Preview**. Select it as Home when ready.
2. Grant Location Access for GPS cards. Enable **Settings > Online Map > Show Embedded Map** only if you want the online map; while visible it sends precise GPS coordinates and the viewed map area to OpenStreetMap.
3. Grant Notification Access to show media sessions and navigation notification text. Start Google Maps or Waze navigation on the head unit, then return Home for directions text.

See the [preview guide](docs/PREVIEW.md) for update, permission and troubleshooting details. Draw Over Other Apps permission does not embed another app's entire window. An external YouTube floating window must be closed in that app.

## Status and development

This is a development preview. The map follows the head unit’s GPS location using OpenStreetMap tiles; it does not mirror Google Maps or import its route line. Earlier device testing showed a blank map area; the latest supplied head-unit photo shows OSM tiles rendering, with location/follow behavior still needing verification. The map uses Android’s local asset loader with automatic retry and resume recovery. Reliable GPS following and ignition/sleep behavior on the QUZHIDA unit still need confirmation. Embedded YouTube playback has been removed; Now Playing retains media-session artwork and controls.

Use JDK 17 and Android SDK 36.1 to build from source:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Passing pushes on main, feature and fix branches publish preview APKs with SHA-256 checksums. Pull requests run checks without publishing. Passing main builds update the download link above automatically.

This public fork does not currently include a repository-level license file. Leaflet and the pinned MIT-licensed rotation plugin retain bundled licenses and provenance in `app/src/main/assets/map/`.
