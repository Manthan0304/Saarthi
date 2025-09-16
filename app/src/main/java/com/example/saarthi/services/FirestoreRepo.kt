package com.example.saarthi.services

import android.content.Context
import android.location.Location
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class MemberLive(
    val uid: String,
    val displayName: String?,
    val phone: String?,
    val lat: Double?,
    val lng: Double?,
    val updatedAt: Timestamp?,
    val sharing: Boolean
)

class FirestoreRepo(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private fun familyId(): String {
        // Minimal: single default family. Replace with real selection later.
        return "default_family"
    }

    fun currentUid(): String? = auth.currentUser?.uid

    fun upsertProfile(displayName: String? = null) {
        val uid = currentUid() ?: return
        val phoneNumber = auth.currentUser?.phoneNumber
        val finalDisplayName = displayName ?: auth.currentUser?.displayName ?: "User"
        
        // Use FirestoreInitializer to ensure proper initialization
        val initializer = FirestoreInitializer(context)
        initializer.initializeDefaultFamily()
        initializer.initializeUserProfile(uid, phoneNumber, finalDisplayName)
    }

    fun setSharing(enabled: Boolean) {
        val uid = currentUid() ?: return
        val doc = db.collection("families").document(familyId())
            .collection("members").document(uid)
        doc.set(hashMapOf(
            "sharing" to enabled,
            "updatedAt" to FieldValue.serverTimestamp()
        ), com.google.firebase.firestore.SetOptions.merge())
    }

    fun addToAllowlist(phoneNumber: String) {
        val uid = currentUid() ?: return
        val doc = db.collection("families").document(familyId())
            .collection("members").document(uid)
        
        android.util.Log.d("FirestoreRepo", "Adding $phoneNumber to allowlist for user $uid")
        
        doc.update("allowlist", FieldValue.arrayUnion(phoneNumber))
            .addOnSuccessListener {
                android.util.Log.d("FirestoreRepo", "Successfully added $phoneNumber to allowlist for $uid")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FirestoreRepo", "Failed to add $phoneNumber to allowlist for $uid", e)
            }
    }

    fun removeFromAllowlist(phoneNumber: String) {
        val uid = currentUid() ?: return
        val doc = db.collection("families").document(familyId())
            .collection("members").document(uid)
        doc.update("allowlist", FieldValue.arrayRemove(phoneNumber))
            .addOnSuccessListener {
                android.util.Log.d("FirestoreRepo", "Removed $phoneNumber from allowlist for $uid")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FirestoreRepo", "Failed to remove from allowlist", e)
            }
    }

    fun getAllowlist(onResult: (List<String>) -> Unit) {
        val uid = currentUid() ?: return
        val doc = db.collection("families").document(familyId())
            .collection("members").document(uid)
        doc.get().addOnSuccessListener { snapshot ->
            val allowlist = snapshot.get("allowlist") as? List<String> ?: emptyList()
            android.util.Log.d("FirestoreRepo", "Retrieved allowlist for $uid: $allowlist")
            onResult(allowlist)
        }.addOnFailureListener { e ->
            android.util.Log.e("FirestoreRepo", "Failed to get allowlist for $uid", e)
        }
    }

    fun debugCurrentUser() {
        val uid = currentUid()
        val phone = auth.currentUser?.phoneNumber
        android.util.Log.d("FirestoreRepo", "DEBUG - Current user UID: $uid")
        android.util.Log.d("FirestoreRepo", "DEBUG - Current user phone: $phone")
        
        if (uid != null) {
            getAllowlist { allowlist ->
                android.util.Log.d("FirestoreRepo", "DEBUG - Current user allowlist: $allowlist")
            }
        }
    }

    fun publishLiveLocation(location: Location) {
        val uid = currentUid() ?: return
        val data = hashMapOf(
            "lat" to location.latitude,
            "lng" to location.longitude,
            "accuracy" to location.accuracy,
            "speed" to location.speed,
            "updatedAt" to FieldValue.serverTimestamp(),
            "sharing" to true
        )
        
        android.util.Log.d("FirestoreRepo", "Publishing location for $uid: ${location.latitude}, ${location.longitude}")
        
        db.collection("families").document(familyId())
            .collection("members").document(uid)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                android.util.Log.d("FirestoreRepo", "Location published successfully for $uid")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FirestoreRepo", "Failed to publish location for $uid", e)
            }
    }

    fun subscribeActiveMembers(onUpdate: (List<MemberLive>) -> Unit): ListenerRegistration {
        val fam = familyId()
        val currentUid = currentUid() ?: return db.collection("families").document(fam).collection("members").addSnapshotListener { _, _ -> }
        val membersRef = db.collection("families").document(fam).collection("members")
        val profilesRef = db.collection("families").document(fam).collection("profiles")
        
        android.util.Log.d("FirestoreRepo", "Starting subscription to family: $fam for user: $currentUid")
        
        return membersRef.addSnapshotListener { snap, error ->
            if (error != null) {
                android.util.Log.e("FirestoreRepo", "Error listening to members", error)
                return@addSnapshotListener
            }
            if (snap == null) {
                android.util.Log.d("FirestoreRepo", "Members snapshot is null")
                return@addSnapshotListener
            }
            
            android.util.Log.d("FirestoreRepo", "Members snapshot received: ${snap.documents.size} documents")
            
            // Get current user's allowlist first
            val currentUserDoc = snap.documents.find { it.id == currentUid }
            val currentUserAllowlist = currentUserDoc?.get("allowlist") as? List<String> ?: emptyList()
            val currentUserPhone = auth.currentUser?.phoneNumber
            
            android.util.Log.d("FirestoreRepo", "Current user allowlist: $currentUserAllowlist")
            
            // Fetch profiles in parallel
            profilesRef.get().addOnSuccessListener { profs ->
                android.util.Log.d("FirestoreRepo", "Profiles fetched: ${profs.documents.size} documents")
                
                val byUid = profs.documents.associateBy({ it.id }, { it })
                val list = snap.documents.map { d ->
                    val uid = d.id
                    val sharing = d.getBoolean("sharing") ?: false
                    val lat = d.getDouble("lat")
                    val lng = d.getDouble("lng")
                    val allowlist = d.get("allowlist") as? List<String> ?: emptyList()
                    val memberPhone = byUid[uid]?.getString("phone")
                    
                    android.util.Log.d("FirestoreRepo", "Member $uid: sharing=$sharing, lat=$lat, lng=$lng, allowlist=$allowlist")
                    
                    MemberLive(
                        uid = uid,
                        displayName = byUid[uid]?.getString("displayName"),
                        phone = memberPhone,
                        lat = lat,
                        lng = lng,
                        updatedAt = d.getTimestamp("updatedAt"),
                        sharing = sharing
                    )
                }
                
                // TEMPORARY: Show all sharing members for debugging
                // TODO: Remove this and use the filtered logic below once allowlist is working
                val filteredList = list.filter { member ->
                    if (!member.sharing) {
                        android.util.Log.d("FirestoreRepo", "Member ${member.uid}: not sharing")
                        return@filter false
                    }
                    android.util.Log.d("FirestoreRepo", "Member ${member.uid}: sharing, showing for debugging")
                    return@filter true // TEMPORARY: Show all sharing members
                }
                
                /* ORIGINAL FILTERED LOGIC - COMMENTED OUT FOR DEBUGGING
                val filteredList = list.filter { member ->
                    if (!member.sharing) {
                        android.util.Log.d("FirestoreRepo", "Member ${member.uid}: not sharing")
                        return@filter false
                    }
                    if (member.uid == currentUid) {
                        android.util.Log.d("FirestoreRepo", "Member ${member.uid}: is current user")
                        return@filter true // Always show yourself
                    }
                    
                    val memberDoc = snap.documents.find { it.id == member.uid }
                    val memberAllowlist = memberDoc?.get("allowlist") as? List<String> ?: emptyList()
                    
                    android.util.Log.d("FirestoreRepo", "Checking permissions for ${member.uid}:")
                    android.util.Log.d("FirestoreRepo", "  Current user phone: $currentUserPhone")
                    android.util.Log.d("FirestoreRepo", "  Member phone: ${member.phone}")
                    android.util.Log.d("FirestoreRepo", "  Current user allowlist: $currentUserAllowlist")
                    android.util.Log.d("FirestoreRepo", "  Member allowlist: $memberAllowlist")
                    
                    // Check if current user is in member's allowlist
                    val currentUserInMemberAllowlist = currentUserPhone != null && memberAllowlist.contains(currentUserPhone)
                    // Check if member is in current user's allowlist  
                    val memberInCurrentUserAllowlist = member.phone != null && currentUserAllowlist.contains(member.phone)
                    
                    val canSee = currentUserInMemberAllowlist || memberInCurrentUserAllowlist
                    android.util.Log.d("FirestoreRepo", "  Result: canSee=$canSee (currentInMember=$currentUserInMemberAllowlist, memberInCurrent=$memberInCurrentUserAllowlist)")
                    
                    canSee
                }
                */
                
                android.util.Log.d("FirestoreRepo", "Filtered members (sharing only for debugging): ${filteredList.size}")
                onUpdate(filteredList)
            }.addOnFailureListener { e ->
                android.util.Log.e("FirestoreRepo", "Error fetching profiles", e)
            }
        }
    }
}


