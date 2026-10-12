package com.openlauncher.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.openlauncher.app.data.TripRecord
import com.openlauncher.app.data.tripStats
import com.openlauncher.app.data.weeklyDistance

private const val DAY_MS = 86_400_000L
private val PERIODS = listOf("7 days" to 7, "30 days" to 30, "All" to 0)

internal fun distanceText(meters: Double, metric: Boolean): String =
    "%.1f %s".format(meters / (if (metric) 1000.0 else 1609.344), if (metric) "km" else "mi")

internal fun durationText(seconds: Double): String {
    val total = seconds.toLong()
    return "%d:%02d h".format(total / 3600, total / 60 % 60)
}

/** Totals and a weekly distance chart over saved trips. Built from stored totals only; no route is kept. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TripInsightsPanel(history: List<TripRecord>, metric: Boolean) {
    var period by remember { mutableIntStateOf(1) }
    val days = PERIODS[period].second
    val now = System.currentTimeMillis()
    val stats = tripStats(history, if (days == 0) 0L else now - days * DAY_MS)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Insights", style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PERIODS.forEachIndexed { index, (label, _) ->
                FilterChip(selected = period == index, onClick = { period = index }, label = { Text(label) })
            }
        }
        if (stats.trips == 0) {
            Text("No saved trips in this period.")
        } else {
            val speed = stats.averageMps * if (metric) 3.6 else 2.236936
            Text("${stats.trips} trips · ${distanceText(stats.totalMeters, metric)} · ${durationText(stats.driveSeconds + stats.idleSeconds)}")
            Text("Average ${"%.1f".format(speed)} ${if (metric) "km/h" else "mph"} while moving · ${(stats.idleShare * 100).toInt()}% of time stopped")
            Text("Average trip ${distanceText(stats.averageMetersPerTrip, metric)}" +
                (stats.longest?.let { " · longest ${distanceText(it.distanceMeters, metric)}" } ?: ""))
        }
        val weeks = weeklyDistance(history, now, 8)
        val peak = (weeks.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
        Text("Distance per week, last 8 weeks", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
            weeks.forEach { meters ->
                Box(Modifier.weight(1f).height((72 * (meters / peak)).coerceAtLeast(2.0).dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = if (meters > 0) 0.9f else 0.25f)))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("8 weeks ago", style = MaterialTheme.typography.bodySmall)
            Text("This week · ${distanceText(weeks.last(), metric)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
