package xyz.siwane.shizucorefetch.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StoreAdItem(
    val position: String = "", 
    val image_url: String = "",
    val target_url: String = ""
)

@JsonClass(generateAdapter = true)
data class StoreDeveloperInfo(
    val name: String = "",
    val banner_url: String = "", 
    val account_url: String = "", 
    val email: String = "",
    val website: String = "",
    val portfolio: String = "",
    val donate_url: String = "", // <-- رابط الدعم موجود هنا ومؤمّن!
    val socials: Map<String, String> = emptyMap() 
)

@JsonClass(generateAdapter = true)
data class StoreLocaleOverride(
    val detailed_description: String? = null,
    val banner_url: String? = null,
    val icon_url: String? = null,
    val developer_message: String? = null,
    val developer_name: String? = null,
    val app_website: String? = null,
    val category: String? = null,
    val changelog: String? = null
)

@JsonClass(generateAdapter = true)
data class ShizuStoreFullConfig(
    val app_name: String = "", 
    val package_name: String = "", 
    val short_description: String = "", 
    val detailed_description: String = "",
    val banner_url: String = "",
    val icon_url: String = "",
    val developer_message: String = "",
    val app_website: String = "",
    val category: String = "",
    val store_issue_number: Int = 0,
    val min_sdk: Int = 0, 
    val target_sdk: Int = 0, // <-- الحقل الجديد
    val changelog: String = "", // <-- الحقل الجديد
    val tags: List<String> = emptyList(), 
    val license: String = "", 
    val requires_shizuku: Boolean = false, 
    val open_source: Boolean = false, 
    val ad: Boolean = false,
    val has_ads: Boolean = false,
    val ads: List<StoreAdItem> = emptyList(),
    val screenshots: List<String> = emptyList(),
    val developer: StoreDeveloperInfo? = null,
    val repo_url: String = "", 
    val locales: Map<String, StoreLocaleOverride> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class AppFullMetadata(
    val longDescription: String = "",
    val descriptionFormat: String = "auto", 
    val bannerUrl: String = "",
    val iconUrl: String = "",
    val category: String = "",
    val developerMessage: String = "",
    val appWebsite: String = "",
    val developerName: String = "",
    val developerInfo: StoreDeveloperInfo? = null,
    val ads: List<StoreAdItem> = emptyList(),
    val adsApproved: Boolean = false,
    val screenshots: List<String> = emptyList(),
    val storeIssueNumber: Int = 0,
    val minSdk: Int = 0, 
    val targetSdk: Int = 0, // <-- الحقل الجديد
    val changelog: String = "", // <-- الحقل الجديد
    val tags: List<String> = emptyList(), 
    val license: String = "", 
    val requiresShizuku: Boolean = false, 
    val openSource: Boolean = false, 
    val source: String = "none", 
    val fetchedAtMs: Long = 0L,
    val locales: Map<String, StoreLocaleOverride> = emptyMap()
)
