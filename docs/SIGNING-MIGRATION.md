# One-time migration to the permanent signing key

0.0.18-preview (versionCode 19) contains the existing map, compact navigation, app rail, weather, media controls and dashboard editor. Its appearance and settings format are unchanged. It uses a new permanent key because the previous build process did not save the old key. Android will reject installation over an old-key preview. Future updates must use this new key.

## Preserve your setup first

1. In the installed launcher, open **Settings → Backup and Restore → Export backup**. Save `openlauncher-settings.json` in Downloads and copy it to another device or storage location. Confirm the file opens and contains `"format": "openlauncher-settings"`, `"version": 1` and a `settings` object. Do not post this file publicly; it includes saved destination addresses.
2. In dashboard controls → Trips, export your trip history as CSV if wanted. CSV preserves a readable record; there is no trip-history CSV import. Settings backup does not contain trip history, current trip totals or the weather cache.
3. Keep copies of your wallpaper and soundboard audio files. Note the current default Home app and permissions. Take a dashboard screenshot as an additional reference.
4. Only after you have a verified backup and the new signed APK, uninstall **Open Launcher Preview**, then install the new APK. Do not uninstall other launchers, navigation/media apps, ZLINK or system components.
5. Open the new launcher and use **Settings → Backup and Restore → Restore backup**, review the restoration prompt and confirm. Restore location and notification access, select the default Home app as needed, and explicitly enable the online map again. Reassign wallpaper and soundboard audio files.
6. Check your layout, shortcuts, clock timezone, media/navigation choices and map position while parked. Physical head-unit rendering and ignition/wake behavior still require an on-device check.

Settings export preserves layouts/profiles/history, shortcuts, theme/font, vehicle name, clock preferences, selected media/navigation apps and destinations. Android permission grants and external-file access do not transfer after uninstall. Backup import intentionally clears wallpaper/audio URIs and turns off online-map sharing until you opt in again. If Export backup is absent or the file cannot be verified, keep the old installation until another migration path is arranged.

## Permanent key and future builds

Keep `OpenLauncher-PRIVATE-signing-backup.zip` private, with a second copy in encrypted storage. It contains the keystore, its password, public certificate and a PowerShell setup script. Never commit it, attach it to a GitHub issue or upload it as a public Actions artifact.

The setup script uses GitHub CLI authentication to store the keystore and password in Actions secrets for `itsTwistys/openlauncher`. It sends secret values through standard input without printing them or putting them in command-line arguments. Only `main` push/manual builds receive the key. Then run **Actions → Android preview → Run workflow** on main. A missing key/password or wrong certificate blocks signed publication.

Certificate SHA-256: `9994a7888b6f5b7d3c3d58b14463ead3eae8ccd5d35060886407e523d2120e3f`.

After this migration, install new releases over the permanent-key version to retain app data. Do not regenerate the key for each build. The unsigned recovery bundle is not installable; it must be signed and verified first.
