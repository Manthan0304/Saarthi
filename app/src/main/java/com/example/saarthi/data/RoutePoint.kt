package com.example.saarthi.data

import com.google.android.gms.maps.model.LatLng
import java.time.LocalDateTime

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: LocalDateTime,
    val accuracy: Float? = null,
    val speed: Float? = null
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}

data class Route(
    val id: String,
    val name: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime? = null,
    val points: List<RoutePoint> = emptyList(),
    val totalDistance: Double = 0.0,
    val duration: Long = 0 // in seconds
)
