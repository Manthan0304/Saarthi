package com.example.saarthi.Screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SettingsScreen(
    onOpenPhoneAuth: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val isSignedIn = currentUser != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // User Info Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Account",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))
                
                if (isSignedIn) {
                    Text(
                        text = "Signed in as:",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = currentUser.phoneNumber ?: "Unknown",
                        color = Color.Green,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                } else {
                    Text(
                        text = "Not signed in",
                        color = Color.Orange,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onOpenPhoneAuth,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Sign In")
                        Spacer(Modifier.width(8.dp))
                        Text("Sign In with Phone")
                    }
                }
            }
        }

        // App Settings Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "App Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))
                
                SettingsItem(
                    icon = Icons.Default.LocationOn,
                    title = "Location Services",
                    subtitle = "Manage location permissions",
                    onClick = { /* TODO: Open location settings */ }
                )
                
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Control app notifications",
                    onClick = { /* TODO: Open notification settings */ }
                )
                
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "Privacy & Security",
                    subtitle = "Manage your privacy settings",
                    onClick = { /* TODO: Open privacy settings */ }
                )
            }
        }

        // Family Settings Card
        if (isSignedIn) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Family & Sharing",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    SettingsItem(
                        icon = Icons.Default.People,
                        title = "Manage Family",
                        subtitle = "Add or remove family members",
                        onClick = { /* TODO: Open family management */ }
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.Share,
                        title = "Location Sharing",
                        subtitle = "Control who can see your location",
                        onClick = { /* TODO: Open sharing settings */ }
                    )
                }
            }
        }

        // About Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "About",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))
                
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "App Version",
                    subtitle = "1.0.0",
                    onClick = { }
                )
                
                SettingsItem(
                    icon = Icons.Default.Help,
                    title = "Help & Support",
                    subtitle = "Get help with the app",
                    onClick = { /* TODO: Open help */ }
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Go",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
