# Open Launcher preview: installation and testing

Use the [current preview download on the main README](https://github.com/itsTwistys/openlauncher#download-the-android-preview). It points to the tested release for that APK. Choose `openlauncher-preview.apk`, not a source archive. `SHA256SUMS.txt` accompanies each APK. The GitHub Actions build and unit tests must succeed before a preview is published.

The preview is debug signed and installs as **Open Launcher Preview** (`com.openlauncher.app.preview`) alongside the original app. The signing certificate is cached between builds, but it is not a production key. If Android reports a signature mismatch after an update, export settings from the old preview before reinstalling. Settings and granted files do not automatically move between the original app and Preview.

## Get started

1. Install the APK on the Android head unit, then open **Open Launcher Preview**.
2. Grant Location Access for GPS cards. Select the preview as the default Home app when ready. Android's default-app settings let you switch back.
3. Use the widget library to add and remove cards. Long-press a card for Resize; unavailable sizes are disabled and the dialog previews placement. Undo after removal is available for eight seconds.
4. To view the online map, add **Map** and turn on **Settings > Online Map > Show Embedded Map**. While the map is visible, OpenStreetMap receives precise GPS coordinates and the viewed map area. Map tiles require internet. The built-in Leaflet code is bundled locally.
5. To show directions from **Google Maps or Waze on the head unit**, enable **Notification Access**, start navigation in that app, and return Home. The Map card can show the title and text exposed by its navigation notification. The map itself uses OpenStreetMap and GPS. It does not draw the other app's route or show directions running only on an iPhone through CarPlay/ZLINK.
6. To use YouTube, paste a specific video or live-stream Share link in **Settings > YouTube**. Choose **YouTube video** from the three-dot source menu on **Now Playing**. Remove the separate YouTube card if you want the combined player, enlarge Now Playing if prompted, and tap **I'm parked · Load player**, then YouTube's play control. Some videos cannot be embedded. A channel `/live` address is not a video link. Video requires internet and a working Android System WebView.
7. If a separate YouTube app opens a floating window, close that window using its own controls. The launcher cannot move another app's window into a widget. Video pauses when leaving the launcher, entering edit mode, losing connectivity, or detecting GPS movement above about 5 km/h. GPS is not a parking brake; play video only while parked.

The Map card offers Follow, Recenter, Reload, an offline indicator, and a visible message if its script fails to load. **Draw Over Other Apps** is requested through Android settings where supported. Granting it does not enable embedding another app's whole interface.

## More controls

- Save Driving, Parked, Day and Night widget layouts. Switch saved layouts from Home; automatic Day/Night requires both profiles to be saved.
- Set 12-hour time, clock seconds and date format in Settings. Use Home and Work destinations, pick a preferred navigation app, and choose recent successful destinations.
- The connectivity card distinguishes WiFi/mobile connection from Android's validated internet status where available. Radar and Traffic open external views.
- Export/restore a versioned JSON settings backup. Addresses are included. File grants for wallpaper/audio cannot be transferred; assign those assets again. Restoring requires a fresh opt-in for the online map.

## Verification status

The Android debug build and 13 unit tests passed. Browser checks covered map marker updates, Follow/Recenter/offline states and YouTube frame bounds with simulated responses. User photos confirmed Google Maps navigation notification text but also showed a blank embedded map and floating video on a QUZHIDA unit. Version 0.0.8 addresses widget identity, native WebView clipping, map rendering and diagnostics. The actual fix and YouTube playback **have not yet been confirmed on that head unit**.

## Build from source

Use JDK 17, Android SDK 36.1 and the included Gradle wrapper:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The workflow builds the same targets on pull requests and publishes a prerelease APK after a successful push. The development preview is for testing, not a production signing or device compatibility guarantee.
