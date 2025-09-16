package com.example.saarthi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.saarthi.Screens.MapScreen
import com.example.saarthi.Screens.RoutesListScreen
import com.example.saarthi.Screens.FamilyScreen
import com.example.saarthi.Screens.SettingsScreen
import com.example.saarthi.Screens.RouteNamingScreen
import com.example.saarthi.data.Route
import com.example.saarthi.ui.theme.SaarthiTheme
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback

class MainActivity : ComponentActivity(), OnMapsSdkInitializedCallback {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Google Maps SDK
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST, this)

        requestLocationPermission()

        setContent {
            SaarthiTheme {
                var selectedRoute by remember { mutableStateOf<Route?>(null) }
                
                MainScreen(selectedRoute = selectedRoute, onRouteSelected = { selectedRoute = it })
            }
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                // Permission denied
            }
        }
    }

    override fun onMapsSdkInitialized(renderer: com.google.android.gms.maps.MapsInitializer.Renderer) {
        // Maps SDK initialized successfully
    }
}

@Composable
fun MainScreen(
    selectedRoute: Route?,
    onRouteSelected: (Route?) -> Unit
) {
    var currentScreen by remember { mutableStateOf("home") }
    var recordedRoute by remember { mutableStateOf<Route?>(null) }
    
    val screens = listOf(
        "home" to "Home",
        "saved" to "Saved", 
        "family" to "Family",
        "settings" to "Settings"
    )
    
    Scaffold(
        bottomBar = {
            if (currentScreen != "route_naming") {
                NavigationBar(
                    containerColor = Color(0xFF1A1A1A),
                    contentColor = Color.White
                ) {
                    screens.forEach { (screen, label) ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = when (screen) {
                                        "home" -> Icons.Default.Home
                                        "saved" -> Icons.Default.Bookmark
                                        "family" -> Icons.Default.People
                                        "settings" -> Icons.Default.Settings
                                        else -> Icons.Default.Home
                                    },
                                    contentDescription = label,
                                    tint = if (currentScreen == screen) Color(0xFF4CAF50) else Color.White
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    color = if (currentScreen == screen) Color(0xFF4CAF50) else Color.White
                                )
                            },
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        when (currentScreen) {
            "home" -> {
                MapScreen(
                    onShowRoutes = { currentScreen = "saved" },
                    selectedRoute = selectedRoute,
                    onRouteRecorded = { route ->
                        recordedRoute = route
                        currentScreen = "route_naming"
                    }
                )
            }
            "saved" -> {
                RoutesListScreen(
                    onRouteSelected = { route ->
                        onRouteSelected(route)
                        currentScreen = "home"
                    },
                    onBackPressed = { 
                        onRouteSelected(null)
                        currentScreen = "home" 
                    }
                )
            }
            "family" -> {
                FamilyScreen()
            }
            "settings" -> {
                SettingsScreen()
            }
            "route_naming" -> {
                recordedRoute?.let { route ->
                    RouteNamingScreen(
                        recordedRoute = route,
                        onSaveRoute = { routeName ->
                            // Here you would save the route with the name
                            // For now, we'll just go back to home
                            recordedRoute = null
                            currentScreen = "home"
                        },
                        onCancel = {
                            recordedRoute = null
                            currentScreen = "home"
                        }
                    )
                }
            }
        }
    }
}
