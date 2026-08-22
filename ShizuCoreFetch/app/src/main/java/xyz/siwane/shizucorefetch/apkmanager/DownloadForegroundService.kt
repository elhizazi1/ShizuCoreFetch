package xyz.siwane.shizucorefetch.apkmanager

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import xyz.siwane.shizucorefetch.R
import xyz.siwane.shizucorefetch.notifications.NotificationHelper

/**
 * خدمة أمامية (Foreground Service) بسيطة، غرضها الوحيد إبقاء عملية التطبيق
 * (process) حية بأولوية عالية أثناء التحميل/التثبيت — بالضبط كيفما كيدير
 * Google Play الحقيقي: عملية التنزيل ماتوقفش وتكمل حتى لو غادر المستخدم
 * التطبيق أو دخل لمكان آخر فيه.
 *
 * ما فيهاش أي منطق تحميل بذاتها؛ المنطق الحقيقي كاملاً فـ [DownloadInstallManager]
 * (اللي كيخدم على coroutine scope خاص بالتطبيق كاملة، ماشي مرتبط بهاد الخدمة
 * ولا بأي Activity). هاد الخدمة غير "غلاف" باش يعطي العملية أولوية أعلى عند
 * النظام طول ما كاين تحميل أو تثبيت جاري.
 */
class DownloadForegroundService : Service() {

    companion object {
        private const val NOTIF_ID = 7788
        private const val ACTION_KEEP_ALIVE = "xyz.siwane.shizucorefetch.action.KEEP_ALIVE"

        // عداد بسيط لعدد العمليات الجارية حاليًا (ممكن تحميلات متزامنة لعدة
        // تطبيقات فنفس الوقت) — الخدمة كتوقف نفسها غير لما يوصل العداد للصفر.
        @Volatile private var activeCount = 0

        fun notifyOperationStarted(context: Context) {
            synchronized(this) { activeCount++ }
            val intent = Intent(context, DownloadForegroundService::class.java).setAction(ACTION_KEEP_ALIVE)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
                // إذا فشل تشغيل الخدمة الأمامية لأي سبب (نادر)، التحميل كيكمل
                // عادي فـ DownloadInstallManager، غير بلا حماية إضافية من قتل
                // النظام للعملية أثناء الخلفية الكاملة.
            }
        }

        fun notifyOperationFinished(context: Context) {
            val remaining = synchronized(this) {
                activeCount = (activeCount - 1).coerceAtLeast(0)
                activeCount
            }
            if (remaining == 0) {
                try {
                    context.stopService(Intent(context, DownloadForegroundService::class.java))
                } catch (_: Exception) { }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        // START_NOT_STICKY: إذا قتل النظام العملية رغم الخدمة الأمامية (نادر
        // جدًا)، ما فيش داعي نعاود تشغيل الخدمة بـ intent فارغ — التحميل نفسه
        // غايفقد فهاد الحالة القصوى، ومن الأحسن ما تعاودش السيرفيس روحها.
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_STORE_OPERATIONS)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(getString(R.string.notif_channel_ops_name))
            .setContentText(getString(R.string.notif_installing))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
