package com.mobdysseus.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/**
 * Foreground service skeleton for on-device inference.
 *
 * Keeps the process alive (START_STICKY) and holds a partial wakelock so that
 * a long-running GGUF inference job is not killed or CPU-suspended while the
 * screen is off or the app is backgrounded. No inference actually runs here
 * yet — this is the lifecycle shell that an on-device engine can be attached to.
 *
 * Requires the manifest wiring documented in the module report:
 *   - <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
 *   - <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE"/>
 *   - <uses-permission android:name="android.permission.WAKE_LOCK"/>
 *   - <service android:name=".service.InferenceService" android:foregroundServiceType="specialUse"
 *              android:exported="false"/>
 *   - a <property> element declaring the specialUse subtype (required on API 34+).
 */
class InferenceService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$TAG:$WAKELOCK_TAG")
            .apply { acquire() }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(com.mobdysseus.app.R.string.app_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "On-device inference progress"
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val icon = applicationInfo.icon
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle("Mobdysseus")
            .setContentText("Running on-device inference")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "InferenceService"
        private const val WAKELOCK_TAG = "InferenceWakeLock"
        private const val CHANNEL_ID = "inference"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            val intent = Intent(context, InferenceService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, InferenceService::class.java)
            context.stopService(intent)
        }
    }
}
