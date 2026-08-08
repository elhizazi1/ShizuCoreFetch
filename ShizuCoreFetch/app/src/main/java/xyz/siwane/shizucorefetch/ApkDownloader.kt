package xyz.siwane.shizucorefetch

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object ApkDownloader {

    fun downloadLatestRelease(
        context: Context,
        developer: String,
        repoName: String,
        onProgress: (String) -> Unit,
        onResult: (Boolean, String?) -> Unit
    ) {
        thread {
            try {
                val apiUrl = "https://api.github.com/repos/$developer/$repoName/releases/latest"
                val result = GithubClient.get(context, apiUrl, "application/vnd.github.v3+json")

                // معالجة حظر GitHub (Rate Limit) بدقة لمنع الرسائل الوهمية
                if (result.code == 403) {
                    postResult(onResult, false, "عذراً، لقد تجاوزت حد الطلبات المسموح به من GitHub. يرجى تسجيل الدخول أو المحاولة لاحقاً.")
                    return@thread
                } else if (result.code != 200 || result.body == null) {
                    postResult(onResult, false, context.getString(R.string.downloader_no_apk))
                    return@thread
                }

                val jsonObject = JSONObject(result.body)
                val assets = jsonObject.getJSONArray("assets")

                val apkAssets = ArrayList<JSONObject>()
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.lowercase().endsWith(".apk")) apkAssets.add(asset)
                }

                if (apkAssets.isEmpty()) {
                    postResult(onResult, false, context.getString(R.string.downloader_no_apk))
                    return@thread
                }

                val chosenAsset = pickBestApkAsset(apkAssets)
                val downloadUrl = chosenAsset?.optString("browser_download_url")
                val fileName = chosenAsset?.optString("name")

                if (downloadUrl.isNullOrEmpty() || fileName.isNullOrEmpty()) {
                    postResult(onResult, false, context.getString(R.string.downloader_no_apk))
                    return@thread
                }

                // 1. استخدام المسار الخارجي لكي ينجح Shizuku (ADB) في الوصول إليه وقراءته
                val apkFile = File(context.externalCacheDir ?: context.cacheDir, fileName)
                
                // 2. التحقق الجذري والذكي من سلامة الملف المتراكم من النسخ السابقة
                if (apkFile.exists()) {
                    // نطلب من نظام الأندرويد فحص الملف (هل هو APK سليم ومكتمل أم تالف؟)
                    val packageInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
                    
                    if (packageInfo != null) {
                        // الملف سليم 100% ومكتمل، نمنحه صلاحية القراءة الإجبارية لتفادي أخطاء Shizuku
                        apkFile.setReadable(true, false)
                        postProgress(onProgress, "الملف موجود ومكتمل، جاري بدء التثبيت...")
                        postResult(onResult, true, apkFile.absolutePath)
                        return@thread
                    } else {
                        // الملف موجود ولكنه تالف (بسبب انقطاع تحميل سابق أو خطأ متراكم)
                        // نقوم بحذف هذا الملف الوهمي فوراً بصمت لكي نعيد تحميل نسخة سليمة
                        apkFile.delete()
                    }
                }

                // 3. بدء التنزيل النظيف بعد التأكد من عدم وجود ملف تالف
                postProgress(onProgress, context.getString(R.string.downloader_downloading, fileName))
                NotificationHelper.showDownloadProgress(context, repoName, context.getString(R.string.downloader_downloading, fileName))

                val downloadConnection = URL(downloadUrl).openConnection() as HttpURLConnection
                downloadConnection.connect()

                val inputStream = downloadConnection.inputStream
                val outputStream = FileOutputStream(apkFile)

                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.close()
                inputStream.close()

                // 4. إعطاء صلاحية القراءة الشاملة للملف فور اكتمال التحميل لضمان نجاح التثبيت عبر ADB
                apkFile.setReadable(true, false)

                postProgress(onProgress, context.getString(R.string.downloader_success))
                postResult(onResult, true, apkFile.absolutePath)

            } catch (e: Exception) {
                postResult(onResult, false, context.getString(R.string.downloader_fail, e.message))
            }
        }
    }

    // كلمات مفتاحية شائعة تُستخدم في أسماء ملفات الـ APK للتمييز بين نسخة الإصدار
    // ونسخ التطوير/الاختبار. لا تغطي 100% من الحالات لكنها تلتقط الغالبية العظمى منها،
    // كما اقترح المستخدم في تقرير الأخطاء
    private val debugKeywords = listOf("debug", "-dev", "_dev", "test", "beta", "alpha", "nightly", "snapshot", "unsigned")
    private val releaseKeywords = listOf("release", "stable")

    /**
     * يختار أفضل ملف APK من قائمة الأصول (assets) الخاصة بإصدار GitHub:
     * - إن وُجد ملف واحد فقط: نستخدمه مباشرة (لا داعي لأي فلترة).
     * - إن وُجدت عدة ملفات: نُفضّل أي ملف يحتوي كلمة "release/stable" في اسمه،
     *   ونستبعد أي ملف يحتوي كلمة دالة على نسخة تطوير/اختبار مثل "debug".
     * - إن لم نجد أي مطابقة واضحة، نرجع لأول ملف كسلوك احتياطي آمن.
     * ملاحظة: نعتمد فقط على اسم الملف (metadata) بدون تحميله، لأن التحقق من التوقيع
     * الرقمي يتطلب تنزيل الملف بالكامل أولاً وهو أمر غير عملي قبل اتخاذ قرار التحميل.
     */
    private fun pickBestApkAsset(apkAssets: List<JSONObject>): JSONObject? {
        if (apkAssets.size == 1) return apkAssets[0]

        val withoutDebug = apkAssets.filter { asset ->
            val name = asset.optString("name", "").lowercase()
            debugKeywords.none { keyword -> name.contains(keyword) }
        }

        val candidates = if (withoutDebug.isNotEmpty()) withoutDebug else apkAssets

        val explicitRelease = candidates.firstOrNull { asset ->
            val name = asset.optString("name", "").lowercase()
            releaseKeywords.any { keyword -> name.contains(keyword) }
        }
        if (explicitRelease != null) return explicitRelease

        // لا توجد كلمة "release" صريحة لكن على الأقل استبعدنا نسخ الـ debug/beta
        if (withoutDebug.isNotEmpty()) return withoutDebug[0]

        // كل الملفات تحتوي كلمات دالة على debug/test (نادر) - نرجع لأول ملف كخيار أخير
        return apkAssets.firstOrNull()
    }

    private fun postResult(onResult: (Boolean, String?) -> Unit, success: Boolean, result: String?) {
        Handler(Looper.getMainLooper()).post { onResult(success, result) }
    }

    private fun postProgress(onProgress: (String) -> Unit, message: String) {
        Handler(Looper.getMainLooper()).post { onProgress(message) }
    }
}
