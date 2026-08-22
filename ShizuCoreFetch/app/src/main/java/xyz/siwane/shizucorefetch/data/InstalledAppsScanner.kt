package xyz.siwane.shizucorefetch.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

/**
 * فحص إجباري وكامل لكل التطبيقات المثبتة فعليًا على الجهاز، مباشرة من
 * PackageManager (بلا استثناء، وبلا اعتماد على أي كاش محلي مبني على تخمين).
 * كنبنيو خريطتين:
 *  - byPackage: اسم الحزمة -> معلومات التطبيق (المطابقة الأدق والأولى).
 *  - byNormalizedName: اسم التطبيق (بعد تطبيع النص) -> معلومات التطبيق،
 *    احتياطي لما ما يكونش عندنا اسم حزمة معروف من الباكند ولا من تثبيت سابق.
 *
 * يجب استدعاء scan() على Dispatchers.IO (عملية ممكن تاخد وقت على أجهزة فيها
 * تطبيقات كثيرة). النتيجة كتبقى مخزّنة هنا (in-memory) للقراءة السريعة من
 * InstallStateResolver بلا الحاجة لإعادة الفحص فكل مرة.
 *
 * إضافة: نتيجة كل فحص كتتسجل فـ SharedPreferences (loadCachedSnapshot/
 * persistSnapshot). هادشي كيسمح لـ MainViewModel يعمّر الحالة بأحدث نتيجة
 * معروفة فوريًا عند بدء التطبيق (قراءة سريعة، بلا مسح PackageManager الثقيل)،
 * بدل ما يبقى زر "فتح" غايب لبضع ثواني حتى يكمل الفحص الكامل الجديد.
 */
object InstalledAppsScanner {

    @Volatile private var byPackage: Map<String, InstalledAppInfo> = emptyMap()
    @Volatile private var byNormalizedName: Map<String, InstalledAppInfo> = emptyMap()

    private const val PREFS_NAME = "shizu_installed_apps_snapshot"
    private const val KEY_SNAPSHOT = "snapshot_json"

    /** تطبيع اسم التطبيق للمقارنة: حروف صغيرة، بلا مسافات ولا رموز، حتى "My App!" و"my app" يتطابقو. */
    fun normalizeName(raw: String): String =
        raw.trim().lowercase().replace(Regex("[^\\p{L}\\p{Nd}]"), "")

    fun scan(context: Context): Map<String, InstalledAppInfo> {
        val pm = context.packageManager
        val packages = try {
            pm.getInstalledPackages(PackageManager.GET_META_DATA)
        } catch (_: Exception) {
            emptyList()
        }

        val pkgMap = HashMap<String, InstalledAppInfo>(packages.size)
        val nameMap = HashMap<String, InstalledAppInfo>(packages.size)

        for (p in packages) {
            val appInfo = p.applicationInfo ?: continue
            val label = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                p.packageName
            }
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                p.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                p.versionCode.toLong()
            }
            val info = InstalledAppInfo(
                packageName = p.packageName,
                label = label,
                versionName = p.versionName,
                versionCode = versionCode
            )
            pkgMap[p.packageName] = info

            val key = normalizeName(label)
            // إلا تكرر نفس الاسم المطبَّع لتطبيقين مختلفين (نادر)، كنحتفظو بأول
            // واحد لقيناه فقط، تفاديًا لأي غموض فالمطابقة الاحتياطية بالاسم.
            if (key.isNotBlank() && !nameMap.containsKey(key)) {
                nameMap[key] = info
            }
        }

        byPackage = pkgMap
        byNormalizedName = nameMap
        persistSnapshot(context, pkgMap)
        return pkgMap
    }

    fun currentByPackage(): Map<String, InstalledAppInfo> = byPackage
    fun currentByNormalizedName(): Map<String, InstalledAppInfo> = byNormalizedName

    /**
     * كنعمّرو الخرائط بآخر نتيجة فحص محفوظة (من تشغيلة سابقة)، بلا مسح
     * PackageManager الحقيقي. غير تقريبية/قد تكون قديمة شوية — الغرض منها
     * سد الفترة القصيرة بين بداية التطبيق واكتمال scan() الحقيقي، ماشي بديل
     * عنه. إذا كانت الخرائط الحية عامرة بالفعل، ما كتديرش والو (مافيش داعي).
     */
    fun loadCachedSnapshot(context: Context) {
        if (byPackage.isNotEmpty()) return
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_SNAPSHOT, null) ?: return
            val arr = JSONArray(json)
            val pkgMap = HashMap<String, InstalledAppInfo>(arr.length())
            val nameMap = HashMap<String, InstalledAppInfo>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val packageName = o.optString("p")
                if (packageName.isBlank()) continue
                val info = InstalledAppInfo(
                    packageName = packageName,
                    label = o.optString("l", packageName),
                    versionName = if (o.has("v")) o.optString("v") else null,
                    versionCode = o.optLong("c", 0L)
                )
                pkgMap[info.packageName] = info
                val key = normalizeName(info.label)
                if (key.isNotBlank() && !nameMap.containsKey(key)) nameMap[key] = info
            }
            byPackage = pkgMap
            byNormalizedName = nameMap
        } catch (_: Exception) {
            // كاش تالف أو غير موجود — ماشي مشكلة، scan() الحقيقي غادي يعمّر كلشي بعد قليل
        }
    }

    private fun persistSnapshot(context: Context, pkgMap: Map<String, InstalledAppInfo>) {
        try {
            val arr = JSONArray()
            for (info in pkgMap.values) {
                val o = JSONObject()
                o.put("p", info.packageName)
                o.put("l", info.label)
                if (info.versionName != null) o.put("v", info.versionName)
                o.put("c", info.versionCode)
                arr.put(o)
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_SNAPSHOT, arr.toString()).apply()
        } catch (_: Exception) {
            // فشل الحفظ ماشي حرج — غير هنعاودو الفحص الكامل فالتشغيلة الجاية
        }
    }
}

