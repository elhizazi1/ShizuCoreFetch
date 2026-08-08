package xyz.siwane.shizucorefetch

import android.app.Application

class ShizuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.applyTheme(this)
        LanguageManager.applyLanguage(this)
        // تهيئة قنوات الإشعارات
        NotificationHelper.initChannels(this)
    }
}
