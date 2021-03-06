package domilopment.apkextractor.autoBackup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import domilopment.apkextractor.MainActivity
import domilopment.apkextractor.R
import domilopment.apkextractor.data.Application
import domilopment.apkextractor.utils.FileHelper
import domilopment.apkextractor.utils.SettingsManager
import java.io.FileNotFoundException

/**
 * Async Task to ensure Backup isn't killed by System when Broadcast finishes
 * @param pendingResult
 * @param context get Broadcast context
 * @param packageName Package name of Updated App
 */
class AsyncBackupTask(
    private val pendingResult: BroadcastReceiver.PendingResult?,
    private val context: Context,
    packageName: String
) : AsyncTask<String, Int, Unit>() {
    companion object {
        private const val CHANNEL_ID = "domilopment.apkextractor.BROADCAST_RECEIVER"
        const val ACTION_DELETE_APK = "domilopment.apkextractor.DELETE_APK"
    }

    private var success: Uri? = null

    // Get Application Info from Package
    private val app =
        Application(
            context.packageManager.getPackageInfo(packageName, 0).applicationInfo,
            context.packageManager
        )

    override fun doInBackground(vararg params: String?) {
        val path = SettingsManager(context).saveDir()

        // Try to Backup App
        success = try {
            FileHelper(context).copy(
                app.appSourceDirectory,
                path!!,
                SettingsManager(context).appName(app)
            )
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    override fun onPostExecute(result: Unit?) {
        super.onPostExecute(result)
        // Let User know when App is or should be Updated
        createNotificationChannel()
        with(NotificationManagerCompat.from(context)) {
            success?.let {
                // notificationId is a unique int for each notification that you must define
                val id = AutoBackupService.getNewNotificationID()
                notify(id, createNotificationSuccess(id).build())
            } ?: run {
                // notificationId is a unique int for each notification that you must define
                notify(AutoBackupService.getNewNotificationID(), createNotificationFailed().build())
            }
        }
        // Must call finish() so the BroadcastReceiver can be recycled.
        pendingResult?.finish()
    }

    /**
     * Create notification Channel for auto Backup Apk results
     */
    private fun createNotificationChannel() {
        // Create Notification Channel
        val channel = NotificationChannel(
            CHANNEL_ID,
            "App Backup Created",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            lightColor = R.color.colorPrimary
            enableLights(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }

        // Open Channel with Notification Service
        val service =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
    }

    /**
     * Creates Notification
     * @return Notification
     * Returns a Notification for Backup Apk
     */
    private fun createNotificationSuccess(notificationID: Int): NotificationCompat.Builder {
        // Call MainActivity an Notification Click
        val pendingIntent: PendingIntent =
            Intent(context, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(context, 0, notificationIntent, 0)
            }

        // Share APK on Button Click
        val sharePendingIntent: PendingIntent =
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = FileHelper.MIME_TYPE
                    putExtra(Intent.EXTRA_STREAM, success!!)
                },
                context.getString(R.string.share_intent_title)
            ).let {
                PendingIntent.getActivity(context, 0, it, 0)
            }

        // Delete APK on Button Click
        val deletePendingIntent: PendingIntent =
            Intent(context, PackageBroadcastReceiver::class.java).apply {
                action = ACTION_DELETE_APK
                data = success!!
                putExtra("ID", notificationID)
            }.let { stopIntent ->
                PendingIntent.getBroadcast(context, 0, stopIntent, 0)
            }

        // Build and return Notification
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.auto_backup_broadcast_receiver_notification_title))
            .setContentText(
                context.getString(
                    R.string.auto_backup_broadcast_receiver_backup_success,
                    app.appName
                )
            ).setSmallIcon(R.drawable.ic_small_notification_icon_24)
            .setColor(context.getColor(R.color.notificationColor))
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_small_notification_icon_24,
                context.getString(R.string.action_bottom_sheet_share),
                sharePendingIntent
            ).addAction(
                R.drawable.ic_small_notification_icon_24,
                context.getString(R.string.alert_apk_selected_delete),
                deletePendingIntent
            ).setAutoCancel(true)
    }

    /**
     * Creates Notification
     * @return Notification
     * Returns a Notification if Backup Apk failed
     */
    private fun createNotificationFailed(): NotificationCompat.Builder {
        // Call MainActivity an Notification Click
        val pendingIntent: PendingIntent =
            Intent(context, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(context, 0, notificationIntent, 0)
            }

        // Build and return Notification
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.auto_backup_broadcast_receiver_notification_title))
            .setContentText(
                context.getString(
                    R.string.auto_backup_broadcast_receiver_backup_failed,
                    app.appName
                )
            ).setSmallIcon(R.drawable.ic_small_notification_icon_24)
            .setColor(context.getColor(R.color.notificationColor))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
    }
}