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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saarthi.services.FirestoreRepo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions

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
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF666666))
            ) {
                Text("← Back")
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = "Manage Sharing",
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
                    text = "👥 Who can see your location?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Add phone numbers of family members you want to share your location with. Both of you need to add each other to see locations.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Add button
        Button(
            onClick = { showAddDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(Modifier.width(8.dp))
            Text("Add Family Member")
        }

        Spacer(Modifier.height(16.dp))

        // Allowlist
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📱 Family Members (${allowlist.size})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            if (allowlist.isNotEmpty()) {
                Text(
                    text = "Tap to remove",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (allowlist.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "👤",
                        fontSize = 48.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "No family members added yet",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Add phone numbers to start sharing your location with family members.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allowlist) { phone ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
                        onClick = {
                            repo.removeFromAllowlist(phone)
                            allowlist = allowlist.filter { it != phone }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📱",
                                fontSize = 20.sp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = phone,
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Add phone dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                newPhone = ""
            },
            title = { 
                Text(
                    text = "Add Family Member",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column {
                    Text(
                        text = "Enter the phone number of the family member you want to share your location with.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+1234567890") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Format: +1234567890 (include country code)",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPhone.isNotBlank() && newPhone.startsWith("+")) {
                            repo.addToAllowlist(newPhone)
                            allowlist = allowlist + newPhone
                            newPhone = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    enabled = newPhone.isNotBlank() && newPhone.startsWith("+")
                ) {
                    Text("Add Member")
                }
            },
            dismissButton = {
                Button(
                    onClick = { 
                        showAddDialog = false
                        newPhone = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF666666))
                ) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF2C2C2C)
        )
    }
}
