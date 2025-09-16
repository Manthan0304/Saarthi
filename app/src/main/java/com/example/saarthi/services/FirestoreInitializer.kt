package com.example.saarthi.services

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Initializes Firestore collections and documents automatically
 * This ensures the database structure is created when the app starts
 */
class FirestoreInitializer(private val context: Context) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    fun initializeDefaultFamily() {
        // Create the default family document
        val defaultFamilyData = hashMapOf(
            "name" to "Default Family",
            "createdAt" to com.google.firebase.Timestamp.now(),
            "description" to "Default family group for location sharing"
        )
        
        db.collection("families").document("default_family")
            .set(defaultFamilyData, SetOptions.merge())
            .addOnSuccessListener {
                android.util.Log.d("FirestoreInit", "Default family initialized")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FirestoreInit", "Failed to initialize default family", e)
            }
    }
    
    fun initializeUserProfile(uid: String, phoneNumber: String?, displayName: String? = null) {
        val profileData = hashMapOf(
            "uid" to uid,
            "phone" to (phoneNumber ?: ""),
            "displayName" to (displayName ?: "User"),
            "familyId" to "default_family",
            "createdAt" to com.google.firebase.Timestamp.now(),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        
        // Create profile in families/default_family/profiles/{uid}
        db.collection("families").document("default_family")
            .collection("profiles").document(uid)
            .set(profileData, SetOptions.merge())
            .addOnSuccessListener {
                android.util.Log.d("FirestoreInit", "User profile initialized for $uid")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FirestoreInit", "Failed to initialize user profile", e)
            }
            
        // Initialize member document for live location sharing
        val memberData = hashMapOf(
            "sharing" to false,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        
        db.collection("families").document("default_family")
            .collection("members").document(uid)
            .set(memberData, SetOptions.merge())
            .addOnSuccessListener {
                android.util.Log.d("FirestoreInit", "Member document initialized for $uid")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FirestoreInit", "Failed to initialize member document", e)
            }
    }
}
