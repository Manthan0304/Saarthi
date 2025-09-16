package com.example.saarthi.Screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.saarthi.services.FamilyTrackingService
import com.example.saarthi.services.FirestoreRepo
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.compose.*
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun FamilyScreen() {
    val context = LocalContext.current
    val repo = remember { FirestoreRepo(context) }
    var isSharing by remember { mutableStateOf(false) }
    var members by remember { mutableStateOf(listOf<com.example.saarthi.services.MemberLive>()) }
    var listener by remember { mutableStateOf<com.google.firebase.firestore.ListenerRegistration?>(null) }
    var showMap by remember { mutableStateOf(true) }
    var showAllowlist by remember { mutableStateOf(false) }
    val Orange = Color(0xFFFF9800)
    DisposableEffect(true) {
        listener = repo.subscribeActiveMembers { list -> members = list }
        onDispose { listener?.remove() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Family Location Sharing",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Share your location with family members",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        // Main Control Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Sharing Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isSharing) "📍 Sharing Location" else "📍 Not Sharing",
                            color = if (isSharing) Color.Green else Orange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isSharing) "Others can see your location" else "Your location is private",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = {
                            if (!isSharing) {
                                context.startForegroundService(Intent(context, FamilyTrackingService::class.java).apply { action = FamilyTrackingService.ACTION_START })
                            } else {
                                context.startService(Intent(context, FamilyTrackingService::class.java).apply { action = FamilyTrackingService.ACTION_STOP })
                            }
                            isSharing = !isSharing
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSharing) Color(0xFFFF5722) else Color(0xFF4CAF50)
                        )
                    ) { 
                        Text(if (isSharing) "Stop" else "Start Sharing") 
                    }
                }

                // Active Members Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👥 Active Members: ${members.size}",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    if (members.isNotEmpty()) {
                        Text(
                            text = "Tap to view",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showMap = !showMap },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (showMap) "📋 List View" else "🗺️ Map View")
                    }
                    Button(
                        onClick = { showAllowlist = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⚙️ Manage")
                    }
                }
                
            }
        }

        if (showMap) {
            // Google Map
            Box(modifier = Modifier.weight(1f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    properties = MapProperties(isMyLocationEnabled = true),
                    uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true)
                ) {
                    // Add markers for all members with location data
                    members.filter { it.lat != null && it.lng != null }.forEach { member ->
                        Marker(
                            state = MarkerState(position = LatLng(member.lat!!, member.lng!!)),
                            title = member.phone ?: member.displayName ?: member.uid,
                            snippet = "Name: ${member.displayName ?: "Unknown"}"
                        )
                    }
                }
            }
        } else {
            // List View
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Current User: ${repo.currentUid()}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                
                if (members.isEmpty()) {
                    Text(
                        text = "No active members sharing location",
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                        members.forEach { m ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Phone number as primary identifier
                                    Text(
                                        text = m.phone ?: "Unknown Phone",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    // Display name as secondary
                                    if (m.displayName != null && m.displayName != m.uid) {
                                        Text(
                                            text = "Name: ${m.displayName}",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                    // Location status
                                    val locationText = if (m.lat != null && m.lng != null) {
                                        "📍 Location: ${String.format("%.4f", m.lat)}, ${String.format("%.4f", m.lng)}"
                                    } else {
                                        "📍 No location yet"
                                    }
                                    Text(
                                        text = locationText,
                                        color = if (m.lat != null && m.lng != null) Color.Green else Orange,
                                        fontSize = 12.sp
                                    )
                                    // UID for debugging
                                    Text(
                                        text = "ID: ${m.uid.take(8)}...",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                }
            }
        }
    }

    // Show allowlist screen
    if (showAllowlist) {
        Box(modifier = Modifier.fillMaxSize()) {
            AllowlistScreen(
                onBack = { showAllowlist = false }
            )
        }
    }
}
