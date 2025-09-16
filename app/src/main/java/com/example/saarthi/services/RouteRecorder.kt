package com.example.saarthi.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.example.saarthi.data.Route
import com.example.saarthi.data.RoutePoint
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.LocalDateTime
import java.util.*
import kotlin.math.abs

class RouteRecorder(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null
    private var isRecording = false
    private var isPaused = false
    private var currentRoute: Route? = null
    private val routePoints = mutableListOf<RoutePoint>()
    private var startTime: Long = 0L
    private var currentDistance = 0.0 // in meters
    private var lastLocation: Location? = null
    
    // Location request configuration
    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000) // 5 seconds interval
        .setMinUpdateDistanceMeters(10f) // Only update if moved 10 meters
        .setMinUpdateIntervalMillis(3000) // Minimum 3 seconds between updates
        .build()
    
    fun startRecording(routeName: String = "Route ${LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}"): Route {
        if (isRecording) {
            throw IllegalStateException("Already recording a route")
        }
        
        val routeId = UUID.randomUUID().toString()
        val startTime = LocalDateTime.now()
        
        currentRoute = Route(
            id = routeId,
            name = routeName,
            startTime = startTime,
            points = emptyList()
        )
        
        routePoints.clear()
        isRecording = true
        isPaused = false
        this.startTime = System.currentTimeMillis()
        currentDistance = 0.0
        lastLocation = null
        
        startLocationUpdates()
        
        return currentRoute!!
    }
    
    fun stopRecording(): Route? {
        if (!isRecording || currentRoute == null) {
            return null
        }
        
        stopLocationUpdates()
        
        val endTime = LocalDateTime.now()
        val duration = java.time.Duration.between(currentRoute!!.startTime, endTime).seconds
        
        val completedRoute = currentRoute!!.copy(
            endTime = endTime,
            points = routePoints.toList(),
            totalDistance = currentDistance,
            duration = duration
        )
        
        // Save route locally
        saveRouteLocally(completedRoute)
        
        isRecording = false
        isPaused = false
        currentRoute = null
        routePoints.clear()
        startTime = 0L
        currentDistance = 0.0
        lastLocation = null
        
        return completedRoute
    }

    fun pauseRecording() {
        if (isRecording && !isPaused) {
            isPaused = true
            stopLocationUpdates()
        }
    }

    fun resumeRecording() {
        if (isRecording && isPaused) {
            isPaused = false
            startLocationUpdates()
        }
    }

    fun getCurrentDistance(): Double {
        return currentDistance * 0.000621371 // Convert meters to miles
    }

    fun getElapsedTime(): Long {
        return if (startTime > 0) System.currentTimeMillis() - startTime else 0L
    }
    
    fun isRecording(): Boolean = isRecording
    
    fun getCurrentRoute(): Route? = currentRoute
    
    fun getRoutePoints(): List<RoutePoint> = routePoints.toList()
    
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    if (isRecording) {
                        addRoutePoint(location)
                    }
                }
            }
        }
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            context.mainExecutor,
            locationCallback!!
        )
    }
    
    private fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            locationCallback = null
        }
    }
    
    private fun addRoutePoint(location: Location) {
        val routePoint = RoutePoint(
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = LocalDateTime.now(),
            accuracy = location.accuracy,
            speed = location.speed
        )
        
        // Calculate distance from last location
        if (lastLocation != null) {
            val distance = lastLocation!!.distanceTo(location)
            currentDistance += distance
        }
        lastLocation = location
        
        // Only add point if it's significantly different from the last point
        if (routePoints.isEmpty() || isSignificantMovement(routePoints.last(), routePoint)) {
            routePoints.add(routePoint)
        }
    }
    
    private fun isSignificantMovement(lastPoint: RoutePoint, newPoint: RoutePoint): Boolean {
        val distance = calculateDistance(lastPoint, newPoint)
        return distance > 5.0 // Only record if moved more than 5 meters
    }
    
    private fun calculateDistance(point1: RoutePoint, point2: RoutePoint): Double {
        val results = FloatArray(1)
        Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0].toDouble()
    }
    
    private fun calculateTotalDistance(points: List<RoutePoint>): Double {
        if (points.size < 2) return 0.0
        
        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            totalDistance += calculateDistance(points[i], points[i + 1])
        }
        return totalDistance
    }
    
    private fun saveRouteLocally(route: Route) {
        // For now, we'll use SharedPreferences to store routes
        // In a real app, you'd use Room database or similar
        val sharedPrefs = context.getSharedPreferences("routes", Context.MODE_PRIVATE)
        val routesJson = sharedPrefs.getString("saved_routes", "[]")
        
        // Simple JSON storage (in production, use proper JSON library)
        // This is a simplified version - you might want to use Gson or Moshi
        val updatedRoutes = routesJson + "," + route.id
        sharedPrefs.edit().putString("saved_routes", updatedRoutes).apply()
        
        // Store individual route data
        val editor = sharedPrefs.edit()
        editor.putString("route_${route.id}_name", route.name)
        editor.putString("route_${route.id}_start", route.startTime.toString())
        editor.putString("route_${route.id}_end", route.endTime?.toString() ?: "")
        editor.putFloat("route_${route.id}_distance", route.totalDistance.toFloat())
        editor.putLong("route_${route.id}_duration", route.duration)
        
        // Store route points as a simple string format
        val pointsString = route.points.joinToString("|") { point ->
            "${point.latitude},${point.longitude},${point.timestamp}"
        }
        editor.putString("route_${route.id}_points", pointsString)
        
        editor.apply()
    }
    
    fun getSavedRoutes(): List<Route> {
        val sharedPrefs = context.getSharedPreferences("routes", Context.MODE_PRIVATE)
        val routesJson = sharedPrefs.getString("saved_routes", "[]") ?: "[]"
        
        if (routesJson == "[]") return emptyList()
        
        val routeIds = routesJson.split(",").filter { it.isNotEmpty() }
        return routeIds.mapNotNull { routeId ->
            try {
                val pointsString = sharedPrefs.getString("route_${routeId}_points", "")
                val points = if (pointsString.isNullOrEmpty()) {
                    emptyList()
                } else {
                    pointsString.split("|").mapNotNull { pointString ->
                        try {
                            val parts = pointString.split(",")
                            if (parts.size >= 3) {
                                RoutePoint(
                                    latitude = parts[0].toDouble(),
                                    longitude = parts[1].toDouble(),
                                    timestamp = LocalDateTime.parse(parts[2])
                                )
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                
                Route(
                    id = routeId,
                    name = sharedPrefs.getString("route_${routeId}_name", "Unknown Route") ?: "Unknown Route",
                    startTime = LocalDateTime.parse(sharedPrefs.getString("route_${routeId}_start", "") ?: ""),
                    endTime = sharedPrefs.getString("route_${routeId}_end", "")?.let { 
                        if (it.isNotEmpty()) LocalDateTime.parse(it) else null 
                    },
                    points = points,
                    totalDistance = sharedPrefs.getFloat("route_${routeId}_distance", 0f).toDouble(),
                    duration = sharedPrefs.getLong("route_${routeId}_duration", 0)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
