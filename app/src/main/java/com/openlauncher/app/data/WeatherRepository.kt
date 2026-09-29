package com.openlauncher.app.data

import android.content.Context
import android.util.AtomicFile
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.openlauncher.app.model.WeatherState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Stores conditions and forecast only. Coordinates and saved destinations are never cached. */
class WeatherRepository(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "weather-cache.json"))

    suspend fun load(nowMs: Long = System.currentTimeMillis()): WeatherState? = withContext(Dispatchers.IO) {
        runCatching {
            file.openRead().use { input ->
                val bytes = ByteArray(64_001)
                var size = 0
                while (size < bytes.size) {
                    val count = input.read(bytes, size, bytes.size - size)
                    if (count < 0) break
                    size += count
                }
                if (size > 64_000) null else WeatherCache.decode(String(bytes, 0, size, Charsets.UTF_8), nowMs)
            }
        }.getOrNull()
    }

    suspend fun save(weather: WeatherState) = withContext(Dispatchers.IO) {
        val bytes = WeatherCache.encode(weather).toByteArray(Charsets.UTF_8)
        val output = file.startWrite()
        try {
            output.write(bytes)
            file.finishWrite(output)
        } catch (e: Exception) {
            file.failWrite(output)
            throw e
        }
    }
}

internal object WeatherCache {
    const val MAX_AGE_MS = 48 * 60 * 60 * 1000L
    private val gson = Gson()

    fun encode(weather: WeatherState): String = gson.toJson(JsonObject().apply {
        addProperty("version", 1)
        add("weather", gson.toJsonTree(weather.copy(hourly = weather.hourly.take(48))))
    })

    fun decode(json: String, nowMs: Long): WeatherState? = runCatching {
        require(json.length <= 64_000)
        val root = JsonParser.parseString(json).asJsonObject
        require(root["version"].asInt == 1)
        val data = root.getAsJsonObject("weather")
        require(listOf("temperatureCelsius", "weatherCode", "windspeedKmh", "updatedAtMs", "isDay", "hourly")
            .all { data.has(it) && !data[it].isJsonNull })
        val weather = gson.fromJson(data, WeatherState::class.java)
        require(weather.updatedAtMs > 0 && nowMs - weather.updatedAtMs in -300_000..MAX_AGE_MS)
        require(weather.temperatureCelsius in -100.0..70.0 && weather.windspeedKmh in 0.0..500.0)
        require(weather.highCelsius?.isFinite() != false && weather.lowCelsius?.isFinite() != false)
        weather.copy(hourly = weather.hourly.filter {
            it.timeMs > 0 && it.celsius?.isFinite() != false && it.feelsLikeCelsius?.isFinite() != false &&
                (it.rainPercent == null || it.rainPercent in 0..100)
        }.take(48))
    }.getOrNull()
}
