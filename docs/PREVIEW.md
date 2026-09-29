# Open Launcher preview: installation and testing

Use the [current preview download on the main README](https://github.com/itsTwistys/openlauncher#download-the-android-preview). It points to the tested release for that APK. Choose `openlauncher-preview.apk`, not a source archive. `SHA256SUMS.txt` accompanies each APK. The GitHub Actions build and unit tests must succeed before a preview is published.

The preview is debug signed and installs as **Open Launcher Preview** (`com.openlauncher.app.preview`) alongside the original app. The signing certificate is cached between builds, but it is not a production key. If Android reports a signature mismatch after an update, export settings from the old preview before reinstalling. Settings and granted files do not automatically move between the original app and Preview.

## Get started

1. Install the APK on the Android head unit, then open **Open Launcher Preview**.
2. Grant Location Access for GPS cards. Select the preview as the default Home app when ready. Android's default-app settings let you switch back.
3. Tap the pencil for **Edit Dashboard**. Add/remove cards and choose sizes in that screen; unavailable sizes are disabled. **Arrange** enables drag placement on the dashboard. **Restore Default** sets a large Map beside Now Playing and Clock + Weather after confirmation. It preserves personal settings and saved profiles, and stops automatic profile switching until re-enabled. Existing separate Weather cards merge into Clock on upgrade. Undo after removal remains available for eight seconds.
4. To view the online map, add **Map** and turn on **Settings > Online Map > Show Embedded Map**. While the map is visible, OpenStreetMap receives precise GPS coordinates and the viewed map area. Map tiles require internet. The built-in Leaflet code is bundled locally.
5. To show directions from **Google Maps or Waze on the head unit**, enable **Notification Access**, start navigation in that app, and return Home. The Map card can show the title and text exposed by its navigation notification. The map itself uses OpenStreetMap and GPS. It does not draw the other app's route or show directions running only on an iPhone through CarPlay/ZLINK.

The Map card offers Follow, Recenter, Reload, an offline indicator, and a visible message if its script fails to load. **Draw Over Other Apps** is requested through Android settings where supported. Granting it does not enable embedding another app's whole interface.

## Media preference, expansion and map behavior

- Tap the source name at the top of Now Playing. Select Automatic, FM/AM Radio or search installed apps. A selected app must publish an Android media session for controls to work. The app stays selected when disconnected; Open app starts it, and Notification Access status is shown separately. Selecting Automatic restores selection from active sessions.
- Tap the Expand icon above Maps or Now Playing to fill the dashboard. Tap the collapse icon, Back to dashboard, or Android Back to return. Saved layout positions/sizes are unchanged and the existing map view remains mounted through expansion.
- Map offers saved Auto zoom and North up/Heading up choices. Zoom levels are 17 at low speeds, 16 at medium speeds and 15 at high speeds (nominal boundaries 35 and 80 km/h, with hysteresis and at least five seconds between automatic transitions). Manual zoom stays in place until Recenter or toggling Auto zoom. Panning pauses following.
- Heading up uses GPS travel bearing at speeds of at least 2 m/s, accuracy within 50 m and fix age under 30 seconds. When direction is unavailable it uses north-up. Recenter is larger and restores following/auto zoom. No compass or location permission is granted to the web page; the launcher supplies motion data.

## More controls

- Save Driving, Parked, Day and Night widget layouts. Switch saved layouts from Home; automatic Day/Night requires both profiles to be saved.
- Set 12-hour time, clock seconds and date format in Settings. Use Home and Work destinations, pick a preferred navigation app, and choose recent successful destinations.
- The connectivity card distinguishes WiFi/mobile connection from Android's validated internet status where available. Radar and Traffic open external views.
- Export/restore a versioned JSON settings backup. Addresses are included. File grants for wallpaper/audio cannot be transferred; assign those assets again. Restoring requires a fresh opt-in for the online map.

## Map recovery and readability

The engine loads bundled assets before a GPS fix. Status distinguishes missing permission, location disabled, waiting/stale GPS, map engine loading, tile loading and offline/failure. Tiles retry up to three times with backoff, then retry on reconnection or resume. Renderer/engine failures get up to two automatic rebuilds per recovery cycle; Reload remains available. GPS registration renews on resume and an older cached fix cannot replace a newer fix. Weather failures retry on subsequent updates rather than waiting a full 30 minutes.

Directions, media titles and trip data use larger sans-serif text and stronger contrast. Clock includes temperature and conditions; a separate weather card is no longer offered. Fresh installs use the map-first layout and system font. Online Map remains opt-in.

## Verification status

Version 0.0.9 removes the YouTube card, video source in Now Playing, and video-link settings. Existing preferences and older backups discard retired video cards from current and saved layouts. Now Playing continues to support Android media-session controls and artwork.

The OpenStreetMap GPS map remains available. Earlier browser checks used simulated tiles; earlier QUZHIDA photos showed a blank embedded map. Device rendering remains unconfirmed. The map exposes loading/error messages and Reload for troubleshooting.

## Build from source

Use JDK 17, Android SDK 36.1 and the included Gradle wrapper:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The workflow builds the same targets on pull requests and publishes a prerelease APK after a successful push. The development preview is for testing, not a production signing or device compatibility guarantee.
