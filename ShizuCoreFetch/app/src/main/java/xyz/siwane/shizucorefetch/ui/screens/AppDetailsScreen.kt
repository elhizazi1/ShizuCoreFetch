package xyz.siwane.shizucorefetch.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.siwane.shizucorefetch.R
import xyz.siwane.shizucorefetch.apkmanager.ApkFileManager
import xyz.siwane.shizucorefetch.data.AppFullMetadata
import xyz.siwane.shizucorefetch.data.InstallUiState
import xyz.siwane.shizucorefetch.data.ReleaseInfo
import xyz.siwane.shizucorefetch.data.StoreDeveloperInfo
import xyz.siwane.shizucorefetch.viewmodel.MainViewModel

private enum class AppDetailsOfflinePhase { NORMAL, TRANSITIONING, OFFLINE }

/**
 * صفحة أوفلاين كاملة منفصلة لتفاصيل التطبيق — بنفس الروح البصرية لصفحة
 * StoreOfflineFullState فـ StoreScreen.kt (أيقونة WifiOff + عبارة + إمكانية
 * الرجوع للخلف)، لكن هنا بلا سحب-للتحديث لأن التحديث التلقائي كيوقع فوريًا
 * (LaunchedEffect(isOnline)) بمجرد رجوع الشبكة — ما كاين حتى داعي لسحب يدوي.
 */
@Composable
private fun AppDetailsOfflineFullState(appName: String, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(8.dp)
                .size(48.dp)
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = appName,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsScreen(
    app: DummyApp,
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit,
    onBackClick: () -> Unit,
    onAppClick: (DummyApp) -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    mainViewModel: MainViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // -------- الحالة الموحّدة (مصدر حقيقة وحيد) --------
    // installStates جاي من mainViewModel.installStates، نفس المصدر بالضبط اللي
    // كتقرا منه StoreScreen وLibraryScreen — بلا نسخة محلية خاصة بهاد الشاشة.
    // التحميل/التثبيت الحقيقي كيخدم فـ DownloadInstallManager (scope خاص
    // بالتطبيق كاملة)، فمغادرة هاد الشاشة (رجوع للخلف، أو حتى إغلاق التطبيق)
    // ما كيوقفش العملية الجارية — كتكمل فالخلفية بالضبط كيفما Google Play.
    val installStatesMap by (mainViewModel?.installStates?.collectAsState() ?: remember { mutableStateOf(emptyMap<String, InstallUiState>()) })
    val installUiState = installStatesMap[app.appId] ?: InstallUiState.Install
    var actionError by remember { mutableStateOf<String?>(null) }
    var commentText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // نلتقطو أخطاء التحميل/التثبيت الجاية من DownloadInstallManager (اللي كيخدم
    // دابا برا هاد الشاشة) باش نعرضوها هنا بالضبط كيفما قبل.
    LaunchedEffect(app.appId) {
        xyz.siwane.shizucorefetch.apkmanager.DownloadInstallManager.events.collect { event ->
            if (event is xyz.siwane.shizucorefetch.apkmanager.DownloadInstallManager.InstallEvent.Finished && event.appId == app.appId) {
                actionError = if (!event.success) (event.errorMessage ?: context.getString(R.string.store_error_generic)) else null
            }
        }
    }

    var fullscreenImageIndex by remember { mutableStateOf<Int?>(null) }
    var adUrlToConfirm by remember { mutableStateOf<String?>(null) }
    var showAllComments by remember { mutableStateOf(false) }

    val allStoreApps by (mainViewModel?.allStoreApps?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val featuredApps by (mainViewModel?.featuredStoreApps?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    
    val popularApps = remember(featuredApps, allStoreApps) {
        val base = if (featuredApps.isNotEmpty()) featuredApps else allStoreApps.sortedByDescending { it.starsCount }
        val filled = (base + allStoreApps.sortedByDescending { it.starsCount }).distinctBy { it.appId }.take(6)
        filled
    }
    
    val developerApps = remember(allStoreApps, app.repoOwner) {
        allStoreApps.filter { it.repoOwner.equals(app.repoOwner, ignoreCase = true) }
    }

    fun performInstall() {
        if (app.apkUrl.isBlank()) {
            actionError = context.getString(R.string.store_download_failed)
            return
        }
        actionError = null
        mainViewModel?.installOrUpdateApp(app)
    }

    fun performOpen() {
        val opened = mainViewModel?.openInstalledApp(app) ?: false
        if (!opened) actionError = context.getString(R.string.store_cannot_open)
    }

    fun performUninstall() {
        mainViewModel?.uninstallApp(app)
    }

    val languageCode by (mainViewModel?.languageCode?.collectAsState() ?: remember { mutableStateOf("ar") })
    var appMetadata by remember { mutableStateOf<AppFullMetadata?>(null) }
    
    var isMetadataLoading by remember { mutableStateOf(true) }
    // ادا فشل الجلب بلا كاش محفوظ (أوفلاين حقيقي، أو تعذر الوصول للسيرفر) —
    // قبل هادشي الاستثناء كان كيتبلع بصمت فـ MainViewModel.fetchAppMetadata
    // بلا ما ينادي onResult، فـ isMetadataLoading كان كيبقى true للأبد
    // (سبيلر لا نهائي، خصوصًا عند فتح تطبيق مثبت/محفوظ فالمكتبة بلا كاش
    // ورا سابق وهو أوفلاين).
    var metadataFetchFailed by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()

    // حالة الاتصال الحقيقية + انتقال متدرج (نفس منطق StoreScreen بالضبط):
    // TRANSITIONING لمدة قصيرة بدل اختفاء المحتوى فجأة، وإلا بقات الشبكة
    // منقطعة، صفحة أوفلاين كاملة منفصلة عوض تفاصيل التطبيق.
    val isOnline by (mainViewModel?.isOnline?.collectAsState() ?: remember { mutableStateOf(true) })
    var offlinePhase by remember { mutableStateOf(AppDetailsOfflinePhase.NORMAL) }
    LaunchedEffect(isOnline) {
        if (isOnline) {
            offlinePhase = AppDetailsOfflinePhase.NORMAL
        } else {
            offlinePhase = AppDetailsOfflinePhase.TRANSITIONING
            delay(8000)
            offlinePhase = if (!isOnline) AppDetailsOfflinePhase.OFFLINE else AppDetailsOfflinePhase.NORMAL
        }
    }

    LaunchedEffect(app.appId, languageCode) {
        isMetadataLoading = true
        metadataFetchFailed = false
        mainViewModel?.fetchAppMetadata(
            app = app,
            onFailure = {
                isMetadataLoading = false
                metadataFetchFailed = true
            }
        ) { metadata ->
            appMetadata = metadata
            isMetadataLoading = false
            metadataFetchFailed = false
        }
    }

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            isMetadataLoading = true
            metadataFetchFailed = false
            mainViewModel?.fetchAppMetadata(
                app = app,
                onFailure = {
                    isMetadataLoading = false
                    metadataFetchFailed = true
                    pullRefreshState.endRefresh()
                }
            ) { metadata ->
                appMetadata = metadata
                isMetadataLoading = false
                metadataFetchFailed = false
                pullRefreshState.endRefresh()
            }
            if (app.repoOwner.isNotBlank() && app.repoName.isNotBlank()) {
                mainViewModel?.loadComments(app.repoOwner, app.repoName)
                mainViewModel?.observeAppRating(app.repoOwner, app.repoName)
            }
        }
    }
    
    val effectiveCategory = appMetadata?.category?.takeIf { it.isNotBlank() } ?: app.category
    val effectiveDescription = appMetadata?.longDescription?.takeIf { it.isNotBlank() } ?: app.desc
    val effectiveDescriptionFormat = appMetadata?.descriptionFormat ?: app.descriptionFormat
    val effectiveDownloads = app.downloads
    val effectiveBanner = appMetadata?.bannerUrl?.takeIf { it.isNotBlank() } ?: app.bannerImageUrl.ifBlank { null }
    val effectiveAds = appMetadata?.ads.orEmpty().filter { appMetadata?.adsApproved == true || app.dataSource == "shizu_store.json" || app.dataSource == "shizu_store" }
    val effectiveScreenshots = appMetadata?.screenshots.orEmpty()

    var formattedDesc by remember(effectiveDescription, effectiveDescriptionFormat) { mutableStateOf("") }
    LaunchedEffect(effectiveDescription, effectiveDescriptionFormat) {
        withContext(Dispatchers.Default) {
            val processed = formatAppDescription(effectiveDescription, effectiveDescriptionFormat)
            withContext(Dispatchers.Main) {
                formattedDesc = processed
            }
        }
    }
    
    var showDeveloperProfile by remember { mutableStateOf(false) }

    val isGithubLoggedIn by (mainViewModel?.isGithubLoggedIn?.collectAsState() ?: remember { mutableStateOf(false) })
    val currentUsername by (mainViewModel?.githubUsername?.collectAsState() ?: remember { mutableStateOf("") })
    val currentAvatarUrl by (mainViewModel?.githubAvatarUrl?.collectAsState() ?: remember { mutableStateOf("") })
    val comments by (mainViewModel?.comments?.collectAsState() ?: remember { mutableStateOf(emptyList<xyz.siwane.shizucorefetch.network.GitHubComment>()) })
    val isLoadingComments by (mainViewModel?.isLoadingComments?.collectAsState() ?: remember { mutableStateOf(false) })
    val commentsError by (mainViewModel?.commentsError?.collectAsState() ?: remember { mutableStateOf<String?>(null) })
    val isPostingComment by (mainViewModel?.isPostingComment?.collectAsState() ?: remember { mutableStateOf(false) })
    val commentReactions by (mainViewModel?.commentReactions?.collectAsState() ?: remember { mutableStateOf(emptyMap<Long, List<xyz.siwane.shizucorefetch.network.GitHubReaction>>()) })
    val hiddenCommentIds by (mainViewModel?.hiddenCommentIds?.collectAsState() ?: remember { mutableStateOf(emptySet<Long>()) })
    val isAdmin = mainViewModel?.isCurrentUserAdmin == true

    val appRating by (mainViewModel?.currentAppRating?.collectAsState() ?: remember { mutableStateOf(xyz.siwane.shizucorefetch.data.AppRatingSummary()) })
    val myRating by (mainViewModel?.myRating?.collectAsState() ?: remember { mutableStateOf<Int?>(null) })
    val isSubmittingRating by (mainViewModel?.isSubmittingRating?.collectAsState() ?: remember { mutableStateOf(false) })
    val ratingError by (mainViewModel?.ratingError?.collectAsState() ?: remember { mutableStateOf<String?>(null) })

    LaunchedEffect(app.repoOwner, app.repoName) {
        if (app.repoOwner.isNotBlank() && app.repoName.isNotBlank()) {
            mainViewModel?.loadComments(app.repoOwner, app.repoName)
            mainViewModel?.observeAppRating(app.repoOwner, app.repoName)
        }
    }

    val bgColor = MaterialTheme.colorScheme.background

    BackHandler {
        if (showDeveloperProfile) {
            showDeveloperProfile = false
        } else {
            onBackClick()
        }
    }

    if (offlinePhase == AppDetailsOfflinePhase.OFFLINE) {
        // صفحة أوفلاين كاملة منفصلة (بنفس أسلوب StoreScreen بالضبط) — كتبدّل
        // محتوى تفاصيل التطبيق بالكامل ملي الشبكة منقطعة فعليًا، عوض ما يبقى
        // المستخدم واقف قدام سبيلر لا نهائي أو محتوى ناقص/كسور (خصوصًا
        // للتطبيقات المثبتة/المحتاجة تحديث/المحفوظة فالمكتبة بلا كاش سابق).
        AppDetailsOfflineFullState(
            appName = app.name,
            onBackClick = onBackClick,
            modifier = modifier.fillMaxSize().background(bgColor)
        )
        return
    }

    Crossfade(targetState = showDeveloperProfile, label = "developer_profile_transition") { isDevProfile ->
        if (isDevProfile) {
            DeveloperProfileScreen(
                app = app,
                developerInfo = appMetadata?.developerInfo,
                devMessage = appMetadata?.developerMessage,
                developerApps = developerApps,
                onBackClick = { showDeveloperProfile = false },
                onAppClick = { clickedApp -> 
                    showDeveloperProfile = false 
                    onAppClick(clickedApp) 
                },
                mainViewModel = mainViewModel,
                modifier = modifier
            )
        } else {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .nestedScroll(pullRefreshState.nestedScrollConnection)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        if (effectiveBanner != null) {
                            AsyncImage(
                                model = effectiveBanner,
                                contentDescription = "Banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                    )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                                        ),
                                        startY = 100f
                                    )
                                )
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.3f))
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IconButton(
                                    onClick = onBookmarkToggle,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.3f))
                                ) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Save to Library",
                                        tint = Color.White
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        // رابط repo الحقيقي ديال هاد التطبيق بالضبط (ماشي حساب المطوّر
                                        // ديال ShizuCoreFetch نفسه، وماشي تخمين مبني على اسم العرض)
                                        val repoUrl = app.htmlUrl.ifBlank { "https://github.com/${app.repoOwner}/${app.repoName}" }
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "Check out ${app.name} on ShizuCoreFetch Store!\n$repoUrl")
                                        }
                                        val chooser = Intent.createChooser(shareIntent, "Share Repository").apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(chooser)
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.3f))
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = app.iconUrl.ifBlank { null },
                            placeholder = painterResource(id = R.mipmap.ic_launcher_round),
                            error = painterResource(id = R.mipmap.ic_launcher_round),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = app.repoOwner.ifBlank { "—" },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { showDeveloperProfile = true }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }

                    if (actionError != null) {
                        Text(
                            text = actionError ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }

                    if (installUiState is InstallUiState.Downloading) {
                        val progress = (installUiState as InstallUiState.Downloading).progress
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isBusy = installUiState is InstallUiState.Downloading || installUiState is InstallUiState.Installing
                        when (installUiState) {
                            is InstallUiState.Open, is InstallUiState.Update -> {
                                val needsUpdate = installUiState is InstallUiState.Update
                                OutlinedButton(
                                    onClick = { performUninstall() },
                                    enabled = !isBusy,
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(text = stringResource(id = R.string.action_uninstall), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { if (needsUpdate) performInstall() else performOpen() },
                                    enabled = !isBusy,
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (needsUpdate) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(text = stringResource(id = if (needsUpdate) R.string.action_update else R.string.action_open), fontWeight = FontWeight.Bold)
                                }
                            }
                            else -> {
                                Button(
                                    onClick = { performInstall() },
                                    enabled = !isBusy,
                                    modifier = Modifier.weight(2f).height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    val label = when (installUiState) {
                                        is InstallUiState.Downloading -> "..."
                                        is InstallUiState.Installing -> "..."
                                        else -> stringResource(id = R.string.details_install)
                                    }
                                    Text(text = label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { 
                                val repoUrl = app.htmlUrl.ifBlank { "https://github.com/${app.repoOwner}/${app.repoName}" }
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_github_v), 
                                contentDescription = "GitHub",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppStatItem(value = app.stars, label = stringResource(id = R.string.stats_stars), icon = Icons.Default.Star, tint = Color(0xFFFFD54F))
                        AppStatDivider()
                        AppStatItem(value = effectiveDownloads, label = stringResource(id = R.string.stats_downloads), icon = Icons.Default.Download, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        AppStatDivider()
                        AppStatItem(value = app.size, label = stringResource(id = R.string.stats_size), icon = Icons.Default.SdStorage, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        AppStatDivider()
                        AppStatItem(value = effectiveCategory, label = stringResource(id = R.string.stats_category), icon = Icons.Default.Category, tint = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(modifier = Modifier.height(24.dp))

                    val topAd = effectiveAds.firstOrNull { it.position == "top" }
                    if (topAd != null && topAd.image_url.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (topAd.target_url.isNotBlank()) {
                                        adUrlToConfirm = topAd.target_url
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                AsyncImage(
                                    model = topAd.image_url,
                                    contentDescription = "Advertisement",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(stringResource(id = R.string.ad_label), style = MaterialTheme.typography.labelSmall, color = Color.White)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        if (app.appWebsite.isNotBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(app.appWebsite)).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = stringResource(id = R.string.action_website), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        Text(
                            text = stringResource(id = R.string.app_description_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (isMetadataLoading) {
                            Column {
                                Box(modifier = Modifier.fillMaxWidth(0.9f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth(1f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth(0.5f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                            }
                        } else {
                            Text(
                                text = formattedDesc,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 26.sp 
                            )
                        }

                        val appTags = appMetadata?.tags ?: emptyList()
                        if (appTags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = stringResource(id = R.string.details_tech_tags),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(appTags) { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = tag, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        val minSdk = appMetadata?.minSdk ?: 0
                        val targetSdk = appMetadata?.targetSdk ?: 0
                        val license = appMetadata?.license ?: ""
                        val openSource = appMetadata?.openSource ?: false
                        val reqShizuku = appMetadata?.requiresShizuku ?: false
                        
                        if (minSdk > 0 || targetSdk > 0 || license.isNotBlank() || openSource || reqShizuku) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (minSdk > 0) {
                                        AppTechInfoRow(icon = Icons.Default.Info, title = stringResource(id = R.string.details_tech_min_sdk), value = "Android $minSdk+")
                                    }
                                    if (targetSdk > 0) {
                                        AppTechInfoRow(icon = Icons.Default.Settings, title = stringResource(id = R.string.details_tech_target_sdk), value = "Android $targetSdk")
                                    }
                                    if (license.isNotBlank()) {
                                        AppTechInfoRow(icon = Icons.Default.Article, title = stringResource(id = R.string.details_tech_license), value = license)
                                    }
                                    if (openSource) {
                                        AppTechInfoRow(icon = Icons.Default.Code, title = stringResource(id = R.string.details_tech_source), value = stringResource(id = R.string.details_tech_open_source))
                                    }
                                    if (reqShizuku) {
                                        AppTechInfoRow(icon = Icons.Default.Security, title = stringResource(id = R.string.details_tech_privilege), value = stringResource(id = R.string.details_tech_requires_shizuku))
                                    }
                                }
                            }
                        }
                    }

                    if (isMetadataLoading) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = stringResource(id = R.string.app_screenshots),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.padding(horizontal = 24.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            repeat(3) {
                                Box(modifier = Modifier.height(320.dp).width(160.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect())
                            }
                        }
                    } else if (effectiveScreenshots.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = stringResource(id = R.string.app_screenshots),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(effectiveScreenshots) { index, screenshotUrl ->
                                AsyncImage(
                                    model = screenshotUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillHeight, 
                                    modifier = Modifier
                                        .height(320.dp)
                                        .wrapContentWidth()
                                        .widthIn(min = 120.dp, max = 300.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                        .clickable { fullscreenImageIndex = index }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Text(text = stringResource(id = R.string.app_rating_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(end = 24.dp)
                                    ) {
                                        Text(
                                            text = String.format("%.1f", appRating.average),
                                            style = MaterialTheme.typography.displayMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row {
                                            for (i in 1..5) {
                                                Icon(
                                                    imageVector = if (i <= appRating.average.toInt() || (i - appRating.average < 1 && i - appRating.average > 0)) Icons.Default.Star else Icons.Default.StarBorder,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFD54F),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${appRating.count} " + stringResource(id = R.string.app_rating_title),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Box(modifier = Modifier.weight(1f)) {
                                        RatingBarsSummary(average = appRating.average, totalCount = appRating.count)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(16.dp))

                                if (!isGithubLoggedIn) {
                                    Text(
                                        text = stringResource(id = R.string.rating_login_warning),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        for (i in 1..5) {
                                            val isSelected = i <= (myRating ?: 0)
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = "Star $i",
                                                tint = if (isSelected) Color(0xFFFFD54F) else MaterialTheme.colorScheme.outline,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clickable(enabled = !isSubmittingRating) {
                                                        mainViewModel?.submitRating(app.repoOwner, app.repoName, i)
                                                    }
                                                    .padding(4.dp)
                                            )
                                        }
                                    }
                                    if (ratingError != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = ratingError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = stringResource(id = R.string.comments_title), 
                                style = MaterialTheme.typography.titleLarge, 
                                fontWeight = FontWeight.Bold, 
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            if (isGithubLoggedIn) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (currentAvatarUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = currentAvatarUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                        )
                                    } else {
                                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    OutlinedTextField(
                                        value = commentText,
                                        onValueChange = { commentText = it },
                                        placeholder = { Text(stringResource(id = R.string.write_comment_hint)) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(24.dp),
                                        trailingIcon = {
                                            IconButton(
                                                enabled = commentText.isNotBlank() && !isPostingComment,
                                                onClick = {
                                                    mainViewModel?.postComment(app.repoOwner, app.repoName, app.name, commentText.trim())
                                                    commentText = ""
                                                }
                                            ) {
                                                Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(id = R.string.comments_login_warning),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (commentsError != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = commentsError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            if (isLoadingComments) {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                val visibleComments = comments.filter { isAdmin || !hiddenCommentIds.contains(it.id) }
                                if (visibleComments.isEmpty()) {
                                    Text(
                                        text = stringResource(id = R.string.comments_empty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    val displayComments = if (showAllComments) visibleComments else visibleComments.takeLast(3)
                                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                        displayComments.forEach { comment ->
                                            RealCommentItem(
                                                comment = comment,
                                                reactions = commentReactions[comment.id].orEmpty(),
                                                currentUsername = currentUsername,
                                                isAdmin = isAdmin,
                                                isHidden = hiddenCommentIds.contains(comment.id),
                                                onReplyClick = {
                                                    commentText = "@${comment.user?.login ?: ""} "
                                                },
                                                onReact = { content ->
                                                    mainViewModel?.toggleCommentReaction(app.repoOwner, app.repoName, comment.id, content)
                                                },
                                                onDelete = {
                                                    mainViewModel?.deleteComment(app.repoOwner, app.repoName, comment.id)
                                                },
                                                onEdit = { newBody ->
                                                    mainViewModel?.editComment(app.repoOwner, app.repoName, comment.id, newBody)
                                                },
                                                onAdminHide = { mainViewModel?.adminHideCommentFromStore(comment.id) },
                                                onAdminUnhide = { mainViewModel?.adminUnhideCommentFromStore(comment.id) }
                                            )
                                        }
                                        
                                        if (visibleComments.size > 3) {
                                            TextButton(
                                                onClick = { showAllComments = !showAllComments },
                                                modifier = Modifier.align(Alignment.CenterHorizontally)
                                            ) {
                                                Text(
                                                    text = if (showAllComments) stringResource(id = R.string.comments_hide) else stringResource(id = R.string.comments_show_more, visibleComments.size),
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    val bottomAd = effectiveAds.firstOrNull { it.position == "bottom" }
                    if (bottomAd != null && bottomAd.image_url.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (bottomAd.target_url.isNotBlank()) {
                                        adUrlToConfirm = bottomAd.target_url
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                                AsyncImage(
                                    model = bottomAd.image_url,
                                    contentDescription = "Advertisement",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.4f)).padding(horizontal = 6.dp, vertical = 2.dp)
                                ) { Text(stringResource(id = R.string.ad_label), style = MaterialTheme.typography.labelSmall, color = Color.White) }
                            }
                        }
                    }

                    val devMessage = appMetadata?.developerMessage
                    if (isMetadataLoading) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Box(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(120.dp).clip(RoundedCornerShape(20.dp)).shimmerEffect())
                    } else if (!devMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.FormatQuote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(id = R.string.dev_word_title),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = devMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }

                    val changelogText = appMetadata?.changelog
                    if (isMetadataLoading) {
                        // يتم التحكم فيها مسبقاً 
                    } else if (!changelogText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Article, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.details_changelog_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    text = formatAppDescription(changelogText, "md"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }

                    if (app.versionTag.isNotBlank() || app.previousReleases.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.version_history_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            val currentRelease = ReleaseInfo(versionTag = app.versionTag, apkUrl = app.apkUrl, apkSize = app.apkSizeBytes, releasedAt = app.releasedAt, releaseNotes = app.releaseNotes)
                            val allReleases = if (app.versionTag.isNotBlank()) listOf(currentRelease) + app.previousReleases else app.previousReleases

                            Column(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                allReleases.forEachIndexed { index, release ->
                                    ReleaseHistoryRow(
                                        release = release, 
                                        isCurrent = index == 0 && app.versionTag.isNotBlank(),
                                        context = context,
                                        app = app,
                                        onGoToLibrary = onNavigateToLibrary
                                    )
                                    if (index != allReleases.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    if (popularApps.isNotEmpty()) {
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Text(
                                text = stringResource(id = R.string.popular_apps_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    popularApps.chunked(3).forEach { rowApps ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            rowApps.forEach { popApp ->
                                                GridAppItem(
                                                    app = popApp,
                                                    mainViewModel = mainViewModel,
                                                    modifier = Modifier.weight(1f).clickable { onAppClick(popApp) }
                                                )
                                            }
                                            repeat(3 - rowApps.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
                
                PullToRefreshContainer(
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }

    if (adUrlToConfirm != null) {
        val titleText = stringResource(id = R.string.action_open)
        val descText = stringResource(id = R.string.ad_external_link_desc, adUrlToConfirm ?: "")
        val continueText = stringResource(id = R.string.action_continue)
        val cancelText = stringResource(id = R.string.action_cancel)

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { adUrlToConfirm = null },
            title = { 
                Text(text = titleText, fontWeight = FontWeight.Bold) 
            },
            text = { 
                Text(text = descText) 
            },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(adUrlToConfirm)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    adUrlToConfirm = null
                }) {
                    Text(text = continueText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { adUrlToConfirm = null }) {
                    Text(text = cancelText)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (fullscreenImageIndex != null) {
        val pagerState = rememberPagerState(
            initialPage = fullscreenImageIndex!!,
            pageCount = { effectiveScreenshots.size }
        )

        Dialog(
            onDismissRequest = { fullscreenImageIndex = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = effectiveScreenshots[page],
                            contentDescription = "Fullscreen Screenshot",
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .fillMaxHeight(0.9f)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // زر الإغلاق
                IconButton(
                    onClick = { fullscreenImageIndex = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 18.dp, end = 18.dp)
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                // زر اليسار
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous",
                            tint = Color.White
                        )
                    }
                }

                // زر اليمين
                if (pagerState.currentPage < effectiveScreenshots.size - 1) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next",
                            tint = Color.White
                        )
                    }
                }
                
                // عداد الصور
                Text(
                    text = "${pagerState.currentPage + 1} / ${effectiveScreenshots.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun DeveloperProfileScreen(
    app: DummyApp,
    developerInfo: StoreDeveloperInfo?,
    devMessage: String?,
    developerApps: List<DummyApp>,
    onBackClick: () -> Unit,
    onAppClick: (DummyApp) -> Unit,
    mainViewModel: MainViewModel?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(280.dp)
        ) {
            val bannerUrl = developerInfo?.banner_url?.takeIf { it.isNotBlank() } ?: app.bannerImageUrl
            if (bannerUrl.isNotBlank()) {
                AsyncImage(
                    model = bannerUrl,
                    contentDescription = "Developer Banner",
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_round),
                    contentDescription = "Developer Banner",
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(id = R.string.dev_profile_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            AsyncImage(
                model = "https://github.com/${app.repoOwner}.png",
                contentDescription = "Developer Profile Picture",
                placeholder = painterResource(id = R.mipmap.ic_launcher_round),
                error = painterResource(id = R.mipmap.ic_launcher_round),
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.BottomCenter)
                    .clip(CircleShape)
                    .border(4.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = developerInfo?.name?.takeIf { it.isNotBlank() } ?: app.repoOwner.ifBlank { "Unknown" }, 
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.dev_role_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.dev_bio_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                val bioText = devMessage?.takeIf { it.isNotBlank() } ?: stringResource(id = R.string.dev_bio_desc)
                Text(
                    text = bioText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val hasContactInfo = developerInfo != null && (developerInfo.email.isNotBlank() || developerInfo.website.isNotBlank() || developerInfo.portfolio.isNotBlank() || developerInfo.donate_url.isNotBlank())
        if (hasContactInfo) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.dev_info_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (developerInfo?.email?.isNotBlank() == true) {
                        DevInfoRow(
                            icon = Icons.Default.Email, 
                            label = stringResource(id = R.string.dev_email_label), 
                            value = developerInfo.email,
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:${developerInfo.email}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                    if (developerInfo?.website?.isNotBlank() == true) {
                        DevInfoRow(
                            icon = Icons.Default.Language, 
                            label = stringResource(id = R.string.dev_website_label), 
                            value = developerInfo.website,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(developerInfo.website)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                    if (developerInfo?.portfolio?.isNotBlank() == true) {
                        DevInfoRow(
                            icon = Icons.Default.Work, 
                            label = stringResource(id = R.string.dev_portfolio_label), 
                            value = developerInfo.portfolio,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(developerInfo.portfolio)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                    if (developerInfo?.donate_url?.isNotBlank() == true) {
                        DevInfoRow(
                            icon = Icons.Default.Favorite, 
                            label = stringResource(id = R.string.dev_donate), 
                            value = developerInfo.donate_url,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(developerInfo.donate_url)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        Text(
            text = stringResource(id = R.string.dev_social_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val socials = developerInfo?.socials ?: emptyMap()
            if (socials.isEmpty()) {
                SocialIcon(iconResId = R.drawable.ic_github) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/${app.repoOwner}")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    context.startActivity(intent)
                }
            } else {
                socials.forEach { (platform, url) ->
                    val iconRes = when (platform.lowercase()) {
                        "facebook" -> R.drawable.ic_facebook
                        "instagram" -> R.drawable.ic_instagram
                        "x", "twitter" -> R.drawable.ic_x
                        "youtube" -> R.drawable.ic_youtube
                        "github" -> R.drawable.ic_github
                        "telegram" -> R.drawable.ic_telegram
                        else -> null
                    }
                    if (iconRes != null && url.isNotBlank()) {
                        SocialIcon(iconResId = iconRes) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (developerApps.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    Text(
                        text = stringResource(id = R.string.dev_apps_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        developerApps.take(6).chunked(3).forEach { rowApps ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                rowApps.forEach { devApp ->
                                    GridAppItem(
                                        app = devApp,
                                        mainViewModel = mainViewModel, 
                                        modifier = Modifier.weight(1f).clickable { onAppClick(devApp) }
                                    )
                                }
                                repeat(3 - rowApps.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun DevInfoRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun SocialIcon(
    iconResId: Int, 
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(
                width = 1.dp, 
                color = if (isDark) Color(0xFFE0E0E0) else Color(0xFF424242), 
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop 
        )
    }
}

@Composable
fun AppStatItem(value: String, label: String, icon: ImageVector, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AppStatDivider() {
    Box(
        modifier = Modifier
            .height(24.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    )
}

fun formatDownloadCount(count: Int): String {
    return when {
        count <= 0 -> "-"
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

@Composable
fun ReleaseHistoryRow(release: ReleaseInfo, isCurrent: Boolean, context: Context, app: DummyApp, onGoToLibrary: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }
    var isDownloaded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val sizeDisplay = when {
        release.apkSize <= 0 -> ""
        release.apkSize >= 1024L * 1024L -> String.format("%.1f MB", release.apkSize / (1024.0 * 1024.0))
        else -> String.format("%.0f KB", release.apkSize / 1024.0)
    }
    val dateDisplay = release.releasedAt.takeIf { it.isNotBlank() }?.split("T")?.firstOrNull() ?: ""

    val toastDownloadCompleteTemplate = stringResource(id = R.string.toast_download_complete)
    val toastDownloadFailedTemplate = stringResource(id = R.string.toast_download_failed)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = release.releaseNotes.isNotBlank()) { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = release.versionTag.ifBlank { "—" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isCurrent) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.version_current_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dateDisplay.isNotBlank()) {
                    Text(text = dateDisplay, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (sizeDisplay.isNotBlank()) {
                    Text(text = "  •  $sizeDisplay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(12.dp))
                
                if (isDownloaded) {
                    TextButton(onClick = onGoToLibrary) {
                        Text(stringResource(id = R.string.action_go_to_library), fontWeight = FontWeight.Bold)
                    }
                } else if (downloadProgress == null) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                val notificationId = app.appId.hashCode() + release.versionTag.hashCode()
                                coroutineScope.launch {
                                    try {
                                        downloadProgress = 0f
                                        isDownloaded = false
                                        val fileName = "${app.repoName}-${release.versionTag}.apk"
                                        ApkFileManager.downloadApk(context, release.apkUrl, fileName) { p ->
                                            downloadProgress = p
                                            xyz.siwane.shizucorefetch.notifications.NotificationHelper.showDownloadProgress(
                                                context, notificationId, app.name, (p * 100).toInt()
                                            )
                                        }
                                        Toast.makeText(context, String.format(toastDownloadCompleteTemplate, fileName), Toast.LENGTH_SHORT).show()
                                        isDownloaded = true
                                        xyz.siwane.shizucorefetch.notifications.NotificationHelper.showStatusNotification(
                                            context, notificationId, app.name, String.format(toastDownloadCompleteTemplate, fileName), true
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(context, String.format(toastDownloadFailedTemplate, e.message ?: ""), Toast.LENGTH_SHORT).show()
                                        xyz.siwane.shizucorefetch.notifications.NotificationHelper.showStatusNotification(
                                            context, notificationId, app.name, String.format(toastDownloadFailedTemplate, e.message ?: ""), false
                                        )
                                    } finally {
                                        downloadProgress = null
                                    }
                                }
                            }
                    )
                }
            }
        }
        
        if (downloadProgress != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { downloadProgress ?: 0f },
                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${((downloadProgress ?: 0f) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (expanded && release.releaseNotes.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatAppDescription(release.releaseNotes, "md"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RealCommentItem(
    comment: xyz.siwane.shizucorefetch.network.GitHubComment,
    reactions: List<xyz.siwane.shizucorefetch.network.GitHubReaction>,
    currentUsername: String,
    isAdmin: Boolean,
    isHidden: Boolean,
    onReplyClick: () -> Unit,
    onReact: (String) -> Unit,
    onDelete: () -> Unit,
    onEdit: (String) -> Unit,
    onAdminHide: () -> Unit,
    onAdminUnhide: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showReactionPicker by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(comment.body) { mutableStateOf(comment.body) }
    val isOwnComment = comment.user?.login == currentUsername && currentUsername.isNotEmpty()

    val availableReactions = listOf("laugh" to "😄", "+1" to "👍", "-1" to "👎", "hooray" to "🎉", "confused" to "😕", "heart" to "❤️", "rocket" to "🚀", "eyes" to "👀")
    val groupedReactions = reactions.groupingBy { it.content }.eachCount()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            val avatarUrl = comment.user?.avatar_url
            if (!avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
            } else {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Text((comment.user?.login ?: "?").take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(comment.user?.login ?: "unknown", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    if (isHidden) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.comment_hidden_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (isEditing) {
                    androidx.compose.material3.OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = stringResource(id = R.string.comment_save),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                if (editText.isNotBlank() && editText != comment.body) onEdit(editText)
                                isEditing = false
                            }.padding(8.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.comment_cancel),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable {
                                editText = comment.body
                                isEditing = false
                            }.padding(8.dp)
                        )
                    }
                } else {
                    Text(comment.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.EmojiEmotions,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showReactionPicker = true }
                                .padding(8.dp)
                                .size(18.dp)
                        )
                        androidx.compose.material3.DropdownMenu(expanded = showReactionPicker, onDismissRequest = { showReactionPicker = false }) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                                availableReactions.forEach { (content, emoji) ->
                                    Text(
                                        text = emoji,
                                        modifier = Modifier
                                            .clickable {
                                                onReact(content)
                                                showReactionPicker = false
                                            }
                                            .padding(6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onReplyClick() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(id = R.string.comment_reply),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isOwnComment && !isEditing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isEditing = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(id = R.string.comment_edit),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (isOwnComment && !isEditing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onDelete() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(id = R.string.comment_delete),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (isAdmin && !isOwnComment) {
                        Text(
                            text = stringResource(id = if (isHidden) R.string.comment_admin_unhide else R.string.comment_admin_hide),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { if (isHidden) onAdminUnhide() else onAdminHide() }
                                .padding(8.dp)
                        )
                    }
                }

                if (groupedReactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        groupedReactions.forEach { (content, count) ->
                            val emoji = availableReactions.firstOrNull { it.first == content }?.second ?: content
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { onReact(content) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(emoji, style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingBarsSummary(average: Double, totalCount: Int) {
    val ratios = when {
        average >= 4.5 -> listOf(0.8f, 0.15f, 0.03f, 0.01f, 0.01f)
        average >= 4.0 -> listOf(0.6f, 0.25f, 0.08f, 0.05f, 0.02f)
        average >= 3.0 -> listOf(0.3f, 0.4f, 0.2f, 0.05f, 0.05f)
        else -> listOf(0.2f, 0.2f, 0.2f, 0.2f, 0.2f)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        (5 downTo 1).forEachIndexed { index, starCount ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Text(
                    text = "$starCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { ratios[index] },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Color(0xFFFFD54F),
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                )
            }
        }
    }
}

@Composable
fun AppTechInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
