<div align="center">
  <img width="192" height="192" alt="Open Launcher icon" src="https://github.com/user-attachments/assets/4c5c4ddb-836d-4c59-8325-76b8c8d78bb3" />
  <h1>Open Launcher</h1>
  <p>A configurable Android dashboard for aftermarket car head units, with connected maps, weather and sports plus local controls that keep working offline.</p>
</div>

## Download the Android preview

<!-- preview-download:start -->
**[Download Open Launcher 0.0.24-preview preview APK](https://github.com/itsTwistys/openlauncher/releases/download/preview-7af790b/openlauncher-preview.apk)**

[Release notes and checksum](https://github.com/itsTwistys/openlauncher/releases/tag/preview-7af790b) · [Installation and testing guide](https://github.com/itsTwistys/openlauncher/blob/preview-7af790b/docs/PREVIEW.md)
<!-- preview-download:end -->

This permanently signed preview installs as **Open Launcher Preview**. Version 0.0.25 adds optional automation (Driving/Parked layouts, background trip recording, camera and broadcast triggers, steering-wheel key mapping), an OBD2 Bluetooth widget, Agenda and Phone widgets, trip insights, an offline map cache with area downloads, and backup sharing. Everything new is off until you enable it, and nothing locks out features while driving. Existing layouts, shortcuts, setup and the unified theme from 0.0.23 are retained. See [the preview guide](docs/PREVIEW.md) for details and [the head-unit checklist](docs/HEAD-UNIT-CHECKLIST.md) for what to verify on a real unit.

The launcher includes connected vector maps, forecast, sports scores and GitHub updates. Dashboard layout, installed-app shortcuts, media controls and saved settings remain usable offline; connected data shows loading, saved-data or connection states instead of pretending stale data is live.

![Earlier Open Launcher dashboard screenshot; current preview layout and controls may differ](https://github.com/user-attachments/assets/a1bc63f3-2d4e-4ac0-bd56-b5d181681658)

## What is built

- **Dashboard:** a map-first default (2 × 2 Map, 1 × 1 Now Playing, 1 × 1 Clock + Weather) on a 3 × 2 grid. Edit Dashboard adds, removes, resizes and restores defaults with a proportional preview; select cards to resize or reposition, then Apply or Cancel. The resize dialog disables sizes that do not fit, and long-press removal offers an eight-second Undo. The previous eight layouts are saved automatically and can be previewed in Recovery. Map and Now Playing have a one-tap Expand control; Home also returns to the dashboard. Expansion never changes saved positions or sizes. Existing layouts are retained, except that separate Weather cards merge into Clock. Save Driving, Parked, Day and Night layouts and select them from Home, and optionally switch Day/Night automatically.
- **Time and display:** combined Clock + Weather card with digital or analog clock, 12- or 24-hour time, prominent AM/PM, weather-local/system/manual timezone selection, optional seconds, compact/full date, light/dark/system/sunrise themes, text and UI scaling, accent and background options, sidebar position, and wallpaper dimming.
- **Media:** choose Automatic, FM/AM Radio, or an installed app in Now Playing; the choice survives restarts and backups. Startup can reconnect the last used app, or the explicit selected app. It does not automatically play audio or reopen a player on every return Home. If background media connection is unavailable, the app opens its own screen once. Disable this in the player selector or Settings → Connected → Now Playing. An unavailable chosen app shows a disconnected/missing-app state instead of switching to another player. Playback buttons have larger touch areas. Now Playing reads an Android media session through user-granted Notification Access and offers play/pause/skip, seeking and custom save/like/rating actions when exposed by the selected app. Artwork fills the card under one source header; compact cards retain transport buttons. FM/AM Radio uses a supported head-unit MCU or assigned radio app; availability and controls depend on the device. The soundboard has configurable pads.
- **Driving:** GPS compass, speedometer, altimeter, trip distance and elapsed time, a 0–100 km/h timer, and head-unit CPU, memory and temperature readings. These depend on available device sensors.
- **Sports:** add the Sports card in Edit Dashboard. Save favorite leagues and the last selected league/view. Live refreshes while the dashboard is visible and shows recent results / next events when nothing is live; Results covers the past seven days for team sports and the latest classified F1 race, while Upcoming covers the next fourteen days or the F1 race calendar. Dates follow the clock time zone. Offline/error states retain time-stamped scores without labeling them as current live data. ESPN supplies team scores and published F1 session status; Jolpica supplies F1 calendars and classified race results. No account or API key is required. No lap-by-lap F1 telemetry is included.

- **Maps and destinations:** an optional interactive vector GPS map using OpenFreeMap and OpenStreetMap data with Auto/Dark/Light appearance and menu-based Follow, Recenter, zoom and Reload and separate GPS, tile-loading and offline/error states. Camera, marker and heading share a bounded interpolation between nearby fresh fixes; major GPS jumps snap. Minor POIs and dense address labels are hidden in the vector style, with subdued buildings. Both WebGL 1 and 2 are supported. Map options and diagnostics show the active renderer and a safe fallback reason. A blue position dot marks fresh fixes; stale positions are explicit. Auto zoom widens the view at higher GPS speeds, with hysteresis to avoid repeated switching. Manual zoom pauses automation until Recenter. Choose North up or Heading up; vector road labels stay upright while roads rotate. Direction-of-travel rotation requires a fresh, sufficiently accurate GPS fix while moving and returns north-up otherwise. Bundled assets load before GPS; bounded retries, reconnect and resume recovery handle temporary failures. When navigation runs in Google Maps or Waze **on the head unit**, the map card displays a compact next-turn cue from that app's navigation notification. Tap it for trip details and Open navigation. Set Home/Work, choose a preferred navigation app and reuse recent destinations. No Google Maps route line or CarPlay/ZLINK route is embedded.
- **Connectivity and weather:** WiFi/mobile and Android-validated internet state, online weather, and Radar/Traffic cards that open external views. The dashboard and local controls work without WiFi; map tiles and weather require internet.
- **Dashboard controls:** tap the sliders icon for a compact flyout with connection status, Wi-Fi settings, Edit Dashboard, saved layouts and Dashboard tools. Open Dashboard tools for media volume/mute, launcher-only brightness, navigation and Wi-Fi shortcuts, nearby gas/parking/coffee/food/charging/rest-area searches, diagnostics/export and a manual GitHub update check. The update check offers release notes, APK and checksum links, never silent installation.
- **Trips and forecast:** trip totals survive restarts; save up to 200 trips and export CSV. Recording runs while the launcher is visible, excludes stale/inaccurate GPS and sleep gaps, and restores paused after a restart. Clock + Weather shows feels-like, high/low, next-hour rain and sunset when space permits. Tap for the next 12 hours, wind, humidity and sunrise. Optional weather background tint is off by default and preserves the existing dashboard styling.
- **Unified settings:** theme-aware categories and flat configuration panels, readable labels, wrapping option chips, accessible confirmation controls and consistent light/dark/custom font behavior. Settings shows the installed build and opens releases from this repository.
- **Optional automation:** Driving and Parked layouts that switch by speed (debounced, off by default), background trip recording with an ongoing notification, a camera-app launcher and broadcast event triggers for head-unit events such as reverse gear, and steering-wheel key mapping with a key tester. None of it hides or disables anything while the car moves.
- **Vehicle and phone widgets:** an OBD2 widget for an ELM327 Bluetooth adapter (RPM, coolant, battery, speed and more), an Agenda widget for device calendar events and a Phone widget with the dialer and recent calls. Each requests its permission only when you use it.
- **Offline map and trip insights:** the map keeps what it loads and can download a 10, 25 or 50 km area; trip totals, averages and an eight-week distance chart appear under Dashboard tools → Trips.
- **Apps and recovery:** installed-app library, configurable shortcuts with matching Chrome/Spotify/Maps/YouTube/YouTube Music/Waze symbols and consistently sized native app icons, first-run permission setup, and local versioned JSON settings backup/restore.

## Update without repeating setup

Version 0.0.18 starts a new permanent-key channel after the old build process lost its signing key. Export your settings and trip CSV before the one-time reinstall; follow [the migration guide](docs/SIGNING-MIGRATION.md). Later builds signed with this permanent key can update the new installation without clearing its data. The app ID, home-screen appearance and settings format are unchanged. GitHub signing requires the backed-up keystore and password secrets; missing or mismatched keys block signed publication. An unsigned recovery artifact is a build input, not an installable APK.

## Set up a preview

1. Install the APK and open **Open Launcher Preview**. Select it as Home when ready.
2. Add Sports through Edit Dashboard if wanted. It requests public score/calendar feeds from ESPN and Jolpica while visible; it does not send GPS coordinates.
3. Grant Location Access for GPS cards. Enable **Settings > Online Map > Show Embedded Map** only if you want the online map; while visible it sends requests for the viewed map area to OpenFreeMap, or OpenStreetMap in raster fallback. GPS remains on the device; map requests reveal the viewed area.
4. Grant Notification Access to show media sessions and navigation notification text. Start Google Maps or Waze navigation on the head unit, then return Home for directions text.

See the [preview guide](docs/PREVIEW.md) for update, permission and troubleshooting details. Draw Over Other Apps permission does not embed another app's entire window. An external YouTube floating window must be closed in that app.

## Status and development

This is a development preview. The map follows the head unit’s GPS location using OpenFreeMap vector tiles with a north-up OpenStreetMap raster fallback; it does not mirror Google Maps or import its route line. Earlier device testing showed a blank map area; the latest supplied head-unit photo shows OSM tiles rendering, with location/follow behavior still needing verification. The map uses Android’s local asset loader with automatic retry and resume recovery. Reliable GPS following and ignition/sleep behavior on the QUZHIDA unit still need confirmation. Embedded YouTube playback has been removed; Now Playing retains media-session artwork and controls.

Use JDK 17 and Android SDK 36.1 to build from source:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Passing pushes on main, feature and fix branches publish preview APKs with SHA-256 checksums. Pull requests run checks without publishing. Passing main builds update the download link above automatically.

This public fork does not currently include a repository-level license file. MapLibre GL JS 5.6.1 (BSD-3-Clause), Leaflet and the pinned MIT-licensed rotation plugin retain bundled licenses and provenance in `app/src/main/assets/map/`.
