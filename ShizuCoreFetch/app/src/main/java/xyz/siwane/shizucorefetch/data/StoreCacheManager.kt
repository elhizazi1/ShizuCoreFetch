package xyz.siwane.shizucorefetch.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import xyz.siwane.shizucorefetch.network.NetworkModule
import java.io.File

class StoreCacheManager(context: Context) {

    private val appContext = context.applicationContext
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, StoreAppDto::class.java)
    private val listAdapter = moshi.adapter<List<StoreAppDto>>(listType)

    private val cacheFile: File by lazy { File(appContext.filesDir, "store_smart_cache.json") }
    private val cacheMetaPrefs by lazy { appContext.getSharedPreferences("store_smart_meta", Context.MODE_PRIVATE) }

    private var memoryCache: List<StoreAppDto>? = null

    companion object {
        // تم التوجيه إلى المسار الخفيف الخاص بالمتجر
        private const val ENDPOINT = "https://corefetch.siwane.xyz/?action=list"
        private const val CACHE_TTL_MS = 1 * 60 * 60 * 1000L 
        private const val KEY_LAST_FETCH = "last_smart_fetch"
    }

    suspend fun getStoreApps(forceRefresh: Boolean = false): Result<List<StoreAppDto>> =
        withContext(Dispatchers.IO) {
            val lastFetch = cacheMetaPrefs.getLong(KEY_LAST_FETCH, 0L)
            val isFresh = (System.currentTimeMillis() - lastFetch) < CACHE_TTL_MS

            if (!forceRefresh && isFresh && memoryCache != null) {
                return@withContext Result.success(memoryCache!!)
            }

            if (!forceRefresh && isFresh) {
                readDiskCache()?.let { 
                    memoryCache = it
                    return@withContext Result.success(it) 
                }
            }

            try {
                val request = Request.Builder().url(ENDPOINT).get().build()
                NetworkModule.plainOkHttpClient.newCall(request).execute().use { res ->
                    if (!res.isSuccessful) throw Exception("HTTP ${res.code}")
                    val body = res.body?.string()?.takeIf { it.isNotBlank() }
                        ?: throw Exception("استجابة فارغة من الخادم")
                    val apps = listAdapter.fromJson(body)
                        ?: throw Exception("تعذّر قراءة بيانات المتجر.")
                    
                    cacheFile.writeText(body)
                    cacheMetaPrefs.edit().putLong(KEY_LAST_FETCH, System.currentTimeMillis()).apply()
                    memoryCache = apps
                    Result.success(apps)
                }
            } catch (e: Exception) {
                readDiskCache()?.let { 
                    memoryCache = it
                    return@withContext Result.success(it) 
                }
                Result.failure(e)
            }
        }

    private fun readDiskCache(): List<StoreAppDto>? {
        return try {
            if (!cacheFile.exists()) return null
            listAdapter.fromJson(cacheFile.readText())
        } catch (_: Exception) {
            null
        }
    }
}
