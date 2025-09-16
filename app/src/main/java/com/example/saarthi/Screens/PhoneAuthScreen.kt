package com.example.saarthi.Screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.saarthi.services.AuthManager
import com.example.saarthi.services.FirestoreRepo
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

@Composable
fun PhoneAuthScreen(
    activity: androidx.activity.ComponentActivity,
    onSignedIn: () -> Unit
) {
    val context = LocalContext.current
    val authManager = remember { AuthManager(activity) }
    val firestoreRepo = remember { FirestoreRepo(context) }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone number") })

        Button(onClick = {
            status = "Sending code..."
            authManager.startPhoneVerification(
                phoneNumber = phone,
                onCodeSent = { vid ->
                    verificationId = vid
                    status = "Code sent"
                },
                onVerificationCompleted = { credential ->
                    signIn(credential, authManager, firestoreRepo, onSignedIn) { e -> status = e.message ?: "Error" }
                },
                onVerificationFailed = { e -> status = e.message ?: "Failed" }
            )
        }) { Text("Send Code") }

        if (verificationId != null) {
            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("OTP") })
            Button(onClick = {
                val cred = PhoneAuthProvider.getCredential(verificationId!!, code)
                signIn(cred, authManager, firestoreRepo, onSignedIn) { e -> status = e.message ?: "Error" }
            }) { Text("Verify & Sign In") }
        }

        Text(text = status)
    }
}

private fun signIn(
    credential: PhoneAuthCredential,
    authManager: AuthManager,
    firestoreRepo: FirestoreRepo,
    onSignedIn: () -> Unit,
    onError: (Exception) -> Unit
) {
    authManager.signInWithCredential(credential, 
        onSuccess = { 
            // Create user profile in Firestore after successful sign-in
            firestoreRepo.upsertProfile()
            onSignedIn() 
        }, 
        onError = onError
    )
}


