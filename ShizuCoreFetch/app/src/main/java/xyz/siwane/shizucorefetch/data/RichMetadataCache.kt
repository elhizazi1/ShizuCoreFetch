package xyz.siwane.shizucorefetch.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

class RichMetadataCache(context: Context) {

    private val appContext = context.applicationContext
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, AppFullMetadata::class.java)
    private val mapAdapter = moshi.adapter<Map<String, AppFullMetadata>>(mapType)

    private val cacheFile: File by lazy { File(appContext.filesDir, "rich_metadata_cache.json") }

    // إضافة كاش الذاكرة (RAM) لحل مشكلة التقطيع الشديد أثناء التمرير
    private var memoryCache: MutableMap<String, AppFullMetadata>? = null

    companion object {
        const val TTL_MS = 6 * 60 * 60 * 1000L // 6 ساعات
    }

    @Synchronized
    fun get(appId: String): AppFullMetadata? {
        ensureLoaded()
        return memoryCache?.get(appId)
    }

    @Synchronized
    fun isFresh(appId: String): Boolean {
        val entry = get(appId) ?: return false
        // إبطال الكاش فوراً إذا كانت البيانات المحفوظة فارغة (بسبب استنزاف 60 طلب سابقاً)
        if (entry.source == "none" && entry.longDescription.isBlank()) return false
        return (System.currentTimeMillis() - entry.fetchedAtMs) < TTL_MS
    }

    @Synchronized
    fun put(appId: String, metadata: AppFullMetadata) {
        ensureLoaded()
        // منع تخزين أي بيانات فارغة ناتجة عن حظر الشبكة
        if (metadata.source == "none" && metadata.longDescription.isBlank()) return
        
        memoryCache?.put(appId, metadata.copy(fetchedAtMs = System.currentTimeMillis()))
        saveToDisk()
    }

    private fun ensureLoaded() {
        if (memoryCache == null) {
            memoryCache = try {
                if (cacheFile.exists()) {
                    mapAdapter.fromJson(cacheFile.readText())?.toMutableMap() ?: mutableMapOf()
                } else mutableMapOf()
            } catch (_: Exception) {
                mutableMapOf()
            }
        }
    }

    private fun saveToDisk() {
        try {
            memoryCache?.let { cacheFile.writeText(mapAdapter.toJson(it)) }
        } catch (_: Exception) {}
    }
}
