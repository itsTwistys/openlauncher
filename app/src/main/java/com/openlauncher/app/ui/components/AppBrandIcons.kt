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
        moveTo(6f,9f); cubicTo(10f,7.4f,15f,8f,18f,10f)
        moveTo(7f,12.5f); cubicTo(10f,11.3f,14.5f,11.8f,17f,13.3f)
        moveTo(8f,16f); cubicTo(10.5f,15f,13.5f,15.4f,16f,16.5f)
    }
}.build()
