package xyz.siwane.shizucorefetch

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageManager {
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANG = "app_language"

    const val LANG_EN = "en"
    const val LANG_AR = "ar"
    const val LANG_FR = "fr"
    const val LANG_ES = "es"
    const val LANG_PT = "pt"
    const val LANG_RU = "ru"
    const val LANG_HI = "hi"
    const val LANG_ZH = "zh"
    const val LANG_JA = "ja"
    const val LANG_CS = "cs"
    const val LANG_TR = "tr"

    fun applyLanguage(context: Context) {
        val lang = getSavedLanguage(context)
        val localeList = LocaleListCompat.forLanguageTags(lang)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun saveLanguage(context: Context, lang: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, lang).apply()
        
        val localeList = LocaleListCompat.forLanguageTags(lang)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANG, LANG_EN) ?: LANG_EN
    }
}
