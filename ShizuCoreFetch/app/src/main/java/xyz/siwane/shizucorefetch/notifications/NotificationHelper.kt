package xyz.siwane.shizucorefetch.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import xyz.siwane.shizucorefetch.MainActivity
import xyz.siwane.shizucorefetch.R

object NotificationHelper {
    // غير private دابا: خاصها تكون قابلة للوصول من DownloadForegroundService
    // (الخدمة الأمامية) باش تبني إشعارها بنفس القناة، بلا تكرار السلسلة النصية.
    const val CHANNEL_STORE_OPERATIONS = "store_ops_channel"
    private const val CHANNEL_UPDATES_ALERTS = "updates_alerts_channel"
    
    // معرف ثابت للإشعارات العامة (مثل توفر تحديثات متعددة)
    private const val NOTIFICATION_ID_UPDATE_ALL = 9999

    /**
     * يجب استدعاء هذه الدالة مرة واحدة في MainActivity.onCreate أو Application class
     */
    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // قناة عمليات المتجر (تحميل وتثبيت) - صامتة وبدون اهتزاز
            val opsChannel = NotificationChannel(
                CHANNEL_STORE_OPERATIONS,
                context.getString(R.string.notif_channel_ops_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_ops_desc)
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }

            // قناة تنبيهات التحديثات المتاحة - باهتزاز وصوت افتراضي
            val updatesChannel = NotificationChannel(
                CHANNEL_UPDATES_ALERTS,
                context.getString(R.string.notif_channel_updates_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_updates_desc)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(opsChannel)
            notificationManager.createNotificationChannel(updatesChannel)
        }
    }

    /**
     * تحقق آمن من صلاحية الإشعارات لأندرويد 13 فما فوق
     */
    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    /**
     * عرض أو تحديث إشعار شريط التحميل لتطبيق معين
     * @param notificationId استخدام appName.hashCode() لضمان عدم تداخل التحميلات المتزامنة
     */
    fun showDownloadProgress(context: Context, notificationId: Int, appName: String, progress: Int) {
        if (!hasNotificationPermission(context)) return

        val notificationManager = NotificationManagerCompat.from(context)
        
        val progressText = if (progress == 100) {
            context.getString(R.string.notif_installing)
        } else {
            context.getString(R.string.notif_download_progress, progress)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_STORE_OPERATIONS)
            .setSmallIcon(R.mipmap.ic_launcher_round) // يُفضل مستقبلاً استخدام أيقونة شفافة مسطحة
            .setContentTitle(appName)
            .setContentText(progressText)
            .setProgress(100, progress, progress == 100) // شريط تحميل حقيقي (يصبح Indeterminate عند 100%)
            .setOngoing(true) // لا يمكن للمستخدم مسحه أثناء التحميل
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (_: SecurityException) { }
    }

    /**
     * عرض نتيجة العملية (تم التثبيت بنجاح، أو فشل)
     */
    fun showStatusNotification(context: Context, notificationId: Int, title: String, message: String, isSuccess: Boolean) {
        if (!hasNotificationPermission(context)) return

        val notificationManager = NotificationManagerCompat.from(context)
        
        // إزالة إشعار التحميل الخاص بهذا التطبيق وتبديله بإشعار النتيجة
        notificationManager.cancel(notificationId)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_STORE_OPERATIONS)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true) // يختفي عند النقر عليه
            .setContentIntent(pendingIntent)
            .setPriority(if (isSuccess) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
        } catch (_: SecurityException) { }
    }

    /**
     * عرض إشعار عند وجود تحديثات لتطبيقات المتجر
     */
    fun showUpdateAvailableNotification(context: Context, count: Int) {
        if (!hasNotificationPermission(context) || count <= 0) return

        val notificationManager = NotificationManagerCompat.from(context)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", "library_updates") // توجيه للمكتبة
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, NOTIFICATION_ID_UPDATE_ALL, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = context.getString(R.string.notif_updates_available_msg, count)

        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(context.getString(R.string.notif_updates_available_title))
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID_UPDATE_ALL, notification)
        } catch (_: SecurityException) { }
    }
    
    /**
     * إلغاء إشعار معين يدوياً (مثلاً إذا ألغى المستخدم التحميل)
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
