package xyz.siwane.shizucorefetch.ui.screens

import androidx.compose.material.icons.filled.Done
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import xyz.siwane.shizucorefetch.R
import xyz.siwane.shizucorefetch.data.AppFullMetadata
import xyz.siwane.shizucorefetch.data.InstallUiState
import xyz.siwane.shizucorefetch.viewmodel.MainViewModel

// ==========================================
// النماذج والحالات المشتركة
// ==========================================

enum class AppState {
    INSTALL, OPEN, UPDATE
}

// InstallUiState دابا معرّفة مرة وحدة فـ data/InstallModels.kt (مستوردة تحت)،
// باش تكون نفس الحالة مشتركة بين StoreScreen وAppDetailsScreen وLibraryScreen
// بلا تكرار أو احتمال تضارب بين شاشة وأخرى.

// نموذج التنبيه السحابي (Remote Announcement)
data class StoreAnnouncement(
    val id: Int,
    val isVisible: Boolean,
    val type: String,
    val isCompact: Boolean,
    val isOutlined: Boolean,
    val messages: Map<String, String>
)

data class DummyApp(
    val name: String,
    val desc: String,
    val categoryRes: Int, 
    val category: String, 
    val downloads: String,
    val size: String,
    val stars: String,
    val requiresShizuku: Boolean,
    val state: AppState,
    val repoOwner: String = "",
    val repoName: String = "",
    val htmlUrl: String = "",
    val topics: List<String> = emptyList(),
    val hasStoreConfig: Boolean = false,
    val starsCount: Int = 0,
    val forksCount: Int = 0,
    val appId: String = "",
    val iconUrl: String = "",
    val bannerImageUrl: String = "",
    val longDescription: String = "",
    val descriptionFormat: String = "txt",
    val packageName: String = "",
    val featuredTier: Int? = null,
    val apkUrl: String = "",
    val apkSizeBytes: Long = 0,
    val versionTag: String = "",
    val releasedAt: String = "",
    val releaseNotes: String = "",
    val previousReleases: List<xyz.siwane.shizucorefetch.data.ReleaseInfo> = emptyList(),
    val downloadsCount: Int = 0,
    val hasAds: Boolean = false,
    val adApproved: Boolean = true,
    val appWebsite: String = "",
    val developerMessage: String = "",
    val dataSource: String = "readme"
)

// دالة تنسيق الأرقام (k و M)
fun formatCompactNumber(count: Int): String {
    return when {
        count < 1000 -> count.toString()
        count < 1_000_000 -> String.format(Locale.US, "%.1fk", count / 1000.0).replace(".0k", "k")
        else -> String.format(Locale.US, "%.1fM", count / 1_000_000.0).replace(".0M", "M")
    }
}

fun xyz.siwane.shizucorefetch.data.StoreAppDto.toDummyApp(languageIsArabic: Boolean = false): DummyApp {
    val sizeDisplay = when {
        this.apkSize <= 0 -> "-"
        this.apkSize >= 1024L * 1024L -> String.format("%.1f MB", this.apkSize / (1024.0 * 1024.0))
        else -> String.format("%.0f KB", this.apkSize / 1024.0)
    }
    
    val downloadsDisplay = formatCompactNumber(this.downloads)
    val starsDisplay = formatCompactNumber(this.stars)
    
    val requiresShizuku = this.requiresShizuku 
    val realCategory = when {
        languageIsArabic && this.categoryAr.isNotBlank() -> this.categoryAr
        this.category.isNotBlank() -> this.category
        else -> "Utility"
    }
    val realDesc = if (languageIsArabic && this.descriptionAr.isNotBlank()) this.descriptionAr else this.description
    return DummyApp(
        name = this.name,
        desc = realDesc,
        categoryRes = R.string.cat_utility,
        category = realCategory,
        downloads = downloadsDisplay,
        size = sizeDisplay,
        stars = starsDisplay,
        requiresShizuku = requiresShizuku,
        state = AppState.INSTALL,
        repoOwner = this.developer,
        repoName = this.name,
        htmlUrl = this.downloadUrl,
        topics = emptyList(),
        hasStoreConfig = this.hasJsonStore,
        starsCount = this.stars,
        forksCount = 0,
        appId = this.id,
        iconUrl = this.iconUrl,
        bannerImageUrl = if (languageIsArabic && this.bannerUrlAr.isNotBlank()) this.bannerUrlAr else this.bannerUrl,
        longDescription = this.longDescription,
        descriptionFormat = this.descriptionFormat,
        packageName = this.packageName,
        featuredTier = this.featuredTier,
        apkUrl = this.apkUrl,
        apkSizeBytes = this.apkSize,
        versionTag = this.versionTag,
        releasedAt = this.releasedAt,
        releaseNotes = this.releaseNotes,
        previousReleases = this.previousReleases,
        downloadsCount = this.downloads,
        hasAds = this.has_ads_flag && this.adApproved,
        adApproved = this.adApproved,
        appWebsite = this.appWebsite,
        developerMessage = if (languageIsArabic && this.developerMessageAr.isNotBlank()) this.developerMessageAr else this.developerMessage,
        dataSource = this.data_source
    )
}

fun xyz.siwane.shizucorefetch.network.GitHubRepo.toDummyApp(totalDownloads: Int? = null): DummyApp {
    val sizeKb = this.size
    val sizeDisplay = when {
        sizeKb <= 0 -> "-"
        sizeKb >= 1024 -> String.format("%.1f MB", sizeKb / 1024.0)
        else -> "$sizeKb KB"
    }
    val requiresShizuku = this.topics?.any { it.equals("shizuku", ignoreCase = true) } == true
    val isShizuCoreFetch = this.topics?.any { it.equals("shizucorefetch", ignoreCase = true) } == true
    
    val downloadsDisplay = totalDownloads?.let { formatCompactNumber(it) } ?: "-"
    val starsDisplay = formatCompactNumber(this.stargazers_count)

    return DummyApp(
        name = this.name,
        desc = this.description ?: "",
        categoryRes = R.string.cat_utility,
        category = this.language ?: "Utility",
        downloads = downloadsDisplay,
        size = sizeDisplay,
        stars = starsDisplay,
        requiresShizuku = requiresShizuku,
        state = AppState.INSTALL,
        repoOwner = this.owner?.login ?: "",
        repoName = this.name,
        htmlUrl = this.html_url,
        topics = this.topics ?: emptyList(),
        hasStoreConfig = isShizuCoreFetch,
        starsCount = this.stargazers_count,
        forksCount = this.forks_count
    )
}

fun formatAppDescription(raw: String, format: String?): String {
    if (raw.isBlank()) return ""
    val effectiveFormat = if (format.isNullOrBlank() || format == "auto") {
        val looksMarkdown = raw.contains(Regex("^#{1,6}\\s", RegexOption.MULTILINE)) ||
            raw.contains("](") || raw.contains("**") || raw.contains("`") || raw.trimStart().startsWith("- ")
        val looksHtml = raw.contains(Regex("<(p|h[1-6]|strong|b|i|em|br|ul|ol|li|div|span|a)\\b[^>]*>", RegexOption.IGNORE_CASE))
        when {
            looksMarkdown -> "md"
            looksHtml -> "html"
            else -> "md" 
        }
    } else format
    val formatted = when (effectiveFormat.lowercase()) {
        "html" -> raw
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
        "md", "markdown" -> raw
            .replace(Regex("^#{1,6}\\s*", RegexOption.MULTILINE), "")
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("`{1,3}(.*?)`{1,3}"), "$1")
            .replace(Regex("!\\[.*?]\\(.*?\\)"), "")
            .replace(Regex("\\[(.*?)]\\(.*?\\)"), "$1")
            .trim()
        else -> raw.trim()
    }
    return formatted.replace(Regex("</?[a-zA-Z][^<>]*>"), "").trim()
}

// ==========================================
// حالة انقطاع الشبكة (Offline) — انتقال متدرج بدل اختفاء مفاجئ للبطاقات
// ==========================================

/**
 * NORMAL: كلشي عادي (متصل، أو ماكاينش فحص جاري).
 * TRANSITIONING: انقطعت الشبكة توًا — كنعرضو سكيليتون (دومي تكست) بدل ما
 *   البطاقات الحقيقية تختفي فجأة، لمدة محدودة (انظر StoreScreen).
 * OFFLINE: الشبكة بقات منقطعة بعد فترة الانتقال — واجهة أوفلاين كاملة بدل المتجر.
 */
private enum class OfflinePhase { NORMAL, TRANSITIONING, OFFLINE }

/**
 * محتوى السكيليتون الحقيقي لبطاقات المتجر (بانر + فلاتر + بطاقات التطبيقات
 * بنفس الشكل والأبعاد الحقيقية). هادا المكوّن مستعمل فحالتين:
 * 1) التحميل الأول للمتجر (isLoading && apps.isEmpty()).
 * 2) الانتقال المتدرج عند انقطاع الشبكة (OfflinePhase.TRANSITIONING) —
 *    باش الدومي تكست يحاكي شكل البطاقات الحقيقية بدل سكيليتون مبسّط
 *    ما عندوش علاقة بشكل واجهة المتجر.
 */
@Composable
private fun StoreCardsSkeletonContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
        LazyRow(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = false // نمنع التمرير هنا لكي يظل مجرد ظل ثابت
        ) {
            item {
                Box(modifier = Modifier.width(320.dp).height(160.dp).clip(RoundedCornerShape(24.dp)).shimmerEffect())
            }
            item {
                // هذا البانر سيظهر الآن مقصوصاً من جهة اليمين تماماً كالبانر الحقيقي
                Box(modifier = Modifier.width(320.dp).height(160.dp).clip(RoundedCornerShape(24.dp)).shimmerEffect())
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                Box(modifier = Modifier.weight(1f).height(72.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect())
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) {
                Box(modifier = Modifier.width(if (it == 0) 60.dp else 90.dp).height(38.dp).clip(RoundedCornerShape(50)).shimmerEffect())
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            repeat(4) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).shimmerEffect())
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth(0.9f).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(modifier = Modifier.width(80.dp).height(36.dp).clip(RoundedCornerShape(50)).shimmerEffect())
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(3) {
                                Box(modifier = Modifier.width(70.dp).height(24.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreSkeletonFullState(modifier: Modifier = Modifier) {
    StoreCardsSkeletonContent(modifier = modifier)
}

@Composable
private fun StoreOfflineFullState(modifier: Modifier = Modifier) {
    // LazyColumn (بدل Box ثابت) عمداً: باش يبقى المحتوى "قابل للسحب" ويوصل
    // السحب من فوق لـ nestedScroll ديال pullRefreshState فـ StoreScreen — Box
    // ثابت بلا محتوى قابل للتمرير ما كان كيوصّل حركة السحب للتحديث (Pull-to-
    // refresh) للأعلى، فكانت واجهة الأوفلاين ما تقدرش تتحدّث بالسحب.
    LazyColumn(modifier = modifier) {
        item {
            Box(
                modifier = Modifier.fillParentMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        // أيقونة WiFi Off: انقطاع الاتصال الحقيقي بشبكة الجهاز (offline) —
                        // مختلفة عن أيقونة Cloud المستعملة فحالة loadError (تعذر الوصول
                        // للسيرفر/Cloudflare Worker رغم وجود اتصال بالإنترنت).
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.store_offline_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// مكون وميض التحميل الاحترافي (Shimmer Effect)
// ==========================================

fun Modifier.shimmerEffect(): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    val shimmerColor1 = if (isDark) Color(0xFF2C2C2C) else Color(0xFFE0E0E0)
    val shimmerColor2 = if (isDark) Color(0xFF3A3A3A) else Color(0xFFF5F5F5)
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )

    val brush = Brush.linearGradient(
        colors = listOf(shimmerColor1, shimmerColor2, shimmerColor1),
        start = Offset(startOffsetX, 0f),
        end = Offset(startOffsetX + 500f, 1000f)
    )

    background(brush)
}

// ==========================================
// مكونات واجهة المتجر الأساسية
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel? = null,
    featuredApps: List<DummyApp> = emptyList(),
    apps: List<DummyApp> = emptyList(),
    isLoading: Boolean = false,
    loadError: String? = null,
    selectedCategory: String? = null,
    availableCategories: List<String> = emptyList(),
    isShizukuGranted: Boolean = false,
    isSilentInstallEnabled: Boolean = true,
    isSearchActive: Boolean = false, // 👈 تم إضافة حالة البحث هنا
    listState: LazyListState = rememberLazyListState(),
    onCategorySelect: (String?) -> Unit = {},
    onRetry: () -> Unit = {},
    onAppClick: (DummyApp) -> Unit
) {
    val context = LocalContext.current

    // جلب لغة التطبيق لضمان التحديث الفوري
    val languageCode by (mainViewModel?.languageCode?.collectAsState() ?: remember { mutableStateOf("en") })

    // حالة الشبكة الحقيقية + انتقال متدرج بدل اختفاء مفاجئ للبطاقات: عند
    // الانقطاع كنعرضو سكيليتون لمدة قصيرة (بدل فراغ فجأة)، وإلا بقات الشبكة
    // منقطعة بعد هاد المدة، كنعرضو واجهة أوفلاين كاملة عوض المتجر.
    val isOnline by (mainViewModel?.isOnline?.collectAsState() ?: remember { mutableStateOf(true) })
    var offlinePhase by remember { mutableStateOf(OfflinePhase.NORMAL) }
    LaunchedEffect(isOnline) {
        if (isOnline) {
            offlinePhase = OfflinePhase.NORMAL
        } else {
            offlinePhase = OfflinePhase.TRANSITIONING
            delay(8000)
            offlinePhase = if (!isOnline) OfflinePhase.OFFLINE else OfflinePhase.NORMAL
        }
    }
    
    var selectedFilter by remember { mutableStateOf("default") }
    var showInfoDialog by remember { mutableStateOf(false) } 
    
    var announcement by remember { mutableStateOf<StoreAnnouncement?>(null) }
    var isAnnouncementVisible by remember { mutableStateOf(false) }

    // جلب التنبيه السحابي بذكاء وتجاوز الكاش
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val timestamp = System.currentTimeMillis()
                val targetUrl = "https://raw.githubusercontent.com/elhizazi1/ShizuCoreFetch/refs/heads/main/announcement.json?t=$timestamp"
                
                val request = Request.Builder()
                    .url(targetUrl)
                    .cacheControl(okhttp3.CacheControl.FORCE_NETWORK) 
                    .build()
                    
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string()
                    if (!jsonStr.isNullOrBlank()) {
                        val rootObj = JSONObject(jsonStr)
                        val announcementsArray = rootObj.optJSONArray("announcements")
                        
                        if (announcementsArray != null) {
                            for (i in 0 until announcementsArray.length()) {
                                val annJson = announcementsArray.optJSONObject(i) ?: continue
                                
                                if (annJson.optBoolean("visible", false)) {
                                    val supportedLangs = listOf("en", "ar", "fr", "es", "pt", "tr", "cs", "ja", "hi", "ru", "zh")
                                    val messagesMap = mutableMapOf<String, String>()
                                    
                                    supportedLangs.forEach { lang ->
                                        messagesMap[lang] = annJson.optString(lang, "")
                                    }
                                    
                                    val fetchedAnn = StoreAnnouncement(
                                        id = annJson.optInt("id", 0),
                                        isVisible = true,
                                        type = annJson.optString("type", "info"),
                                        isCompact = annJson.optBoolean("is_compact", false),
                                        isOutlined = annJson.optBoolean("is_outlined", false),
                                        messages = messagesMap
                                    )
                                    
                                    withContext(Dispatchers.Main) {
                                        announcement = fetchedAnn
                                        val prefs = context.getSharedPreferences("ShizuCoreFetchPrefs", Context.MODE_PRIVATE)
                                        val dismissedId = prefs.getInt("dismissed_announcement_id", -1)
                                        isAnnouncementVisible = (dismissedId != fetchedAnn.id)
                                    }
                                    break 
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // الصمت في حالة الخطأ
            }
        }
    }

    val processedApps = remember(apps, selectedFilter) {
        when (selectedFilter) {
            "top_rated" -> apps.sortedByDescending { it.starsCount }.take(5)
            "top_downloads" -> apps.sortedByDescending { it.downloadsCount }.take(5)
            "trusted" -> apps.filter { it.hasStoreConfig }.sortedByDescending { it.starsCount }
            else -> apps
        }
    }

    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            onRetry()
        }
    }
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            pullRefreshState.endRefresh()
        }
    }

    Box(modifier = modifier.fillMaxSize().nestedScroll(pullRefreshState.nestedScrollConnection)) {
        when (offlinePhase) {
            OfflinePhase.OFFLINE -> StoreOfflineFullState(modifier = Modifier.fillMaxSize())
            OfflinePhase.TRANSITIONING -> StoreSkeletonFullState(modifier = Modifier.fillMaxSize())
            OfflinePhase.NORMAL ->
        // listState محقونة من الأعلى (MainScreen) ماشي rememberLazyListState()
        // محلية هنا — هادشي هو سبب رجوع السكرول لفوق عند فتح تفاصيل تطبيق والرجوع:
        // StoreScreen كان كيخرج من composition بالكامل (Crossfade/التبديل الشرطي)،
        // فأي remember محلي هنا كان كيتصفّى ويترجع للصفر. دابا الحالة معاشة فمستوى
        // MainScreen اللي ماكيخرجش من composition، فكتبقى محفوظة.
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // التنبيه السحابي الديناميكي (Dynamic Live Announcement) بتخصيص الواجهة
                    AnimatedVisibility(
                        visible = isAnnouncementVisible,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        announcement?.let { ann ->
                            val currentMessage = ann.messages[languageCode]?.takeIf { it.isNotBlank() } ?: ann.messages["en"] ?: ""
                            
                            if (currentMessage.isNotBlank()) {
                                // تحديد الألوان والأيقونات ديناميكياً بناءً على نوع الرسالة
                                val (bgColor, contentColor, icon) = when (ann.type.lowercase()) {
                                    "error" -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Icons.Default.Warning)
                                    "warning" -> Triple(Color(0xFFFFF0C2), Color(0xFFB8860B), Icons.Default.Warning)
                                    "success" -> Triple(Color(0xFFD4EDDA), Color(0xFF155724), Icons.Default.Done)
                                    "update" -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Icons.Default.Campaign)
                                    else -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Icons.Default.Info)
                                }

                                val shape = if (ann.isCompact) RoundedCornerShape(50) else RoundedCornerShape(16.dp)
                                val cardBorder = if (ann.isOutlined) BorderStroke(1.5.dp, contentColor.copy(alpha = 0.8f)) else null
                                val cardBgColor = if (ann.isOutlined) Color.Transparent else bgColor

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = if (ann.isCompact) 24.dp else 16.dp,
                                            vertical = 8.dp
                                        ),
                                    shape = shape,
                                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                    border = cardBorder,
                                    elevation = CardDefaults.cardElevation(0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = if (ann.isCompact) 20.dp else 16.dp,
                                                vertical = if (ann.isCompact) 12.dp else 16.dp
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = contentColor,
                                            modifier = Modifier.size(if (ann.isCompact) 24.dp else 28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = currentMessage,
                                            style = if (ann.isCompact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                                            color = contentColor,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                isAnnouncementVisible = false
                                                context.getSharedPreferences("ShizuCoreFetchPrefs", Context.MODE_PRIVATE)
                                                    .edit()
                                                    .putInt("dismissed_announcement_id", ann.id)
                                                    .apply()
                                            },
                                            modifier = Modifier.size(24.dp),
                                            colors = IconButtonDefaults.iconButtonColors(contentColor = contentColor)
                                        ) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isLoading && apps.isEmpty()) {
                item {
                    StoreCardsSkeletonContent()
                }
            } 
            else {
                if (!isSearchActive && featuredApps.isNotEmpty()) {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 0.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(featuredApps) { app ->
                                FeaturedAppBanner(
                                    app = app,
                                    mainViewModel = mainViewModel,
                                    onClick = { onAppClick(app) }
                                )
                            }
                        }
                    }
                }

                if (!isSearchActive && (apps.isNotEmpty() || selectedFilter != "default")) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val filters = listOf(
                                Triple("trusted", stringResource(id = R.string.filter_trusted), Icons.Default.WorkspacePremium),
                                Triple("top_rated", stringResource(id = R.string.filter_top_rated), Icons.Default.Star),
                                Triple("top_downloads", stringResource(id = R.string.filter_top_downloads), Icons.Default.Download)
                            )
                            
                            filters.forEach { (id, label, icon) ->
                                val isSelected = selectedFilter == id
                                val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            selectedFilter = if (selectedFilter == id) "default" else id
                                        },
                                    colors = CardDefaults.cardColors(containerColor = bgColor),
                                    elevation = CardDefaults.cardElevation(0.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(imageVector = icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = contentColor,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isSearchActive) {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                val isSelected = selectedCategory == null
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { 
                                            onCategorySelect(null)
                                            selectedFilter = "default" 
                                        }
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.cat_all),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            items(availableCategories) { categoryText ->
                                val isSelected = categoryText == selectedCategory
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { onCategorySelect(categoryText) }
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = categoryText,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (loadError != null && apps.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // أيقونة سحابة (Cloud) لخطأ السيرفر/الرابط — مختلفة عن أيقونة
                            // WifiOff الخاصة بانقطاع شبكة الجهاز (StoreOfflineFullState)، باش
                            // يميز المستخدم بصريًا بين مشكل فالسيرفر ومشكل فالشبكة.
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = loadError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            OutlinedButton(onClick = onRetry) {
                                Text(stringResource(id = R.string.action_retry))
                            }
                        }
                    }
                } else if (processedApps.isEmpty() && !isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(id = R.string.store_no_results),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(processedApps, key = { it.appId.ifBlank { it.name } }) { app ->
                    AppListItem(
                        app = app, 
                        mainViewModel = mainViewModel,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        isShizukuGranted = isShizukuGranted,
                        isSilentInstallEnabled = isSilentInstallEnabled,
                        onClick = { onAppClick(app) }
                    )
                }
            }
        }
        }
        
        PullToRefreshContainer(
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // رسالة التوضيح (Info Dialog) مع حل مشكلة لغة النافذة!
        if (showInfoDialog) {
            // سياق مخصص لربط النافذة بلغة التطبيق
            val localizedContext = remember(languageCode) {
                val locale = Locale(languageCode)
                val config = Configuration(context.resources.configuration)
                config.setLocale(locale)
                context.createConfigurationContext(config)
            }

            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = {
                    Text(
                        text = localizedContext.getString(R.string.github_explanation_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(text = localizedContext.getString(R.string.github_explanation_body))
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showInfoDialog = false }
                    ) {
                        Text(text = localizedContext.getString(R.string.github_explanation_agree))
                    }
                }
            )
        }
    }
}

@Composable
fun FeaturedAppBanner(app: DummyApp, mainViewModel: MainViewModel?, onClick: () -> Unit) {
    var appMetadata by remember { mutableStateOf<AppFullMetadata?>(null) }
    LaunchedEffect(app.appId) {
        mainViewModel?.fetchAppMetadata(app) { metadata -> appMetadata = metadata }
    }
    
    val effectiveCategory = appMetadata?.category?.takeIf { it.isNotBlank() } ?: app.category
    val effectiveBanner = appMetadata?.bannerUrl?.takeIf { it.isNotBlank() } ?: app.bannerImageUrl

    Card(
        modifier = Modifier
            .width(320.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (effectiveBanner.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(effectiveBanner)
                        .crossfade(true)
                        .build(),
                    contentDescription = app.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.05f),
                                Color.Black.copy(alpha = 0.55f)
                            )
                        )
                    )
            )
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(0.6f)
                )

                BannerBadge(
                    text = effectiveCategory,
                    icon = Icons.Default.Category,
                    containerColor = Color(0xFF673AB7).copy(alpha = 0.85f),
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd)
                )

                BannerBadge(
                    text = app.downloads,
                    icon = Icons.Default.Download,
                    containerColor = Color(0xFF2196F3).copy(alpha = 0.85f),
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomStart)
                )

                BannerBadge(
                    text = app.stars,
                    icon = Icons.Default.Star,
                    containerColor = Color(0xFFFFC107).copy(alpha = 0.85f),
                    contentColor = Color(0xFF3E2723),
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }
}

// بادج صغير مخصص للبانر
@Composable
fun BannerBadge(text: String, icon: ImageVector, containerColor: Color, contentColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .border(1.dp, contentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = contentColor)
    }
}

@Composable
fun AppListItem(
    app: DummyApp,
    mainViewModel: MainViewModel?,
    modifier: Modifier = Modifier,
    isShizukuGranted: Boolean = false,
    isSilentInstallEnabled: Boolean = true,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    var appMetadata by remember { mutableStateOf<AppFullMetadata?>(null) }
    LaunchedEffect(app.appId) {
        mainViewModel?.fetchAppMetadata(app) { metadata -> appMetadata = metadata }
    }
    
    val effectiveCategory = appMetadata?.category?.takeIf { it.isNotBlank() } ?: app.category
    val effectiveDesc = appMetadata?.longDescription?.takeIf { it.isNotBlank() } ?: app.desc
    val effectiveIcon = appMetadata?.iconUrl?.takeIf { it.isNotBlank() } ?: app.iconUrl

    var formattedDesc by remember(app.appId) { 
        mutableStateOf(app.desc.replace('\n', ' ').replace('\r', ' ').replace(Regex("\\s+"), " ").trim()) 
    }
    
    LaunchedEffect(effectiveDesc) {
        withContext(Dispatchers.Default) {
            val processed = formatAppDescription(effectiveDesc, "auto")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace(Regex("\\s+"), " ")
                .take(120)
                .replace(Regex("[*#`]"), "")
                .trim()
            withContext(Dispatchers.Main) {
                formattedDesc = processed
            }
        }
    }

    // -------- الحالة الموحّدة (مصدر حقيقة وحيد) --------
    // installStates جاي من mainViewModel.installStates، بالضبط نفس المصدر اللي
    // كتقرا منه AppDetailsScreen وLibraryScreen. ماكاينش نسخة محلية هنا. وبما أن
    // عملية التحميل/التثبيت كتخدم فعليًا فـ DownloadInstallManager (scope خاص
    // بالتطبيق كاملة، ماشي بهاد الـ Composable)، خروج هاد العنصر من composition
    // (تمرير القائمة، تغيير الفلتر، الخ) ما كيوقفش أي تحميل جاري.
    val installStatesMap by (mainViewModel?.installStates?.collectAsState() ?: remember { mutableStateOf(emptyMap<String, InstallUiState>()) })
    val installUiState = installStatesMap[app.appId] ?: InstallUiState.Install

    fun performInstall() {
        if (app.apkUrl.isBlank()) {
            Toast.makeText(context, context.getString(R.string.store_download_failed), Toast.LENGTH_SHORT).show()
            return
        }
        mainViewModel?.installOrUpdateApp(app)
    }

    fun performOpen() {
        val opened = mainViewModel?.openInstalledApp(app) ?: false
        if (!opened) {
            Toast.makeText(context, context.getString(R.string.store_cannot_open), Toast.LENGTH_SHORT).show()
        }
    }

    val isBusy = installUiState is InstallUiState.Downloading || installUiState is InstallUiState.Installing
    val downloadProgress = (installUiState as? InstallUiState.Downloading)?.progress

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(effectiveIcon.ifBlank { null })
                            .crossfade(true)
                            .build(),
                        placeholder = painterResource(id = R.mipmap.ic_launcher_round),
                        error = painterResource(id = R.mipmap.ic_launcher_round),
                        fallback = painterResource(id = R.mipmap.ic_launcher_round),
                        contentDescription = app.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(if (isBusy) 46.dp else 56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    )
                    if (isBusy) {
                        if (downloadProgress != null) {
                            val currentProgress = downloadProgress ?: 0f
                            CircularProgressIndicator(
                                progress = { currentProgress },
                                modifier = Modifier.size(56.dp),
                                strokeWidth = 3.dp,
                                strokeCap = StrokeCap.Round,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(56.dp),
                                strokeWidth = 3.dp,
                                strokeCap = StrokeCap.Round,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                SmartActionButton(
                    state = installUiState,
                    isBusy = isBusy,
                    onInstallClick = { performInstall() },
                    onOpenClick = { performOpen() },
                    onUpdateClick = { performInstall() }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColoredItemBadge(
                    text = effectiveCategory, 
                    icon = Icons.Default.Category, 
                    containerColor = MaterialTheme.colorScheme.secondaryContainer, 
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                ColoredItemBadge(
                    text = app.downloads, 
                    icon = Icons.Default.Download, 
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer, 
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
                ColoredItemBadge(
                    text = app.size, 
                    icon = Icons.Default.SdStorage, 
                    containerColor = MaterialTheme.colorScheme.primaryContainer, 
                    contentColor = MaterialTheme.colorScheme.primary
                )
                
                val isDark = isSystemInDarkTheme()
                val starBg = if (isDark) Color(0xFF423300) else Color(0xFFFFF3CD)
                val starContent = if (isDark) Color(0xFFFFD54F) else Color(0xFFF39C12)
                ColoredItemBadge(
                    text = app.stars, 
                    icon = Icons.Default.Star, 
                    containerColor = starBg, 
                    contentColor = starContent
                )
                
                // بادج Shizuku أعيد إحياؤه!
                if (app.requiresShizuku) {
                    ColoredItemBadge(
                        text = stringResource(id = R.string.shizuku_label), 
                        icon = Icons.Default.Security, 
                        containerColor = MaterialTheme.colorScheme.errorContainer, 
                        contentColor = MaterialTheme.colorScheme.error
                    )
                }

                if (app.hasStoreConfig) {
                    val isDarkGold = isSystemInDarkTheme()
                    val goldBg = if (isDarkGold) Color(0xFF4A3B00) else Color(0xFFFFF0C2)
                    val goldContent = if (isDarkGold) Color(0xFFFFD54F) else Color(0xFFB8860B)
                    ColoredItemBadge(
                        text = stringResource(id = R.string.shizucorefetch_label),
                        icon = Icons.Default.WorkspacePremium,
                        containerColor = goldBg,
                        contentColor = goldContent
                    )
                }
            }
        }
    }
}

// البادج المصغر للتطبيقات 
@Composable
fun ColoredItemBadge(text: String, icon: ImageVector, containerColor: Color, contentColor: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = contentColor)
    }
}

@Composable
fun SmartActionButton(
    state: InstallUiState,
    isBusy: Boolean,
    onInstallClick: () -> Unit,
    onOpenClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
    when (state) {
        is InstallUiState.Open -> {
            OutlinedButton(
                onClick = onOpenClick,
                enabled = !isBusy,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(text = stringResource(id = R.string.action_open), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        is InstallUiState.Update -> {
            Button(
                onClick = onUpdateClick,
                enabled = !isBusy,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(text = stringResource(id = R.string.action_update), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }
        else -> {
            Button(
                onClick = onInstallClick,
                enabled = !isBusy,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(text = stringResource(id = R.string.details_install), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun GridAppItem(app: DummyApp, mainViewModel: MainViewModel?, modifier: Modifier = Modifier) {
    var appMetadata by remember { mutableStateOf<AppFullMetadata?>(null) }
    LaunchedEffect(app.appId) {
        mainViewModel?.fetchAppMetadata(app) { metadata -> appMetadata = metadata }
    }
    val effectiveIcon = appMetadata?.iconUrl?.takeIf { it.isNotBlank() } ?: app.iconUrl

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (effectiveIcon.isNotBlank()) {
            AsyncImage(
                model = effectiveIcon,
                placeholder = painterResource(id = R.mipmap.ic_launcher_round),
                error = painterResource(id = R.mipmap.ic_launcher_round),
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = app.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
