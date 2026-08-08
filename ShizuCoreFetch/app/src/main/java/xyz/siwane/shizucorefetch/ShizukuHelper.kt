package xyz.siwane.shizucorefetch

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuHelper {
    
    // التحقق مما إذا كان محرك Shizuku يعمل في الخلفية
    fun isShizukuRunning(): Boolean {
        return Shizuku.pingBinder()
    }

    // التحقق مما إذا كان تطبيقنا يملك صلاحية استخدام Shizuku
    fun hasPermission(): Boolean {
        if (!isShizukuRunning()) return false
        return if (Shizuku.isPreV11()) {
            false // لا ندعم الإصدارات القديمة جداً
        } else {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    }

    // طلب الصلاحية من نظام Shizuku
    fun requestPermission(requestCode: Int) {
        if (isShizukuRunning() && !hasPermission()) {
            Shizuku.requestPermission(requestCode)
        }
    }
}
