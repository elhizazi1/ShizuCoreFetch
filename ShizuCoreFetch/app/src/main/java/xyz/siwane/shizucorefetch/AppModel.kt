package xyz.siwane.shizucorefetch

import com.google.gson.annotations.SerializedName

data class AppModel(
    val id: String,
    val name: String,
    val developer: String,
    val iconUrl: String, 
    val downloadUrl: String,
    val hasJsonStore: Boolean = false,
    val stars: Int = 0,
    val downloads: Long = 0,
    val updated_at: String = "",
    
    @SerializedName("adApproved", alternate = ["ad_approved"])
    val adApproved: Boolean = false,
    
    @SerializedName("has_ads_flag")
    val hasAdsFlag: Boolean = false,

    // الحقول التي قد تكون مفقودة في JSON نحددها كـ Nullable (؟)
    @SerializedName("description") private val _description: String? = null,
    @SerializedName("descriptionAr") private val _descriptionAr: String? = null, 
    @SerializedName("category") private val _category: String? = null,
    @SerializedName("categoryAr") private val _categoryAr: String? = null,
    @SerializedName("bannerUrl") private val _bannerUrl: String? = null,     
    @SerializedName("bannerUrlAr") private val _bannerUrlAr: String? = null,   
    @SerializedName("developerMessage") private val _developerMessage: String? = null,
    @SerializedName("developerMessageAr") private val _developerMessageAr: String? = null,
    @SerializedName("developerNameAr") private val _developerNameAr: String? = null,
    @SerializedName("developerBanner") private val _developerBanner: String? = null
) {
    // دوال جلب آمنة (Safe Getters) تمنع الكراش نهائياً
    val description: String get() = _description ?: ""
    val descriptionAr: String get() = _descriptionAr ?: ""
    val category: String get() = _category ?: ""
    val categoryAr: String get() = _categoryAr ?: ""
    val bannerUrl: String get() = _bannerUrl ?: ""
    val bannerUrlAr: String get() = _bannerUrlAr ?: ""
    val developerMessage: String get() = _developerMessage ?: ""
    val developerMessageAr: String get() = _developerMessageAr ?: ""
    val developerNameAr: String get() = _developerNameAr ?: ""
    val developerBanner: String get() = _developerBanner ?: ""
}
