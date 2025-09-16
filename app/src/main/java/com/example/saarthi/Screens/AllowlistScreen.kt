package com.example.saarthi.Screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saarthi.services.FirestoreRepo
import androidx.compose.ui.platform.LocalContext

@Composable
fun AllowlistScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { FirestoreRepo(context) }
    var allowlist by remember { mutableStateOf<List<String>>(emptyList()) }
    var newPhone by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    // Load allowlist on screen open
    LaunchedEffect(Unit) {
        repo.getAllowlist { list ->
            allowlist = list
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = onBack) {
                Text("← Back")
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = "Location Sharing",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(Modifier.height(16.dp))

        // Info card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Who can see your location?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Add phone numbers of people you want to share your location with. They will only see your location if you're both sharing.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Add button
        Button(
            onClick = { showAddDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(Modifier.width(8.dp))
            Text("Add Phone Number")
        }

        Spacer(Modifier.height(16.dp))

        // Allowlist
        Text(
            text = "Allowed Phone Numbers (${allowlist.size})",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        if (allowlist.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
            ) {
                Text(
                    text = "No phone numbers added yet.\nAdd phone numbers to start sharing your location.",
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allowlist) { phone ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = phone,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    repo.removeFromAllowlist(phone)
                                    allowlist = allowlist.filter { it != phone }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add phone dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Phone Number") },
            text = {
                Column {
                    Text("Enter the phone number you want to share your location with:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+1234567890") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPhone.isNotBlank()) {
                            repo.addToAllowlist(newPhone)
                            allowlist = allowlist + newPhone
                            newPhone = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
