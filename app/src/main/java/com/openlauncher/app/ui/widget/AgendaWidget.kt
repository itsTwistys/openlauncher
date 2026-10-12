package com.openlauncher.app.ui.widget

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.openlauncher.app.util.AgendaEvent
import com.openlauncher.app.util.agendaTimeLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val HORIZON_MS = 36L * 60 * 60 * 1000

/** Upcoming events from the calendars already on this device. Read only; nothing leaves the head unit. */
@SuppressLint("MissingPermission")
internal fun loadAgenda(context: Context, nowMs: Long, limit: Int = 8): List<AgendaEvent> = runCatching {
    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
        ContentUris.appendId(it, nowMs)
        ContentUris.appendId(it, nowMs + HORIZON_MS)
    }.build()
    val projection = arrayOf(
        CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END,
        CalendarContract.Instances.ALL_DAY, CalendarContract.Instances.EVENT_LOCATION
    )
    val events = ArrayList<AgendaEvent>()
    context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
        while (cursor.moveToNext() && events.size < limit) {
            events += AgendaEvent(
                title = cursor.getString(0).orEmpty().ifBlank { "(No title)" },
                startMs = cursor.getLong(1),
                endMs = cursor.getLong(2),
                allDay = cursor.getInt(3) == 1,
                location = cursor.getString(4).orEmpty()
            )
        }
    }
    events.toList()
}.getOrDefault(emptyList())

@Composable
fun AgendaWidget(use12Hour: Boolean, isDayMode: Boolean, isEditing: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var permitted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)
    }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permitted = it }
    var events by remember { mutableStateOf<List<AgendaEvent>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(permitted) {
        if (!permitted) return@LaunchedEffect
        while (true) {
            events = withContext(Dispatchers.IO) { loadAgenda(context, System.currentTimeMillis()) }
            loaded = true
            delay(5 * 60_000L)
        }
    }
    val color = if (isDayMode) Color(0xFF111111) else MaterialTheme.colorScheme.onBackground
    val dim = color.copy(alpha = 0.6f)
    Column(modifier.padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when {
            !permitted -> {
                Text("Show today's events from this device's calendar.", fontSize = 12.sp, color = dim)
                TextButton(onClick = { request.launch(Manifest.permission.READ_CALENDAR) }, enabled = !isEditing,
                    modifier = Modifier.heightIn(min = 48.dp)) { Text("Allow calendar access") }
            }
            loaded && events.isEmpty() -> Text("No events in the next day and a half.", fontSize = 13.sp, color = dim)
            else -> {
                val now = System.currentTimeMillis()
                events.forEach { event ->
                    Row(Modifier.fillMaxWidth().clickable(enabled = !isEditing) { openCalendar(context) }.padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(agendaTimeLabel(event, now, use12Hour), Modifier.width(88.dp), fontSize = 12.sp, color = dim, maxLines = 1)
                        Column {
                            Text(event.title, fontSize = 15.sp, color = color, maxLines = 1)
                            if (event.location.isNotBlank()) Text(event.location, fontSize = 11.sp, color = dim, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

private fun openCalendar(context: Context) {
    val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
