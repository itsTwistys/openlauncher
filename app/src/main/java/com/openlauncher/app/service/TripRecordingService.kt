package com.openlauncher.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import com.openlauncher.app.MainActivity

/**
 * Keeps the launcher process alive with an ongoing notification while a trip is recording and the owner has chosen
 * background recording. The distance counting itself stays in the view model; this service only keeps it running
 * and keeps the owner informed. It stops when the trip is paused or saved.
 */
class TripRecordingService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val located = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!located) {
            // A location service without location access would be rejected by Android; recording stays with the visible launcher.
            stopSelf()
            return START_NOT_STICKY
        }
        val open = PendingIntentCompat.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT, false
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Recording trip")
            .setContentText("Open Launcher is counting distance and time. Pause the trip to stop.")
            .setContentIntent(open)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            else startForeground(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Location access was revoked after the check; recording simply falls back to the visible launcher.
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Trip recording", NotificationManager.IMPORTANCE_LOW))
    }

    companion object {
        private const val CHANNEL_ID = "trip_recording"
        private const val NOTIFICATION_ID = 4101

        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, TripRecordingService::class.java)) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, TripRecordingService::class.java)) }
        }
    }
}
