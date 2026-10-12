# Head-unit verification checklist

CI cannot exercise a real head unit. Run this list on the actual device after installing a preview, and record the result
for the build (version name is shown in Settings). Items marked **upgrade** matter most after any signing or storage change.

## Install and upgrade

- [ ] **upgrade** Install the new APK over the previous permanent-key preview. It must install without uninstalling.
- [ ] **upgrade** Layout, shortcuts, theme, destinations, trip history and the Now Playing app survive the update.
- [ ] Settings shows the new version name and the preview release list opens.
- [ ] Settings → Backup → Export backup, then Restore backup, round-trips without errors.

## Location and map

- [ ] GPS fix arrives within a couple of minutes outdoors (Settings → Diagnostics shows its age and accuracy).
- [ ] Map follows the car, the blue dot is correct and heading-up rotates only while moving.
- [ ] Map options → Follow, Recenter and zoom work; auto zoom widens at higher speed.
- [ ] Turn the car off and on again, and let the head unit sleep and wake. GPS, map and music reconnect.
- [ ] Offline map: download an area (Dashboard tools → Offline map), switch Wi-Fi/data off, and confirm the area still renders.

## Media

- [ ] Now Playing shows the track, artwork and seek bar for Spotify.
- [ ] Play/pause, next and previous work. Spotify-specific buttons (like, save) appear only when Spotify exposes them.
- [ ] Hardware radio widget controls work (seek, band) on MCU units.
- [ ] Steering-wheel buttons: Settings → Automation → Steering keys → Key tester shows the code each button sends.

## Dashboard

- [ ] Edit Dashboard add/remove/resize works for each widget, including OBD, Agenda and Phone.
- [ ] Driving/Parked auto layouts (if enabled) switch after the configured delay and do not flicker at traffic lights.
- [ ] Trip recording continues in the background if Background trip recording is enabled, and stops when the trip is paused.

## Optional hardware

- [ ] OBD2: pair the adapter in Android Bluetooth settings, choose it in the OBD widget, and confirm RPM/coolant/battery readings.
- [ ] Reverse camera: with an event trigger set to the head unit's reverse broadcast, putting the car in reverse opens the camera app.
- [ ] Calendar widget lists today's events after granting Calendar access; Phone widget lists recent calls after granting Call log access.

## Known gaps to confirm

- Ignition and sleep behavior on the QUZHIDA unit.
- Whether the unit forwards steering-wheel keys to the foreground app, or only to the active media session.
