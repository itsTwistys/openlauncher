# Uni for Android

Uni is an independent native Android movie and TV browsing app that uses Stremio-compatible add-ons. Version 0.2 adopts a warm-dark iOS-inspired layout with a cinematic featured card, translucent floating navigation, poster shelves, and local playback progress.

## Features

- Cinematic title carousel and metadata from Cinemeta and configured catalog add-ons
- Real Continue Watching cards with thumbnails and saved progress for supported direct streams
- Discover, title search, series season selection, and source choices
- Local library and editable Stremio add-ons through their manifest links
- Media3 player for directly playable HTTPS/HTTP video, including HLS and DASH
- External player handoff for unsupported source types

## Install

Open the **Uni Android APK** workflow in GitHub Actions for the `feature/uni-cinema` branch. Download the **Uni-Android-APK** artifact from a successful run, extract the APK, and install it on Android 8 or later.

The app keeps the existing package ID `dev.nova.cinema` so that an install over the older NOVA prototype preserves its local settings and watchlist. The launch name and all visible branding are **Uni**.

## Build

Use JDK 17, Android SDK 36 and Gradle 8.13, then run `gradle :app:assembleDebug`.

The built-in Cinemeta metadata catalog does not provide playable content by itself. Add-ons are independent and Uni does not supply video streams or a debrid connection. Continue Watching works for streams played in the built-in player; external apps manage their own playback history. Use sources you are authorized to access.
