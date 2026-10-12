package com.openlauncher.app.ui.widget

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal data class RecentCall(val label: String, val number: String, val type: Int, val dateMs: Long)

/** Recent calls from the device call log. Read only; tapping a row opens the dialer and never places the call. */
@SuppressLint("MissingPermission")
internal fun loadRecentCalls(context: Context, limit: Int = 6): List<RecentCall> = runCatching {
    val calls = ArrayList<RecentCall>()
    context.contentResolver.query(
        CallLog.Calls.CONTENT_URI,
        arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE),
        null, null, "${CallLog.Calls.DATE} DESC"
    )?.use { cursor ->
        while (cursor.moveToNext() && calls.size < limit) {
            val number = cursor.getString(0).orEmpty()
            if (number.isBlank()) continue
            calls += RecentCall(cursor.getString(1).orEmpty().ifBlank { number }, number, cursor.getInt(2), cursor.getLong(3))
        }
    }
    calls.toList()
}.getOrDefault(emptyList())

private fun openDialer(context: Context, number: String? = null) {
    val intent = if (number == null) Intent(Intent.ACTION_DIAL) else Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
fun PhoneWidget(isDayMode: Boolean, isEditing: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var permitted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED)
    }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permitted = it }
    var calls by remember { mutableStateOf<List<RecentCall>>(emptyList()) }
    LaunchedEffect(permitted) {
        if (!permitted) return@LaunchedEffect
        while (true) {
            calls = withContext(Dispatchers.IO) { loadRecentCalls(context) }
            delay(60_000L)
        }
    }
    val color = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val dim = color.copy(alpha = 0.6f)
    Column(modifier.padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { openDialer(context) }, enabled = !isEditing, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Default.Call, null, Modifier.size(18.dp)); Text("  Dialer", fontSize = 13.sp)
            }
            if (!permitted) TextButton(onClick = { request.launch(Manifest.permission.READ_CALL_LOG) }, enabled = !isEditing,
                modifier = Modifier.heightIn(min = 48.dp)) { Text("Show recent calls", fontSize = 13.sp) }
        }
        if (permitted && calls.isEmpty()) Text("No recent calls.", fontSize = 13.sp, color = dim)
        calls.forEach { call ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = !isEditing) { openDialer(context, call.number) },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    when (call.type) {
                        CallLog.Calls.MISSED_TYPE -> Icons.Default.CallMissed
                        CallLog.Calls.OUTGOING_TYPE -> Icons.Default.CallMade
                        else -> Icons.Default.CallReceived
                    }, null, Modifier.size(18.dp),
                    tint = if (call.type == CallLog.Calls.MISSED_TYPE) MaterialTheme.colorScheme.error else dim
                )
                Column {
                    Text(call.label, fontSize = 15.sp, color = color, maxLines = 1)
                    if (call.label != call.number) Text(call.number, fontSize = 11.sp, color = dim, maxLines = 1)
                }
            }
        }
    }
}
