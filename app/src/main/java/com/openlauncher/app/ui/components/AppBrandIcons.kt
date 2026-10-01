package com.openlauncher.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.openlauncher.app.data.DefaultShortcutIcon

/** Monochrome sidebar symbols, with native icon fallback for every other installed app. */
fun automaticShortcutIcon(packageName: String): DefaultShortcutIcon? = when (packageName) {
    "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary" -> DefaultShortcutIcon.CHROME
    "com.spotify.music", "com.spotify.lite" -> DefaultShortcutIcon.SPOTIFY
    "com.google.android.apps.maps", "com.google.android.apps.mapslite" -> DefaultShortcutIcon.GOOGLE_MAPS
    "com.google.android.youtube", "com.google.android.youtube.tv" -> DefaultShortcutIcon.YOUTUBE
    "com.google.android.apps.youtube.music" -> DefaultShortcutIcon.YOUTUBE_MUSIC
    "com.waze" -> DefaultShortcutIcon.WAZE
    else -> null
}
val ChromeSidebarIcon = ImageVector.Builder("Chrome", 24.dp, 24.dp, 24f, 24f).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
        moveTo(22f,12f); arcTo(10f,10f,0f,true,true,2f,12f); arcTo(10f,10f,0f,true,true,22f,12f); close()
        moveTo(16f,12f); arcTo(4f,4f,0f,true,true,8f,12f); arcTo(4f,4f,0f,true,true,16f,12f); close()
        moveTo(12f,8f); lineTo(21f,8f); moveTo(8.5f,14f); lineTo(4f,6f); moveTo(15.5f,14f); lineTo(11f,22f)
    }
}.build()
val SpotifySidebarIcon = ImageVector.Builder("Spotify", 24.dp, 24.dp, 24f, 24f).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
        moveTo(22f,12f); arcTo(10f,10f,0f,true,true,2f,12f); arcTo(10f,10f,0f,true,true,22f,12f); close()
        moveTo(6f,9f); curveTo(10f,7.4f,15f,8f,18f,10f)
        moveTo(7f,12.5f); curveTo(10f,11.3f,14.5f,11.8f,17f,13.3f)
        moveTo(8f,16f); curveTo(10.5f,15f,13.5f,15.4f,16f,16.5f)
    }
}.build()

val YouTubeSidebarIcon = ImageVector.Builder("YouTube", 24.dp, 24.dp, 24f, 24f).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f) {
        moveTo(5f,5f); lineTo(19f,5f); quadTo(22f,5f,22f,8f); lineTo(22f,16f)
        quadTo(22f,19f,19f,19f); lineTo(5f,19f); quadTo(2f,19f,2f,16f)
        lineTo(2f,8f); quadTo(2f,5f,5f,5f); close()
    }
    path(fill = SolidColor(Color.Black)) {
        moveTo(10f,8f); lineTo(16f,12f); lineTo(10f,16f); close()
    }
}.build()
val WazeSidebarIcon = ImageVector.Builder("Waze", 24.dp, 24.dp, 24f, 24f).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
        moveTo(5f,12f); curveTo(3f,1f,22f,1f,22f,12f)
        curveTo(22f,20f,8f,21f,2f,14f); quadTo(5f,15f,5f,12f); close()
        moveTo(10f,13f); quadTo(14f,17f,18f,13f)
        moveTo(10f,9f); lineTo(10f,10f); moveTo(17f,9f); lineTo(17f,10f)
    }
}.build()
