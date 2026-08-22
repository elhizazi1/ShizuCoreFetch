package xyz.siwane.shizucorefetch.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import xyz.siwane.shizucorefetch.network.NetworkModule

object StoreConfig {
    val SEARCH_TOPICS = listOf("shizuku", "shizucorefetch")
    const val FASTLANE_METADATA_PATH = "fastlane/metadata/android/en-US"
    const val COMMENTS_ISSUE_LABEL = "shizu-store-comments"
    const val ADMIN_USERNAME = "elhizazi1"
}

class AppMetadataFetcher {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, AppFullMetadata::class.java)
    private val adapter = moshi.adapter<Map<String, AppFullMetadata>>(mapType)

    suspend fun fetchAndCache(owner: String, repo: String, languageCode: String): AppFullMetadata =
        withContext(Dispatchers.IO) {
            val url = "https://corefetch.siwane.xyz/?action=details"
            val request = Request.Builder().url(url).get().build()

            NetworkModule.plainOkHttpClient.newCall(request).execute().use { res ->
                if (!res.isSuccessful) throw Exception("HTTP ${res.code}")
                val body = res.body?.string() ?: throw Exception("استجابة فارغة من الخادم")
                
                val detailsMap = adapter.fromJson(body) ?: throw Exception("تعذر قراءة التفاصيل من الخادم")
                val appId = "${owner.lowercase()}/${repo.lowercase()}"
                
                val rawMetadata = detailsMap[appId] ?: throw Exception("لم يتم العثور على تفاصيل التطبيق في الخادم")
                
                // تطبيق الترجمة (Localization) برمجياً بناءً على لغة المستخدم الحالية
                var localizedDesc = rawMetadata.longDescription
                var localizedBanner = rawMetadata.bannerUrl
                var localizedIcon = rawMetadata.iconUrl
                var localizedDevMessage = rawMetadata.developerMessage
                var localizedDevName = rawMetadata.developerName
                var localizedWebsite = rawMetadata.appWebsite
                var localizedCategory = rawMetadata.category
                var localizedChangelog = rawMetadata.changelog

                rawMetadata.locales[languageCode]?.let { loc ->
                    loc.detailed_description?.let { if (it.isNotBlank()) localizedDesc = it }
                    loc.banner_url?.let { if (it.isNotBlank()) localizedBanner = it }
                    loc.icon_url?.let { if (it.isNotBlank()) localizedIcon = it }
                    loc.developer_message?.let { if (it.isNotBlank()) localizedDevMessage = it }
                    loc.developer_name?.let { if (it.isNotBlank()) localizedDevName = it }
                    loc.app_website?.let { if (it.isNotBlank()) localizedWebsite = it }
                    loc.category?.let { if (it.isNotBlank()) localizedCategory = it }
                    loc.changelog?.let { if (it.isNotBlank()) localizedChangelog = it }
                }

                return@withContext rawMetadata.copy(
                    longDescription = localizedDesc,
                    bannerUrl = localizedBanner,
                    iconUrl = localizedIcon,
                    developerMessage = localizedDevMessage,
                    developerName = localizedDevName,
                    appWebsite = localizedWebsite,
                    category = localizedCategory,
                    changelog = localizedChangelog,
                    source = "backend"
                )
            }
        }
}
