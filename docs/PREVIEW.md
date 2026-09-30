# Open Launcher preview: installation and testing

## Consistent appearance and in-place updates in 0.0.16

Map controls now use the dashboard accent and matching bundled font, with the same flat controls and small corners. Media controls use the dashboard accent and contrast color; media, clock and directions honor the chosen font. Album art and map content remain visible within the existing card design.

Install the release APK **over Open Launcher Preview**. Keep the existing app installed and do not clear storage. Its application ID and settings store stay unchanged, with an increased Android version code. Dashboard layouts, shortcuts, media/navigation selection, theme, wallpaper URI, destinations and completed setup remain in the app's data. Android still controls permission and default-Home grants; the launcher does not reset them. This updates the launcher, not the separately installed Spotify, Maps or Waze apps.

Published builds must restore the existing signing key and pass an APK check against preview-25a391c: matching application ID, matching verified certificate and a higher version code. Missing keys or signature mismatch block publication. The signing key is still in GitHub's existing cache; a missing cache must be recovered by the owner instead of generating a replacement. This guard prevents publishing an incompatible update but does not make cache storage permanent. A different legacy application ID/certificate cannot be upgraded through this preview channel.

A repository reopen regression checks persisted setup and a subsequent settings edit. CI verifies real APK identities/signatures before publication. An on-device package replacement has not been automated.

## Weather, media icons and driving maps in 0.0.15

- Clock + Weather adds feels-like, daily high/low, next-hour rain probability and sunset on cards at least 220 dp tall. Smaller cards retain the clock and conditions. Tap the card for the full forecast, including wind, humidity and sunrise. Missing or expired hourly values show Unavailable, not zero; existing cache age/offline indicators remain. Sun times use the forecast location's timezone.
- Rich media actions use the selected app's supplied icons with 48 dp targets and accessible labels. If an icon cannot be loaded, familiar action labels receive a matching symbol; unknown actions use More. Like/rating and Open player are icon buttons too. Dispatch still uses only currently exposed session actions, never guessed Spotify commands.
- **Map options → Auto / Dark / Light** controls the built-in map. Auto follows the dashboard theme. Dark recolors only the raster tile pane and controls, preserving the blue GPS marker and attribution. No new tile provider, key or account is required; changing appearance retains the map and cached tiles.
- GPS updates no longer resend all map options. Unchanged orientation does not reset north-up, map/tile fade transitions are disabled, tile retention is increased, and ordinary tile loading no longer flashes a blocking loading banner over an already rendered map. First load, offline and failure states remain visible.
- The map tile shows supported Google Maps/Waze navigation notification instructions and separate trip details (such as ETA and remaining distance) when the source supplies them. Start navigation on the head unit and grant Notification Access. The selected navigation app preference applies. Tap the banner to return to that route. Its height stays fixed as turn text changes so the map is not resized on every instruction. This does not embed another app's route geometry or read CarPlay-only navigation. Unsupported notification fields are not fabricated.

Regression coverage includes simulated driving without tile redraws, dark/light switching without map recreation, retained marker colors, transient loading visibility, weather fields/cache/timezones, navigation text deduplication, fixed banner bounds and icon-only media controls. Actual road-motion smoothness and provider notification contents still require physical head-unit verification.

## Dashboard polish in 0.0.14

- Now Playing artwork fills the card behind one source header. Track text, large transport controls, seeking and additional controls adapt to available height. Position advances between media-session updates. Failed full-resolution artwork falls back to the session bitmap.
- Like, save, rating and other actions appear only when the selected media app exposes them through its Android media session. Actions are rechecked before dispatch. Spotify versions may expose different controls; Open player remains available. This does not add Spotify account authorization or invent unsupported library commands.
- Clock Time Zone defaults to **Automatic local weather**, with **System** and searchable IANA zones as overrides. AM/PM is prominent, and the displayed timezone is visible. The override affects the launcher only. If the device's actual date or minutes are incorrect, correct its date/time separately. Automatic location time needs a successful forecast; cached forecasts retain their timezone.
- Map adds a clear blue position dot, accuracy circle, stale-location indication and short animated movement between nearby fresh fixes. GPS updates request one-second intervals; actual frequency depends on the head unit. Recent precise GPS fixes resist coarse network-location jumps. Resizing while following keeps the position centered. Existing tile provider, theme and map controls remain.
- Chrome, Spotify and Google Maps have matching monochrome sidebar symbols. Other installed apps use their native icons at the same size; native icons remain selectable for the named apps too.
- Traffic explicitly opens Google Maps or a browser with the documented traffic layer URL and visible launch-failure feedback. The embedded OSM card has no live traffic data feed. If the Maps app ignores the layer, use Layers → Traffic or Open in browser.

Validation covers map movement/centering/stale fixes with simulated tiles, local-time/AM-PM boundaries, GPS quality filtering, traffic link freshness and media-session action gating. Physical GPS reception, Spotify's available controls, live traffic provider behavior and ignition wake still need head-unit verification.

## Dashboard reliability and readability in 0.0.13

- Map settings now live in **Map options**. Auto zoom, Heading up, GPS/tile status and Reload are available in a scrollable dialog; the dashboard keeps navigation and exceptional status messages visible.
- **Compatibility rendering** preserves the earlier software-rendering default. While parked, if diagnostics show loaded tiles but the map is visibly blank, switch it off to try Android's default accelerated rendering. Changing this saved setting recreates only the map. Switching it back restores compatibility rendering. This is a troubleshooting choice, not confirmed QUZHIDA certification.
- Map resizing waits for nonzero bounds after wake or expansion; zoom buttons have larger touch targets. Sidebar icons, dashboard controls and resize controls are easier to see and tap.
- The last successful conditions and up to 48 forecast hours are cached atomically on the device, without coordinates. The original update time is retained after restart; age, offline, stale (30+ minutes) and expired (48+ hours) states are explicit. Expired or corrupt saved forecasts are ignored at startup. The cache is separate from settings backups and does not add offline map tiles.
- Android 6+ map/weather internet availability uses the validated default network, including Ethernet and VPN. A hotspot without validated internet is shown offline. Android 5 uses its connected-network signal because it cannot report validation.

### Verification for this release

CI runs Android lint, unit/Compose tests, APK assembly and map browser checks with simulated tiles. Browser checks include reconnect/error recovery and zero-height/restored map bounds. Tests cover weather restart/power-loss restoration, cache validity/freshness, Ethernet/VPN and captive portals, map options at 600×360 dp with enlarged text, and native map-frame resize/recreation/release.

Physical QUZHIDA rendering and ignition behavior still require a head-unit check. While parked, verify cold start, switching from navigation back to Home, map expansion/collapse, hotspot reconnection and ignition sleep/wake. For a blank map, export diagnostics before and after changing Compatibility rendering, using the existing evidence procedure below. Browser tiles are simulated; passing CI does not prove live tile delivery or physical head-unit pixels.

Use the [current preview download on the main README](https://github.com/itsTwistys/openlauncher#download-the-android-preview). It points to the tested release for that APK. Choose `openlauncher-preview.apk`, not a source archive. `SHA256SUMS.txt` accompanies each APK. The GitHub Actions build and unit tests must succeed before a preview is published.

The preview is debug signed and installs as **Open Launcher Preview** (`com.openlauncher.app.preview`) alongside the original app. The signing certificate is cached between builds, but it is not a production key. If Android reports a signature mismatch, keep the existing app installed and recover a correctly signed update. Do not uninstall as an update workaround. Settings and granted files do not automatically move between the original app and Preview.

## Get started

1. Install the APK on the Android head unit, then open **Open Launcher Preview**.
2. Grant Location Access for GPS cards. Select the preview as the default Home app when ready. Android's default-app settings let you switch back.
3. Tap the pencil for **Edit Dashboard**. The compact grid offers a proportional preview. Tap a widget to add/select it, choose a size or use the arrow buttons to move it. Adding to a full layout may shrink existing cards in the draft, with a visible notice. **Apply** saves the draft; **Cancel**, Close or Back discards it. **Recovery** previews up to eight previous layouts, including empty layouts. **Restore default** changes the draft after confirmation; Apply keeps it. Existing long-press dashboard arrangement remains available. The default is a large Map beside Now Playing and Clock + Weather. It preserves personal settings and saved profiles, and stops automatic profile switching until re-enabled. Existing separate Weather cards merge into Clock on upgrade. Long-press removal retains its eight-second Undo; editor changes can be canceled before Apply or recovered afterward.
4. To view the online map, add **Map** and turn on **Settings > Online Map > Show Embedded Map**. While the map is visible, OpenStreetMap receives precise GPS coordinates and the viewed map area. Map tiles require internet. The built-in Leaflet code is bundled locally.
5. To show directions from **Google Maps or Waze on the head unit**, enable **Notification Access**, start navigation in that app, and return Home. The Map card can show the title and text exposed by its navigation notification. The map itself uses OpenStreetMap and GPS. It does not draw the other app's route or show directions running only on an iPhone through CarPlay/ZLINK.

The Map card offers Follow, Recenter, Reload, an offline indicator, and a visible message if its script fails to load. **Draw Over Other Apps** is requested through Android settings where supported. Granting it does not enable embedding another app's whole interface.

## Media preference, expansion and map behavior

- Tap the source name at the top of Now Playing. Select Automatic, FM/AM Radio or search installed apps. A selected app must publish an Android media session for controls to work. The app stays selected when disconnected; Open app starts it, and Notification Access status is shown separately. Selecting Automatic restores selection from active sessions.
- Tap the Expand icon above Maps or Now Playing to fill the dashboard. Tap the collapse icon, Back to dashboard, or Android Back to return. Saved layout positions/sizes are unchanged and the existing map view remains mounted through expansion.
- Map offers saved Auto zoom and North up/Heading up choices. Zoom levels are 17 at low speeds, 16 at medium speeds and 15 at high speeds (nominal boundaries 35 and 80 km/h, with hysteresis and at least five seconds between automatic transitions). Manual zoom stays in place until Recenter or toggling Auto zoom. Panning pauses following.
- Heading up uses GPS travel bearing at speeds of at least 2 m/s, accuracy within 50 m and fix age under 30 seconds. When direction is unavailable it uses north-up. Recenter is larger and restores following/auto zoom. No compass or location permission is granted to the web page; the launcher supplies motion data.

## New in 0.0.12

The dashboard retains its existing theme, fonts, thin borders and widget styling. Tap the sliders icon in its header for these additions:

- **Quick controls:** media-stream volume/mute, a persistent launcher-window brightness override with Use system brightness, navigation and Wi-Fi settings shortcuts. Hardware radio/MCU volume may remain controlled by the head unit; fixed Android volume is detected. No firmware or global brightness settings are changed.
- **Nearby:** Google Maps app/browser searches for gas, parking, coffee, restaurants, charging and rest areas. Choose a result in Maps to navigate. This does not embed a Google Maps route into the OSM widget.
- **Trips:** Start/Pause/Save from the existing widget or controls. Current totals persist every five seconds and on controls/stop; an abrupt power cut can lose the last interval. Trips restore paused. Recording requires the launcher to be visible; no background service is added. GPS older than five seconds, accuracy worse than 50 m, invalid speed and sleep gaps are excluded. History retains the latest 200 aggregate trips, supports CSV export and confirmed deletion, and stores no coordinates. Settings backups do not include trip history; export CSV separately.
- **Weather:** also opens by tapping Clock + Weather. Shows up to 12 forecast hours, rain probability, feels-like and high/low, with refresh, offline/error states and the last update time. Forecast uses local time at the forecast location and your temperature/time units. Open-Meteo requires a connection; cached-in-memory results remain visible after an interruption. Optional weather background tint defaults off and expires after an hour without refreshed conditions.
- **Diagnostics:** live GPS age/accuracy, permissions, internet validation, media connection, WebView version, last map status/time, map engine stage and initialization attempts, console warning/error counts, HTTP/network/SSL failure codes by resource category, renderer failures, current-page tile load/error/timeout counts, and weather state. Each failure kind retains its last code/category/time even after recovery. Export writes these same statuses and counts; no URLs (including tile paths), console/error text, GPS coordinates, saved addresses or track titles.
- **Updates:** manual GitHub check selects published previews with APK and checksum assets. These are produced after passing build/unit tests, not physical-device certification. View notes/checksum or download through your browser; Android handles installation. No automatic installation or new install permission.

The editor's **Recovery** tab is local and automatic after dashboard changes; restoring previews a draft before Apply. Color/font/wallpaper preferences are unaffected. Changes to settings and installed apps keep their existing behavior. Now Playing reuses unchanged per-session states when notifications arrive; explicit metadata callbacks still refresh artwork.

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

The OpenStreetMap GPS map remains available. Earlier browser checks used simulated tiles; earlier QUZHIDA photos showed a blank embedded map. Device rendering remains unconfirmed. The map exposes loading/error messages and Reload for troubleshooting. Successful build/browser checks do not establish physical QUZHIDA rendering.

### QUZHIDA blank-map evidence to collect

Use the preview built from the current PR #8 head, not an older main-branch download. Record its full commit or release tag with the export (the app version alone can be shared by several preview commits). Leave firmware, MCU, WebView provider and system settings unchanged.

1. While parked, open the existing Map with its current online-map consent and permissions. Wait at least 20 seconds. Note whether the map is blank, shows a loading/error message, or draws tiles. In **Dashboard tools > Diagnostics**, export `openlauncher-diagnostics.txt` before closing the app.
2. Retain the App, Device, WebView package/version, GPS permission/fix age/accuracy, Internet, Map status/time, Map engine, Map tiles and all Map failure rows. Add whether this was first open, after **Reload**, after returning from another app, or after a normal ignition sleep/wake. Note approximate elapsed time since opening the map. Do not send coordinates, addresses, media titles, full URLs or raw console/logcat output.
3. Tap the existing **Reload** once, wait 20 seconds and export again. If the issue occurs after a normal sleep/wake or an existing network interruption/reconnection, collect another export then. No network, location or WebView system-setting changes are required for this check.
4. If needed, attach a cropped image of the blank map/status or diagnostics only, excluding the map's identifiable location, navigation text, destinations and media titles.

Interpretation:

- `CREATE_FAILED`, `SCRIPT_FAILED`, `TIMED_OUT` or `EVALUATION_FAILED` isolate WebView construction, script readiness, readiness timeout or evaluation failures. Console errors can explain a page that finishes loading but never becomes ready; raw messages are intentionally discarded.
- `HTTP` retains the actual response status (for example 403/429/5xx); `NETWORK` retains Android's numeric WebView error code; `SSL` retains the primary SSL error code and always cancels the request. Resource categories are `PAGE`, `LOCAL_ASSET`, `TILE` and `OTHER`. These callback counts can overlap with tile/console events and are not unique-request totals. HTTP detail is available from WebView on Android 6+; the QUZHIDA Android 13 unit supports it.
- `READY` with `started=false` means the tile layer has not started, often because GPS or connectivity is unavailable. Tile counters are cumulative for the current WebView page, survive retries, and reset on Reload/recreation. Native failure counts and the last failure of each kind survive Reload until the app process ends. Counters update about every three seconds while the map is ready and resumed; export before restarting the app.
- `READY`, `loaded>0` and a visibly blank map narrows the investigation toward device rendering/layout. It still does not prove pixels are visible. `errors>0` or `timeouts>0` with HTTP/network evidence instead points toward tile delivery. A later “Map ready” status does not clear prior failure evidence.

## Build from source

Use JDK 17, Android SDK 36.1 and the included Gradle wrapper:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The workflow builds the same targets on pull requests and publishes a prerelease APK after a successful push. The development preview is for testing, not a production signing or device compatibility guarantee.
