package com.openlauncher.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Only aggregate totals are saved, never a route or coordinates. */
data class TripRecord(
    val startedAtMs: Long = 0,
    val endedAtMs: Long = 0,
    val distanceMeters: Double = 0.0,
    val driveSeconds: Double = 0.0,
    val idleSeconds: Double = 0.0,
    val running: Boolean = false
) {
    val averageMps: Double get() = if (driveSeconds > 0) distanceMeters / driveSeconds else 0.0
}
data class TripLog(val current: TripRecord = TripRecord(), val history: List<TripRecord> = emptyList())

fun advanceTrip(trip: TripRecord, speedMps: Float?, fixAgeMs: Long, accuracy: Float, elapsedSeconds: Double): TripRecord {
    if (!trip.running || elapsedSeconds !in 0.0..5.0 || fixAgeMs !in 0..5000 ||
        speedMps == null || !speedMps.isFinite() || speedMps !in 0f..100f || accuracy !in 0f..50f) return trip
    return if (speedMps > 0.5f) trip.copy(distanceMeters = trip.distanceMeters + speedMps * elapsedSeconds,
        driveSeconds = trip.driveSeconds + elapsedSeconds)
    else trip.copy(idleSeconds = trip.idleSeconds + elapsedSeconds)
}
fun completeTrip(log: TripLog, now: Long): TripLog = if (log.current.startedAtMs == 0L) log else
    TripLog(history = (listOf(log.current.copy(running = false, endedAtMs = now)) + log.history).take(200))

fun tripsCsv(trips: List<TripRecord>): String = "started_epoch_ms,ended_epoch_ms,distance_meters,drive_seconds,idle_seconds,average_mps\n" +
    trips.joinToString("\n") { "${it.startedAtMs},${it.endedAtMs},${it.distanceMeters},${it.driveSeconds},${it.idleSeconds},${it.averageMps}" }

private val Context.tripStore by preferencesDataStore("trip_history")
class TripRepository(private val context: Context) {
    private val key = stringPreferencesKey("log")
    private val gson = Gson()
    suspend fun load(): TripLog = context.tripStore.data.map { prefs ->
        prefs[key]?.let { json -> requireNotNull(gson.fromJson(json, TripLog::class.java)) { "Invalid trip history" } } ?: TripLog()
    }.first().let { it.copy(current = it.current.copy(running = false)) }
    suspend fun save(log: TripLog) { context.tripStore.edit { it[key] = gson.toJson(log) } }
}
