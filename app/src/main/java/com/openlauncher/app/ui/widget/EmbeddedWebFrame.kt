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
internal fun EmbeddedWebFrame(modifier: Modifier, create: () -> WebView?, onRelease: (WebView?) -> Unit = {}) {
    AndroidView(
        modifier = modifier.clipToBounds(),
        factory = { context ->
            FrameLayout(context).apply {
                clipChildren = true
                clipToPadding = true
                create()?.let { addView(it, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)) }
            }
        },
        onRelease = { frame ->
            val web = frame.getChildAt(0) as? WebView
            onRelease(web)
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
