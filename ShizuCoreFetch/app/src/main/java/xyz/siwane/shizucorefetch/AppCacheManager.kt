package xyz.siwane.shizucorefetch

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AppCacheManager {
    private const val PREFS_NAME = "InstalledAppsCache"

    fun savePackageName(context: Context, appId: String, packageName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(appId, packageName).apply()
    }

    fun removePackageName(context: Context, appId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(appId).apply()
    }

    // الدالة السريعة (آمنة تماماً على الخيط الرئيسي)
    fun getSavedPackageFast(context: Context, appId: String): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedPackage = prefs.getString(appId, null)
        
        if (savedPackage != null) {
            return try {
                context.packageManager.getPackageInfo(savedPackage, 0)
                savedPackage 
            } catch (e: PackageManager.NameNotFoundException) {
                removePackageName(context, appId) 
                null
            }
        }
        return null
    }

    // الدالة المحدثة: تعمل في الخلفية ولا تسبب أي تجميد (ANR)
    fun getPackageNameAsync(context: Context, appName: String, appId: String, onResult: (String) -> Unit) {
        // الفحص السريع أولاً
        val saved = getSavedPackageFast(context, appId)
        if (saved != null) {
            onResult(saved)
            return
        }

        // نقل المهمة الشاقة إلى الخلفية (IO Thread)
        CoroutineScope(Dispatchers.IO).launch {
            var foundPackage = ""
            try {
                val pm = context.packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                val cleanAppName = appName.replace(" ", "").lowercase()
                
                if (cleanAppName.isNotEmpty()) {
                    for (appInfo in packages) {
                        val pkgLastSegment = appInfo.packageName.substringAfterLast('.').lowercase()
                        val label = pm.getApplicationLabel(appInfo).toString().replace(" ", "").lowercase()

                        val isExactMatch = label == cleanAppName || pkgLastSegment == cleanAppName
                        if (isExactMatch) {
                            savePackageName(context, appId, appInfo.packageName)
                            foundPackage = appInfo.packageName
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // إرجاع النتيجة إلى الخيط الرئيسي لتحديث الواجهة بأمان
            withContext(Dispatchers.Main) {
                onResult(foundPackage)
            }
        }
    }
}
