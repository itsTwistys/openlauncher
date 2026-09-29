package com.openlauncher.app.ui.widget

import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView

/** Give native web content an explicit, clipped owner tied to AndroidView's lifetime. */
@Composable
internal fun EmbeddedWebFrame(modifier: Modifier, create: () -> WebView) {
    AndroidView(
        modifier = modifier.clipToBounds(),
        factory = { context ->
            FrameLayout(context).apply {
                clipChildren = true
                clipToPadding = true
                addView(create(), FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
        },
        onRelease = { frame ->
            val web = frame.getChildAt(0) as? WebView
            frame.removeAllViews()
            web?.apply {
                onPause()
                stopLoading()
                loadUrl("about:blank")
                removeAllViews()
                destroy()
            }
        }
    )
}
