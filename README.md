# NOVA Cinema for Android

NOVA Cinema is an independent, native Android movie and series browser powered by Stremio-compatible add-ons.

Features: cinematic Jetpack Compose browsing, movie and TV catalogs, title search, detailed metadata, season and episode selection, local watchlist, configurable add-on manifests, multi-add-on stream discovery, and Media3 video playback for HTTP/HTTPS streams.

## Get the APK

Go to GitHub Actions for this branch, select **NOVA Cinema APK**, open a successful run, and download **NOVA-Cinema-Android-APK**. Extract `app-debug.apk` and install it on Android 8 or newer.

## Build from source

Use Android Studio or JDK 17, Android SDK 36, and Gradle 8.13.

```sh
gradle :app:assembleDebug
```

The app's package is `dev.nova.cinema`. Its built-in Cinemeta catalog gives you movie and TV metadata. To find playable streams, install a compatible add-on you trust using its Stremio manifest link. Stream availability and external-player support depend on the installed add-ons. No content is bundled.

NOVA is not affiliated with or endorsed by Stremio. You are responsible for using sources and media you are authorized to access.