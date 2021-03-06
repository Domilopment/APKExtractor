package domilopment.apkextractor.autoBackup

import android.app.*
import android.content.*
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import domilopment.apkextractor.MainActivity
import domilopment.apkextractor.R
import java.lang.IllegalArgumentException
import java.util.concurrent.atomic.AtomicInteger

class AutoBackupService : Service() {
    companion object {
        private const val CHANNEL_ID = "domilopment.apkextractor.AUTO_BACKUP_SERVICE"
        const val ACTION_STOP_SERVICE = "domilopment.apkextractor.STOP_AUTO_BACKUP_SERVICE"
        const val ACTION_RESTART_SERVICE = "domilopment.apkextractor.RESTART_AUTO_BACKUP_SERVICE"

        // Check for Service is Running
        var isRunning = false

        // Create unique notification IDs
        private val c: AtomicInteger = AtomicInteger(1)
        fun getNewNotificationID() = c.incrementAndGet()
    }

    private lateinit var br: BroadcastReceiver

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        // Create Broadcast Receiver for App Updates
        br = PackageBroadcastReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start Foreground Service with Notification
        startForeground(1, createNotification())

        // Set Filter for Broadcast
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        // Start Broadcast Receiver
        registerReceiver(br, filter)

        isRunning = true

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            unregisterReceiver(br)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
        stopForeground(true)
        // Restart Service if kill isn't called by user
        if (
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
                .getBoolean("auto_backup", false)
        ) restartService()
    }

    /**
     * Creates Notification
     * @return Notification
     * Returns a Notification ready to use
     */
    private fun createNotification(): Notification {
        // Create Notification Channel
        val channel = NotificationChannel(
            CHANNEL_ID,
            "App Update Watching Service",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }

        // Open Channel with Notification Service
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)

        // Call MainActivity an Notification Click
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        // Stop Foreground Service on Button Click
        val stopPendingIntent: PendingIntent =
            Intent(this, PackageBroadcastReceiver::class.java).apply {
                action = ACTION_STOP_SERVICE
            }.let { stopIntent ->
                PendingIntent.getBroadcast(this, 0, stopIntent, 0)
            }


        // Build and return Notification
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.auto_backup_notification_title))
            .setContentText(getString(R.string.auto_backup_notification_content_text))
            .setSmallIcon(R.drawable.ic_small_notification_icon_24)
            .setColor(getColor(R.color.notificationColor))
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_small_notification_icon_24,
                getString(R.string.auto_backup_notification_action_stop),
                stopPendingIntent
            )
            .build()
    }

    /**
     * Restarts Service even if Phone is in dose mode
     */
    private fun restartService() {
        // restart Pending Intent
        val restartServicePendingIntent: PendingIntent = Intent(
            this,
            PackageBroadcastReceiver::class.java
        ).apply {
            action = ACTION_RESTART_SERVICE
        }.let { restartIntent ->
            PendingIntent.getBroadcast(this, 0, restartIntent, 0)
        }
        // System Alarm Manager
        val alarmService: AlarmManager =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Set Alarm to be executed
        alarmService.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent,
        )
    }
}