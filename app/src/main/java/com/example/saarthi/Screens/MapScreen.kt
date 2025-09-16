package com.example.saarthi.Screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.LocationOn
import androidx.core.app.ActivityCompat
import com.example.saarthi.data.Route
import com.example.saarthi.data.RoutePoint
import com.example.saarthi.services.RouteRecorder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

// Helper function for time formatting
fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onShowRoutes: () -> Unit = {},
    selectedRoute: Route? = null,
    onRouteRecorded: (Route, Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    // Route recorder
    val routeRecorder = remember { RouteRecorder(context) }

    // State variables
    var userLocation by remember {
        mutableStateOf(
            LatLng(
                16.46321,
                80.5064
            )
        )
    } // default: Vijayawada
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var routePoints by remember { mutableStateOf<List<RoutePoint>>(emptyList()) }
    var currentDistance by remember { mutableStateOf(0.0) } // in miles
    var elapsedTime by remember { mutableStateOf(0L) } // in milliseconds
    var isContinuing by remember { mutableStateOf(false) }
    var basePointCount by remember { mutableStateOf(0) }

    var mapProperties by remember {
        mutableStateOf(MapProperties(isMyLocationEnabled = true))
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 15f)
    }

    val coroutineScope = rememberCoroutineScope()

    // Fetch current location once
    LaunchedEffect(true) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val newLocation = LatLng(it.latitude, it.longitude)
                    userLocation = newLocation
                    // Animate camera inside coroutine with safety check
                    coroutineScope.launch {
                        try {
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngZoom(newLocation, 15f),
                                durationMs = 1000
                            )
                        } catch (e: Exception) {
                            cameraPositionState.position =
                                CameraPosition.fromLatLngZoom(newLocation, 15f)
                        }
                    }
                }
            }
        }
    }

    // When a saved route is selected, move camera to fit it
    LaunchedEffect(selectedRoute) {
        selectedRoute?.let { route ->
            if (route.points.size >= 2) {
                val builder = LatLngBounds.Builder()
                route.points.forEach { p -> builder.include(p.toLatLng()) }
                val bounds = builder.build()
                coroutineScope.launch {
                    try {
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngBounds(bounds, 80),
                            durationMs = 800
                        )
                    } catch (_: Exception) {
                        // ignore if map not ready for bounds; fallback to first point
                        val first = route.points.first().toLatLng()
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(first, 15f)
                    }
                }
            }
        }
    }

    // Update route points when recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                routePoints = routeRecorder.getRoutePoints()
                kotlinx.coroutines.delay(1000) // Update every second
            }
        }
    }

    // Update distance and time during recording
    LaunchedEffect(isRecording, isPaused) {
        if (isRecording && !isPaused) {
            while (isRecording && !isPaused) {
                currentDistance = routeRecorder.getCurrentDistance()
                elapsedTime = routeRecorder.getElapsedTime()
                kotlinx.coroutines.delay(1000) // Update every second
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TrailTrek",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            IconButton(onClick = { /* Menu action */ }) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.Black
                )
            }
        }

        if (!isRecording) {
            // Map section (half screen)
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f), // Takes up half the remaining space
                properties = mapProperties,
                cameraPositionState = cameraPositionState
            ) {
                // Draw selected saved route polyline
                selectedRoute?.let { route ->
                    if (route.points.size >= 2) {
                        val savedRoutePoints = route.points.map { it.toLatLng() }
                        Polyline(
                            points = savedRoutePoints,
                            color = Color.Red,
                            width = 6f
                        )
                    }
                }
            }

            // Bottom half with action buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f), // Takes up the other half
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedRoute != null) {
                        Button(
                            onClick = {
                                try {
                                    routeRecorder.continueRecording(selectedRoute)
                                    isRecording = true
                                    isPaused = false
                                    currentDistance = selectedRoute.totalDistance * 0.000621371
                                    elapsedTime = 0L // we only show elapsed session time here
                                    isContinuing = true
                                    basePointCount = routeRecorder.getInitialExistingPointCount()
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text(
                                text = "Continue Recording",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    routeRecorder.startRecording()
                                    isRecording = true
                                    isPaused = false
                                    currentDistance = 0.0
                                    elapsedTime = 0L
                                    isContinuing = false
                                    basePointCount = 0
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Start New Recording",
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                try {
                                    routeRecorder.startRecording()
                                    isRecording = true
                                    isPaused = false
                                    currentDistance = 0.0
                                    elapsedTime = 0L
                                    isContinuing = false
                                    basePointCount = 0
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text(
                                text = "Start Recording",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // Recording state: Full screen with overlays
            Box(
                modifier = Modifier.weight(1f)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    properties = mapProperties,
                    cameraPositionState = cameraPositionState
                ) {
                    // Draw recorded route polyline (current recording)
                    if (routePoints.size >= 2) {
                        if (isContinuing && basePointCount >= 2 && basePointCount < routePoints.size) {
                            val oldPoints = routePoints.take(basePointCount).map { it.toLatLng() }
                            val newPoints = routePoints.drop(basePointCount - 1).map { it.toLatLng() }
                            Polyline(
                                points = oldPoints,
                                color = Color.Red,
                                width = 6f
                            )
                            Polyline(
                                points = newPoints,
                                color = Color.Blue,
                                width = 8f
                            )
                        } else {
                            val polylinePoints = routePoints.map { it.toLatLng() }
                            Polyline(
                                points = polylinePoints,
                                color = Color.Blue,
                                width = 8f
                            )
                        }
                    }

                    // Draw selected saved route polyline
                    selectedRoute?.let { route ->
                        if (route.points.size >= 2) {
                            val savedRoutePoints = route.points.map { it.toLatLng() }
                            Polyline(
                                points = savedRoutePoints,
                                color = Color.Red,
                                width = 6f
                            )
                        }
                    }
                }

                // Top Info Bar (Distance, Time)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A1A).copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Distance: ${String.format("%.1f", currentDistance)} mi",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Time: ${formatTime(elapsedTime)}",
                            fontSize = 14.sp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(start = 32.dp)
                        )
                    }
                }

                // Save Route Button (above bottom controls)
                Button(
                    onClick = {
                        val completedRoute = routeRecorder.stopRecording()
                        isRecording = false
                        isPaused = false
                        completedRoute?.let { route ->
                            onRouteRecorded(route, !isContinuing)
                        }
                        isContinuing = false
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 100.dp), // Position above bottom control bar
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Save Route",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bottom Control Bar (Start, Pause, Stop)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 80.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A1A).copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Start Button (only visible if paused)
                        FloatingActionButton(
                            onClick = {
                                if (isPaused) {
                                    routeRecorder.resumeRecording()
                                    isPaused = false
                                }
                            },
                            containerColor = if (isPaused) Color(0xFF4CAF50) else Color.Gray,
                            contentColor = Color.Black,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Start",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Pause Button
                        FloatingActionButton(
                            onClick = {
                                if (isRecording && !isPaused) {
                                    routeRecorder.pauseRecording()
                                    isPaused = true
                                }
                            },
                            containerColor = if (isRecording && !isPaused) Color.LightGray else Color.Gray,
                            contentColor = Color.Black,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.Default.Pause,
                                contentDescription = "Pause",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Stop Button
                        FloatingActionButton(
                            onClick = {
                                val completedRoute = routeRecorder.stopRecording()
                                isRecording = false
                                isPaused = false
                                completedRoute?.let { route ->
                                    onRouteRecorded(route, !isContinuing)
                                }
                                isContinuing = false
                            },
                            containerColor = Color.Red,
                            contentColor = Color.White,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}