package com.example.saarthi.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.saarthi.MainActivity
import com.example.saarthi.R
import com.google.android.gms.location.*

class FamilyTrackingService : Service() {

    companion object {
        const val CHANNEL_ID = "family_tracking"
        const val NOTIF_ID = 202
        const val ACTION_START = "FAMILY_START"
        const val ACTION_STOP = "FAMILY_STOP"
    }

    private lateinit var fused: FusedLocationProviderClient
    private lateinit var repo: FirestoreRepo
    private var callback: LocationCallback? = null

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        repo = FirestoreRepo(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startSharing()
            ACTION_STOP -> stopSharing()
        }
        return START_STICKY
    }

    private fun startSharing() {
        startForeground(NOTIF_ID, buildNotification("Sharing live location"))
        repo.setSharing(true)
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15000L)
            .setMinUpdateDistanceMeters(30f)
            .build()
        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    android.util.Log.d("FamilyTrackingService", "Location received: ${location.latitude}, ${location.longitude}")
                    repo.publishLiveLocation(location)
                }
            }
        }
        fused.requestLocationUpdates(request, callback as LocationCallback, mainLooper)
    }

    private fun stopSharing() {
        repo.setSharing(false)
        callback?.let { fused.removeLocationUpdates(it) }
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = Intent(this, FamilyTrackingService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, getString(R.string.stop), stopPending)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Family sharing", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}


