package com.openlauncher.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MapHealth(val status: String = "Map has not opened in this session", val updatedAtMs: Long = 0)
object DashboardDiagnostics {
    private val _map = MutableStateFlow(MapHealth())
    val map: StateFlow<MapHealth> = _map
    fun mapStatus(status: String) { _map.value = MapHealth(status, System.currentTimeMillis()) }
}
