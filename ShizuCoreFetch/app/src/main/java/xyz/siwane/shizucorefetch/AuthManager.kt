package xyz.siwane.shizucorefetch

import android.content.Context

object AuthManager {
    private const val PREFS_NAME = "AuthPrefs"
    private const val KEY_GITHUB_TOKEN = "github_token"
    private const val KEY_GITHUB_USERNAME = "github_username"

    fun saveToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_GITHUB_TOKEN, token).apply()
    }

    // يجلب توكن المستخدم الفعلي فقط (يستخدم للتحقق من تسجيل الدخول والعمليات الحساسة)
    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_GITHUB_TOKEN, null)
    }

    fun saveUsername(context: Context, username: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_GITHUB_USERNAME, username).apply()
    }

    fun getUsername(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_GITHUB_USERNAME, null)
    }

    fun hasValidToken(context: Context): Boolean = !getToken(context).isNullOrEmpty()

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_GITHUB_TOKEN)
            .remove(KEY_GITHUB_USERNAME)
            .apply()
    }
}
