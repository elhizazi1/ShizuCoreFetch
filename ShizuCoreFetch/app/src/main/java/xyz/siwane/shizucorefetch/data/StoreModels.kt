package xyz.siwane.shizucorefetch.data

import com.squareup.moshi.JsonClass

/**
 * إصدار واحد من إصدارات التطبيق (يُستخدم لأحدث إصدار وللسجل السابق).
 * يطابق بنية release_history المبنية في GAS من نفس استجابة /releases (بدون طلبات إضافية).
 */
@JsonClass(generateAdapter = true)
data class ReleaseInfo(
    val versionTag: String = "",
    val apkUrl: String = "",
    val apkSize: Long = 0,
    val releasedAt: String = "",
    val releaseNotes: String = ""
)

/**
 * يطابق تمامًا كل حقل يُخرجه GAS (updateStoreData -> buildAppsList) في shizuku_pro_store_data.json
 * الذي يعرضه الوركر على corefetch.siwane.xyz. التطبيق هنا "مرآة" لهذا الملف: لا يُعاد حساب أو تخمين
 * أي شيء لم يصرّح به الباكند صراحة.
 *
 * كل الحقول لها قيمة افتراضية آمنة حتى لا ينهار التحليل (parsing) إن غاب حقل ما مستقبلاً.
 */
@JsonClass(generateAdapter = true)
data class StoreAppDto(
    val id: String = "",
    val name: String = "",
    val developer: String = "",
    val description: String = "",
    val descriptionAr: String = "",
    val category: String = "",
    val categoryAr: String = "",
    val iconUrl: String = "",
    val bannerUrl: String = "",
    val bannerUrlAr: String = "",
    val downloadUrl: String = "",
    val appWebsite: String = "",
    val developerBanner: String = "",
    val developerMessage: String = "",
    val developerNameAr: String = "",
    val developerMessageAr: String = "",
    val has_ads_flag: Boolean = false,
    val adApproved: Boolean = true,
    val hasJsonStore: Boolean = false,
    val stars: Int = 0,
    val downloads: Int = 0,
    val updated_at: String = "",
    val data_source: String = "readme",
    // ==================== حقول أُضيفت لاحقًا في GAS (اختيارية بالكامل) ====================
    val longDescription: String = "",
    val descriptionFormat: String = "txt", // txt | md | html
    val packageName: String = "",
    val featuredTier: Int? = null, // 0 = json+توبيك shizucorefetch، 1 = json فقط، null = بدون بانر مميز
    val requiresShizuku: Boolean = false, // <-- تمت الإضافة هنا لتطابق الباكند
    val apkUrl: String = "",
    val apkSize: Long = 0,
    val versionTag: String = "",
    val releasedAt: String = "",
    val releaseNotes: String = "",
    val previousReleases: List<ReleaseInfo> = emptyList()
)
