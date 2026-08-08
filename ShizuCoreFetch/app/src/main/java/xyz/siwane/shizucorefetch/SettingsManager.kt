package xyz.siwane.shizucorefetch

import android.content.Context

object SettingsManager {
    private const val PREFS_NAME = "app_settings_prefs"
    private const val KEY_SILENT_INSTALL = "silent_install_enabled"

    // الوضع الافتراضي "مفعل" (true) للحفاظ على سلوك المتجر الأصلي
    fun isSilentInstallEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SILENT_INSTALL, true)
    }

    fun setSilentInstallEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SILENT_INSTALL, enabled).apply()
    }
}
