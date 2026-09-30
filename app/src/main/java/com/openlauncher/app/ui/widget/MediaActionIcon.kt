package com.openlauncher.app.ui.widget

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.core.graphics.drawable.toBitmap
import com.openlauncher.app.model.MediaAction

/** Use the player's own action icon, retaining its exact label for accessibility. */
@Composable
internal fun MediaActionIcon(action: MediaAction, packageName: String?, tint: Color) {
    val context = LocalContext.current
    val pixels = with(LocalDensity.current) { 24.dp.roundToPx() }
    val bitmap = remember(packageName, action.icon, pixels) {
        runCatching {
            if (packageName == null || action.icon == 0) null else
                context.packageManager.getResourcesForApplication(packageName)
                    .getDrawable(action.icon, null)?.toBitmap(pixels, pixels)?.asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) Icon(BitmapPainter(bitmap), action.label, Modifier.size(24.dp), tint = tint)
    else Icon(when {
        action.label.contains("dislike", true) -> Icons.Default.ThumbDown
        action.label.contains("like", true) || action.label.contains("favorite", true) ->
            if (action.label.contains("remove", true) || action.label.contains("unlike", true)) Icons.Default.Favorite else Icons.Default.FavoriteBorder
        action.label.contains("shuffle", true) -> Icons.Default.Shuffle
        action.label.contains("repeat", true) -> Icons.Default.Repeat
        action.label.contains("queue", true) -> Icons.Default.QueueMusic
        action.label.contains("save", true) -> Icons.Default.BookmarkBorder
        action.label.contains("radio", true) -> Icons.Default.Radio
        else -> Icons.Default.MoreHoriz
    }, action.label, Modifier.size(24.dp), tint = tint)
}
