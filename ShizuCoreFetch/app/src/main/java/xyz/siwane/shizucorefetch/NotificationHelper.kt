package xyz.siwane.shizucorefetch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    private const val CHANNEL_STORE_OPERATIONS = "store_ops_channel"
    private const val CHANNEL_UPDATES_ALERTS = "updates_alerts_channel"
    
    const val NOTIFICATION_ID_PROGRESS = 101
    const val NOTIFICATION_ID_STATUS = 102
    const val NOTIFICATION_ID_UPDATE = 103

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // قناة عمليات المتجر (تحميل وتثبيت)
            val opsChannel = NotificationChannel(
                CHANNEL_STORE_OPERATIONS,
                context.getString(R.string.notif_channel_ops_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_ops_desc)
                setSound(null, null)
                enableVibration(false)
            }

            // قناة تنبيهات التحديثات المتاحة
            val updatesChannel = NotificationChannel(
                CHANNEL_UPDATES_ALERTS,
                context.getString(R.string.notif_channel_updates_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_updates_desc)
            }

            notificationManager.createNotificationChannel(opsChannel)
            notificationManager.createNotificationChannel(updatesChannel)
        }
    }

    fun showDownloadProgress(context: Context, appName: String, progressText: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(context, CHANNEL_STORE_OPERATIONS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(appName)
            .setContentText(progressText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(NOTIFICATION_ID_PROGRESS, notification)
    }

    fun showStatusNotification(context: Context, title: String, message: String, isSuccess: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_PROGRESS) // إلغاء إشعار التحميل المستمر

        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_STORE_OPERATIONS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(if (isSuccess) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID_STATUS, notification)
    }

    fun showUpdateAvailableNotification(context: Context, count: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(context, HomeActivity::class.java).apply {
            putExtra("navigate_to", "account")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = context.getString(R.string.notif_updates_available_msg, count)

        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notif_updates_available_title))
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID_UPDATE, notification)
    }
}
