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
import com.example.saarthi.data.Route
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TrackingService : Service() {

    companion object {
        const val CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_ID = 101

        const val ACTION_START_NEW = "TRACKING_START_NEW"
        const val ACTION_CONTINUE = "TRACKING_CONTINUE"
        const val ACTION_PAUSE = "TRACKING_PAUSE"
        const val ACTION_RESUME = "TRACKING_RESUME"
        const val ACTION_STOP = "TRACKING_STOP"

        const val EXTRA_ROUTE = "extra_route"
    }

    private lateinit var routeRecorder: RouteRecorder
    private val isRunning = AtomicBoolean(false)
    private var scope: CoroutineScope? = null
    private var tickerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        routeRecorder = RouteRecorder(this)
        scope = CoroutineScope(Dispatchers.Default)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ensure we start foreground quickly to show notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!isRunning.get()) {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        }
        when (intent?.action) {
            ACTION_START_NEW -> startNew()
            ACTION_CONTINUE -> {
                val route = intent.getSerializableExtra(EXTRA_ROUTE) as? Route
                if (route != null) continueExisting(route)
            }
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_STOP -> stopAll()
        }
        return START_STICKY
    }

    private fun startNew() {
        if (isRunning.get()) return
        routeRecorder.startRecording()
        startForeground(NOTIFICATION_ID, buildNotification())
        isRunning.set(true)
        setRecordingFlag(true)
        startTicker()
    }

    private fun continueExisting(route: Route) {
        if (isRunning.get()) return
        routeRecorder.continueRecording(route)
        startForeground(NOTIFICATION_ID, buildNotification())
        isRunning.set(true)
        setRecordingFlag(true)
        startTicker()
    }

    private fun pause() {
        if (!isRunning.get()) return
        routeRecorder.pauseRecording()
        updateNotification()
    }

    private fun resume() {
        if (!isRunning.get()) return
        routeRecorder.resumeRecording()
        updateNotification()
    }

    private fun stopAll() {
        // Service is the single source of truth for saving to avoid duplicates
        routeRecorder.stopRecording(save = true)
        stopTicker()
        isRunning.set(false)
        setRecordingFlag(false)
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = scope?.launch {
            while (isRunning.get()) {
                updateNotification()
                delay(1000)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseResumeAction = if (routeRecorder.isRecording()) {
            val pauseIntent = Intent(this, TrackingService::class.java).apply { action = ACTION_PAUSE }
            val pausePending = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(0, getString(R.string.pause), pausePending)
        } else {
            val resumeIntent = Intent(this, TrackingService::class.java).apply { action = ACTION_RESUME }
            val resumePending = PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(0, getString(R.string.resume), resumePending)
        }

        val stopIntent = Intent(this, TrackingService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val distanceMiles = String.format("%.1f", routeRecorder.getCurrentDistance())
        val time = formatMs(routeRecorder.getElapsedTime())

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Recording • ${distanceMiles} mi • ${time}")
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(pauseResumeAction)
            .addAction(0, getString(R.string.stop), stopPending)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification()
        nm.notify(NOTIFICATION_ID, notification)
        // Persist live metrics for UI
        val prefs = getSharedPreferences("routes", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean("recording_active", isRunning.get())
        editor.putLong("live_elapsed_ms", routeRecorder.getElapsedTime())
        editor.putFloat("live_distance_miles", routeRecorder.getCurrentDistance().toFloat())
        editor.apply()
    }

    private fun setRecordingFlag(active: Boolean) {
        val prefs = getSharedPreferences("routes", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("recording_active", active).apply()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_tracking),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun formatMs(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, seconds) else String.format("%02d:%02d", minutes, seconds)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTicker()
        scope?.cancel()
        super.onDestroy()
    }
}


