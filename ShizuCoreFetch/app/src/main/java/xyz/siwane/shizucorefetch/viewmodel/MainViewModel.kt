package xyz.siwane.shizucorefetch.viewmodel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
import xyz.siwane.shizucorefetch.apkmanager.ApkFileManager
import xyz.siwane.shizucorefetch.apkmanager.DownloadInstallManager
import xyz.siwane.shizucorefetch.auth.GitHubAuth
import xyz.siwane.shizucorefetch.data.AppFullMetadata
import xyz.siwane.shizucorefetch.data.InstallStateResolver
import xyz.siwane.shizucorefetch.data.InstallUiState
import xyz.siwane.shizucorefetch.data.InstalledAppInfo
import xyz.siwane.shizucorefetch.data.InstalledAppsScanner
import xyz.siwane.shizucorefetch.data.NetworkStatusMonitor
import xyz.siwane.shizucorefetch.data.RichMetadataCache
import xyz.siwane.shizucorefetch.data.AppMetadataFetcher
import xyz.siwane.shizucorefetch.data.AppRatingSummary
import xyz.siwane.shizucorefetch.data.RatingsRepository
import xyz.siwane.shizucorefetch.data.StoreCacheManager
import xyz.siwane.shizucorefetch.data.TokenStore
import xyz.siwane.shizucorefetch.network.CreateCommentRequest
import xyz.siwane.shizucorefetch.network.CreateIssueRequest
import xyz.siwane.shizucorefetch.network.CreateReactionRequest
import xyz.siwane.shizucorefetch.network.GitHubComment
import xyz.siwane.shizucorefetch.network.GitHubReaction
import xyz.siwane.shizucorefetch.network.GitHubRepo
import xyz.siwane.shizucorefetch.network.NetworkModule
import xyz.siwane.shizucorefetch.network.bearer
import xyz.siwane.shizucorefetch.ui.screens.DummyApp
import xyz.siwane.shizucorefetch.ui.screens.toDummyApp
import xyz.siwane.shizucorefetch.R

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("ShizuSettings", Context.MODE_PRIVATE)
    private val tokenStore = TokenStore(application)
    private val api = NetworkModule.githubApi

    // متغيرات الاختصارات (Shortcuts) الجديدة
    private val _shortcutAction = MutableStateFlow<String?>(null)
    val shortcutAction: StateFlow<String?> = _shortcutAction.asStateFlow()

    fun setShortcutAction(action: String?) {
        _shortcutAction.value = action
    }

    fun consumeShortcutAction() {
        _shortcutAction.value = null
    }

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "System") ?: "System")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _themeColorIndex = MutableStateFlow(prefs.getInt("theme_color", 2))
    val themeColorIndex: StateFlow<Int> = _themeColorIndex.asStateFlow()

    private val _isDynamicColor = MutableStateFlow(prefs.getBoolean("dynamic_color", false))
    val isDynamicColor: StateFlow<Boolean> = _isDynamicColor.asStateFlow()

    private val _useCustomColor = MutableStateFlow(prefs.getBoolean("use_custom_color", false))
    val useCustomColor: StateFlow<Boolean> = _useCustomColor.asStateFlow()

    private val _customHexColor = MutableStateFlow(prefs.getString("custom_hex_color", "#4A90E2") ?: "#4A90E2")
    val customHexColor: StateFlow<String> = _customHexColor.asStateFlow()

    private val _uiScale = MutableStateFlow(prefs.getFloat("ui_scale", 0.90f))
    val uiScale: StateFlow<Float> = _uiScale.asStateFlow()

    // إلا ماكانش عندنا تفضيل محفوظ (أول تشغيل للتطبيق)، كنجربو لغة النظام؛ إلا
    // ماكانتش مدعومة فالتطبيق، الإنجليزية هي اللغة الافتراضية (أول لغة فاللائحة).
    // كنسجلوها مباشرة فـ prefs باش تبقى ثابتة فالتشغيلات الجاية حتى لو تبدلات
    // لغة النظام من بعد، وباش شاشة الإعدادات تعرض الاختيار الصحيح فورًا.
    private fun resolveInitialLanguage(): String {
        val saved = prefs.getString("language", null)
        if (saved != null) return saved
        val systemLang = java.util.Locale.getDefault().language
        val resolved = if (xyz.siwane.shizucorefetch.ui.screens.supportedLanguages.any { it.first == systemLang }) systemLang else "en"
        prefs.edit().putString("language", resolved).apply()
        return resolved
    }

    private val _languageCode = MutableStateFlow(resolveInitialLanguage())
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    private val _isGithubLoggedIn = MutableStateFlow(tokenStore.hasToken())
    val isGithubLoggedIn: StateFlow<Boolean> = _isGithubLoggedIn.asStateFlow()

    private val _isTokenActive = MutableStateFlow(tokenStore.hasToken())
    val isTokenActive: StateFlow<Boolean> = _isTokenActive.asStateFlow()

    private val _githubUsername = MutableStateFlow(prefs.getString("github_username", "") ?: "")
    val githubUsername: StateFlow<String> = _githubUsername.asStateFlow()

    private val _githubAvatarUrl = MutableStateFlow(prefs.getString("github_avatar_url", "") ?: "")
    val githubAvatarUrl: StateFlow<String> = _githubAvatarUrl.asStateFlow()

    private val _githubName = MutableStateFlow(prefs.getString("github_name", "") ?: "")
    val githubName: StateFlow<String> = _githubName.asStateFlow()

    private val _githubEmail = MutableStateFlow(prefs.getString("github_email", "") ?: "")
    val githubEmail: StateFlow<String> = _githubEmail.asStateFlow()

    private val _githubBio = MutableStateFlow(prefs.getString("github_bio", "") ?: "")
    val githubBio: StateFlow<String> = _githubBio.asStateFlow()

    private val _githubProfileUrl = MutableStateFlow(prefs.getString("github_profile_url", "") ?: "")
    val githubProfileUrl: StateFlow<String> = _githubProfileUrl.asStateFlow()

    private val _userRepos = MutableStateFlow<List<GitHubRepo>>(emptyList())
    val userRepos: StateFlow<List<GitHubRepo>> = _userRepos.asStateFlow()

    private val _githubFollowers = MutableStateFlow(prefs.getInt("github_followers", 0))
    val githubFollowers: StateFlow<Int> = _githubFollowers.asStateFlow()

    private val _githubFollowing = MutableStateFlow(prefs.getInt("github_following", 0))
    val githubFollowing: StateFlow<Int> = _githubFollowing.asStateFlow()

    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private var pendingState: String? = null

    data class RateLimitInfo(val limit: Int, val remaining: Int, val used: Int, val resetEpochSeconds: Long)

    private val _rateLimitInfo = MutableStateFlow<RateLimitInfo?>(null)
    val rateLimitInfo: StateFlow<RateLimitInfo?> = _rateLimitInfo.asStateFlow()

    private val _isLoadingRateLimit = MutableStateFlow(false)
    val isLoadingRateLimit: StateFlow<Boolean> = _isLoadingRateLimit.asStateFlow()

    fun refreshRateLimit() {
        _isLoadingRateLimit.value = true
        viewModelScope.launch {
            try {
                val token = tokenStore.getToken()
                val headers = if (!token.isNullOrBlank()) mapOf("Authorization" to bearer(token)) else emptyMap()
                val resp = withContext(Dispatchers.IO) { api.getRateLimit(headers) }
                if (resp.isSuccessful && resp.body() != null) {
                    val core = resp.body()!!.resources.core
                    _rateLimitInfo.value = RateLimitInfo(
                        limit = core.limit,
                        remaining = core.remaining,
                        used = core.used,
                        resetEpochSeconds = core.reset
                    )
                }
            } catch (_: Exception) {
            } finally {
                _isLoadingRateLimit.value = false
            }
        }
    }

    private val _workerEndpoint = MutableStateFlow(prefs.getString("worker_endpoint", "https://corefetch.siwane.xyz") ?: "https://corefetch.siwane.xyz")
    val workerEndpoint: StateFlow<String> = _workerEndpoint.asStateFlow()

    sealed class WorkerStatus {
        data object Unknown : WorkerStatus()
        data object Checking : WorkerStatus()
        data class Online(val latencyMs: Long, val httpCode: Int) : WorkerStatus()
        data class Offline(val reason: String) : WorkerStatus()
    }

    private val _workerStatus = MutableStateFlow<WorkerStatus>(WorkerStatus.Unknown)
    val workerStatus: StateFlow<WorkerStatus> = _workerStatus.asStateFlow()

    fun updateWorkerEndpoint(url: String) {
        _workerEndpoint.value = url
        prefs.edit().putString("worker_endpoint", url).apply()
    }

    fun testWorkerConnection() {
        val url = _workerEndpoint.value
        if (url.isBlank()) {
            _workerStatus.value = WorkerStatus.Offline("لم يتم ضبط عنوان الـ Worker بعد.")
            return
        }
        _workerStatus.value = WorkerStatus.Checking
        viewModelScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                
                // تعديل الرابط برمجياً للفحص فقط دون تغيير الرابط الظاهر في الواجهة
                val testUrl = if (url.endsWith("/")) "${url}?action=list" else "$url/?action=list"
                
                val request = okhttp3.Request.Builder().url(testUrl).get().build()
                val code = withContext(Dispatchers.IO) {
                    xyz.siwane.shizucorefetch.network.NetworkModule.plainOkHttpClient.newCall(request).execute().use { response ->
                        response.code
                    }
                }
                val latency = System.currentTimeMillis() - startTime
                _workerStatus.value = if (code in 200..399) {
                    WorkerStatus.Online(latencyMs = latency, httpCode = code)
                } else {
                    WorkerStatus.Offline("استجابة غير متوقعة (HTTP $code).")
                }
            } catch (e: Exception) {
                val errorDetails = if (!e.message.isNullOrBlank()) e.message else e.javaClass.simpleName
                val errorMsg = getApplication<Application>().getString(R.string.worker_error_prefix, errorDetails)
                _workerStatus.value = WorkerStatus.Offline(errorMsg)
            }
        }
    }

    fun checkForUpdates(onResult: (String, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    api.getLatestRelease("elhizazi1", "ShizuCoreFetch")
                }
                if (response.isSuccessful && response.body() != null) {
                    val latestTag = response.body()!!.tag_name
                    val url = "https://github.com/elhizazi1/ShizuCoreFetch/releases/tag/$latestTag"
                    val msgSuccess = getApplication<Application>().getString(R.string.update_available, latestTag)
                    onResult(msgSuccess, url)
                } else {
                    val msgHttpError = getApplication<Application>().getString(R.string.update_error_http, response.code())
                    onResult(msgHttpError, null)
                }
            } catch (e: Exception) {
                val msgConnError = getApplication<Application>().getString(R.string.update_error_connection)
                onResult(msgConnError, null)
            }
        }
    }

    val isShizukuRunning: StateFlow<Boolean> = xyz.siwane.shizucorefetch.shizuku.ShizukuManager.isRunning
    val isShizukuGranted: StateFlow<Boolean> = xyz.siwane.shizucorefetch.shizuku.ShizukuManager.isGranted
    val shizukuBinderVersion: StateFlow<Int> = xyz.siwane.shizucorefetch.shizuku.ShizukuManager.binderVersion

    private val _isSilentInstallEnabled = MutableStateFlow(prefs.getBoolean("silent_install_enabled", true))
    val isSilentInstallEnabled: StateFlow<Boolean> = _isSilentInstallEnabled.asStateFlow()

    fun refreshShizukuState() {
        xyz.siwane.shizucorefetch.shizuku.ShizukuManager.refreshState()
    }

    fun requestShizukuPermission() {
        xyz.siwane.shizucorefetch.shizuku.ShizukuManager.requestPermission()
    }

    fun updateSilentInstallEnabled(enabled: Boolean) {
        _isSilentInstallEnabled.value = enabled
        prefs.edit().putBoolean("silent_install_enabled", enabled).apply()
    }

    private val _bookmarkedApps = MutableStateFlow<Set<String>>(prefs.getStringSet("bookmarked_apps", emptySet()) ?: emptySet())
    val bookmarkedApps: StateFlow<Set<String>> = _bookmarkedApps.asStateFlow()

    private val metadataFetcher = AppMetadataFetcher()
    private val storeCacheManager = StoreCacheManager(application)

    private val _allStoreApps = MutableStateFlow<List<DummyApp>>(emptyList())
    val allStoreApps: StateFlow<List<DummyApp>> = _allStoreApps.asStateFlow()

    private val _isLoadingStore = MutableStateFlow(true)
    val isLoadingStore: StateFlow<Boolean> = _isLoadingStore.asStateFlow()

    private val _storeLoadError = MutableStateFlow<String?>(null)
    val storeLoadError: StateFlow<String?> = _storeLoadError.asStateFlow()

    private val _storeSearchQuery = MutableStateFlow("")
    val storeSearchQuery: StateFlow<String> = _storeSearchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null) 
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _availableCategories = MutableStateFlow<List<String>>(emptyList())
    val availableCategories: StateFlow<List<String>> = _availableCategories.asStateFlow()

    private val _filteredStoreApps = MutableStateFlow<List<DummyApp>>(emptyList())
    val filteredStoreApps: StateFlow<List<DummyApp>> = _filteredStoreApps.asStateFlow()

    private val _featuredStoreApps = MutableStateFlow<List<DummyApp>>(emptyList())
    val featuredStoreApps: StateFlow<List<DummyApp>> = _featuredStoreApps.asStateFlow()

    private var isFirstStoreLoad = true
    private var isFetchingStore = false

    fun loadStoreApps(force: Boolean = false) {
        if (isFetchingStore) return
        if (!force && _allStoreApps.value.isNotEmpty()) {
            applyStoreFilters()
            _isLoadingStore.value = false
            return
        }
        
        isFetchingStore = true
        _isLoadingStore.value = true
        _storeLoadError.value = null
        
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                val isArabic = _languageCode.value == "ar"
                val result = storeCacheManager.getStoreApps(forceRefresh = force)
                result.onSuccess { dtoList ->
                    val mapped = dtoList.map { it.toDummyApp(languageIsArabic = isArabic) }
                    _allStoreApps.value = mapped
                    _featuredStoreApps.value = mapped
                        .filter { it.featuredTier != null }
                        .sortedWith(compareBy({ it.featuredTier }, { -it.starsCount }))
                    _availableCategories.value = mapped
                        .map { it.category.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sortedWith(compareBy({ it != "Utility" && it != "أدوات" }, { it }))
                    applyStoreFilters()
                }.onFailure { e ->
                    _storeLoadError.value = e.message ?: "تعذر الاتصال بالمتجر. تحقق من الإنترنت."
                }
            } catch (e: Exception) {
                _storeLoadError.value = e.message ?: "تعذر الاتصال بالمتجر. تحقق من الإنترنت."
            } finally {
                if (isFirstStoreLoad) {
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed < 3000L) {
                        kotlinx.coroutines.delay(3000L - elapsed)
                    }
                    isFirstStoreLoad = false
                }
                _isLoadingStore.value = false
                isFetchingStore = false
            }
        }
    }

    fun updateStoreSearchQuery(query: String) {
        _storeSearchQuery.value = query
        applyStoreFilters()
    }

    fun selectStoreCategory(category: String?) {
        _selectedCategory.value = category
        applyStoreFilters()
    }

    private fun applyStoreFilters() {
        var list = _allStoreApps.value
        val query = _storeSearchQuery.value.trim()
        if (query.isNotEmpty()) {
            list = list.filter {
                it.name.contains(query, ignoreCase = true) || it.desc.contains(query, ignoreCase = true)
            }
        }
        val category = _selectedCategory.value
        if (!category.isNullOrBlank()) {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }
        _filteredStoreApps.value = list
    }

    // ============================================================
    // حالة التثبيت الموحّدة (مصدر حقيقة وحيد لكل الشاشات)
    // ============================================================
    // فحص إجباري وكامل للتطبيقات المثبتة فعليًا على الجهاز (الاسم + اسم الحزمة
    // + الإصدار)، مباشرة من PackageManager. installStates كتدمج هاد الفحص مع
    // قائمة المتجر ومع العمليات الجارية حاليًا (DownloadInstallManager.operations)
    // لتعطي حالة واحدة نهائية لكل تطبيق، تقرا منها StoreScreen وAppDetailsScreen
    // وLibraryScreen بلا أي نسخة محلية مكررة.
    private val _installedByPackage = MutableStateFlow<Map<String, InstalledAppInfo>>(emptyMap())
    private val _installedByName = MutableStateFlow<Map<String, InstalledAppInfo>>(emptyMap())

    /** يفرض إعادة فحص كل التطبيقات المثبتة على الجهاز الآن. يُستدعى عند بدء التطبيق وعند العودة له (onResume). */
    fun refreshInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            // نبدأو بآخر نتيجة محفوظة من تشغيلة سابقة (قراءة SharedPreferences
            // سريعة، بلا مسح PackageManager الثقيل) باش زر "فتح" يبان مباشرة عند
            // بدء التطبيق، بلا تأخير محسوس، بلا ما ننتظرو الفحص الكامل يكمل.
            // تقريبية فقط: كتتصحح تلقائيًا بالفحص الحقيقي تحت مباشرة.
            if (_installedByPackage.value.isEmpty()) {
                InstalledAppsScanner.loadCachedSnapshot(getApplication<Application>())
                val cachedByPkg = InstalledAppsScanner.currentByPackage()
                if (cachedByPkg.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        _installedByPackage.value = cachedByPkg
                        _installedByName.value = InstalledAppsScanner.currentByNormalizedName()
                    }
                }
            }

            // الفحص الحقيقي والكامل — بيصحح أي فرق (تطبيق تزاد، تحيّد، تحدّث) منذ
            // آخر تشغيلة، وبيسجّل نسخة جديدة فالكاش لقراءة أسرع فالمرة الجاية.
            InstalledAppsScanner.scan(getApplication<Application>())
            val byPkg = InstalledAppsScanner.currentByPackage()
            val byName = InstalledAppsScanner.currentByNormalizedName()
            withContext(Dispatchers.Main) {
                _installedByPackage.value = byPkg
                _installedByName.value = byName
            }
        }
    }

    // "الحالة المستقرة": الحساب الثقيل الحقيقي (مطابقة بالحزمة/الاسم + قراءة
    // SharedPreferences لـ121 تطبيق) — كيعاود الحساب غير لما تتبدل قائمة المتجر
    // أو نتيجة فحص الجهاز، ماشي لما تتبدل عمليات التحميل الجارية. قبل هاد الفصل،
    // installStates كانت كتعاود الحساب الثقيل هادا مع كل نبضة تقدم تحميل (progress
    // tick) — يعني عشرات المرات فالثانية أثناء أي تحميل جاري — بلا داعي، لأن أي
    // تطبيق غير مطابَق ماكيقدرش يوصل لحالة "تحديث" أصلاً.
    private val restingInstallStates: StateFlow<Map<String, InstallUiState>> = combine(
        _allStoreApps, _installedByPackage, _installedByName
    ) { apps, byPkg, byName ->
        val result = HashMap<String, InstallUiState>(apps.size)
        for (app in apps) {
            result[app.appId] = InstallStateResolver.resolve(
                appName = app.name,
                declaredPackageName = app.packageName,
                rememberedPackageName = ApkFileManager.getKnownPackageName(getApplication<Application>(), app.appId),
                versionTag = app.versionTag,
                installedByPackage = byPkg,
                installedByName = byName
            ).state
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // "الحالة الظاهرة": دمج خفيف وسريع فوق الحالة المستقرة — العمليات الجارية
    // (تحميل/تثبيت) كتعلو فوق النتيجة المحسوبة بلا إعادة حساب المطابقة لكل
    // الـ121 تطبيق فكل مرة. الحالة الشائعة (ماكاينش تحميل جاري) كترجع resting
    // مباشرة بلا أي نسخ إضافي.
    val installStates: StateFlow<Map<String, InstallUiState>> = combine(
        restingInstallStates, DownloadInstallManager.operations
    ) { resting, activeOps ->
        if (activeOps.isEmpty()) {
            resting
        } else {
            val merged = resting.toMutableMap()
            for ((appId, state) in activeOps) merged[appId] = state
            merged
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** اسم الحزمة الحقيقي المطابَق لتطبيق معين (بالحزمة أو بالاسم)، أو null إذا ماكانش مثبت. */
    fun resolvedPackageNameFor(app: DummyApp): String? {
        return InstallStateResolver.resolve(
            appName = app.name,
            declaredPackageName = app.packageName,
            rememberedPackageName = ApkFileManager.getKnownPackageName(getApplication<Application>(), app.appId),
            versionTag = app.versionTag,
            installedByPackage = _installedByPackage.value,
            installedByName = _installedByName.value
        ).matchedPackageName
    }

    /** يبدأ تثبيت/تحديث تطبيق. العملية كتخدم فـ DownloadInstallManager (مستقلة عن أي شاشة)، فماكتوقفش عند التنقل أو مغادرة التطبيق. */
    fun installOrUpdateApp(app: DummyApp) {
        DownloadInstallManager.startInstall(
            context = getApplication<Application>(),
            appId = app.appId,
            appName = app.name,
            repoName = app.repoName,
            apkUrl = app.apkUrl,
            versionTag = app.versionTag,
            isShizukuGranted = isShizukuGranted.value,
            isSilentInstallEnabled = isSilentInstallEnabled.value
        )
    }

    /** يفتح تطبيق مثبت. يرجع false إذا ما قدرش يحدد اسم الحزمة أو ما لقاش نية فتح صالحة. */
    fun openInstalledApp(app: DummyApp): Boolean {
        val pkg = resolvedPackageNameFor(app) ?: return false
        val intent = ApkFileManager.buildLaunchIntent(getApplication<Application>(), pkg) ?: return false
        getApplication<Application>().startActivity(intent)
        return true
    }

    /** يحذف تطبيق مثبت. العملية كتخدم فـ DownloadInstallManager بنفس منطق startInstall. */
    fun uninstallApp(app: DummyApp) {
        val pkg = resolvedPackageNameFor(app) ?: return
        DownloadInstallManager.startUninstall(getApplication<Application>(), app.appId, pkg, isShizukuGranted.value)
    }

    // ============================================================
    // حالة الاتصال بالإنترنت الحقيقية (لشارة Online/Offline)
    // ============================================================
    // كانت شارة "Online" فالشاشة الرئيسية نص ثابت (Static) بلا أي فحص حقيقي —
    // دايمًا خضراء حتى لو الجهاز فوضع الطيران. دابا كتراقب ConnectivityManager
    // فعليًا وكتبدّل فوريًا مع أي تغيير حقيقي فحالة الشبكة.
    //
    // SharingStarted.Eagerly (بدل WhileSubscribed(5000)): المراقبة كتبدا مع
    // إنشاء الـ ViewModel وكتبقى شغالة طول عمر التطبيق كاملة، بلا ما تتوقف
    // وتعاود تبدا حسب وجود/غياب مستمعين (Composable). قبل هادشي، فأي لحظة ما
    // كانش فيها مستمع نشيط لـ 5 ثواني، كان NetworkCallback كيتلغى تسجيله
    // بالكامل (awaitClose)، فالتطبيق كان "يفوت" أحداث فقدان الشبكة الحقيقية
    // للحظتها، وما كان كيصحح الحالة غير ملي تعاود تسجيل مستمع جديد (كخروج
    // ودخول التطبيق) — دابا التسجيل دائم وفوري.
    val isOnline: StateFlow<Boolean> = NetworkStatusMonitor.observe(getApplication<Application>())
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val richCache = RichMetadataCache(application)

    fun fetchAppMetadata(app: DummyApp, onFailure: (() -> Unit)? = null, onResult: (AppFullMetadata) -> Unit) {
        if (app.repoOwner.isBlank() || app.repoName.isBlank() || app.appId.isBlank()) {
            onFailure?.invoke()
            return
        }

        val lang = _languageCode.value
        val cacheKey = "${app.appId}_$lang"

        viewModelScope.launch(Dispatchers.IO) {
            val cached = richCache.get(cacheKey)
            if (cached != null) {
                withContext(Dispatchers.Main) { onResult(cached) }
            }

            if (cached == null || !richCache.isFresh(cacheKey)) {
                try {
                    val fresh = metadataFetcher.fetchAndCache(app.repoOwner, app.repoName, lang)
                    richCache.put(cacheKey, fresh)
                    withContext(Dispatchers.Main) { onResult(fresh) }
                } catch (e: Exception) {
                    // قبل هادشي: الاستثناء كان كيتبلع بصمت بلا ما يتنادى onResult
                    // ولا أي حاجة خرى، فـ isMetadataLoading فـ AppDetailsScreen كان
                    // كيبقى true للأبد (سبيلر لا نهائي) إذا ماكانش كاش محفوظ —
                    // خصوصًا فحالة الأوفلاين أو تعذر الوصول للسيرفر. دابا كنبلغو
                    // الشاشة بالفشل صراحة (فقط إذا ماكانش عندنا كاش عرضناه فوق).
                    if (cached == null) {
                        withContext(Dispatchers.Main) { onFailure?.invoke() }
                    }
                }
            }
        }
    }

    init {
        if (tokenStore.hasToken()) {
            viewModelScope.launch { refreshProfile() }
        }

        refreshInstalledApps()

        // نلتقطو نتائج التحميل/التثبيت الجاية من DownloadInstallManager (اللي
        // كيخدم دابا مستقل عن أي شاشة) باش: (1) نعاودو فحص التطبيقات المثبتة
        // فور ما تكمل عملية (تثبيت/تحديث/حذف)، حتى الحالة تتبدّل لـ"فتح" أو
        // "تثبيت" فوريًا بلا حاجة لسحب يدوي للتحديث، و(2) نبينو رسالة توست
        // مناسبة عند النجاح (الحذف) أو الفشل، بلا ما كل شاشة تدير هادشي بروحها.
        viewModelScope.launch {
            DownloadInstallManager.events.collect { event ->
                when (event) {
                    is DownloadInstallManager.InstallEvent.Finished -> {
                        refreshInstalledApps()
                        val appContext = getApplication<Application>()
                        if (event.success) {
                            if (event.operation == "uninstall") {
                                Toast.makeText(appContext, appContext.getString(R.string.toast_uninstalled), Toast.LENGTH_SHORT).show()
                            }
                        } else if (!event.errorMessage.isNullOrBlank()) {
                            Toast.makeText(appContext, event.errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    fun updateThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun updateThemeColorIndex(index: Int) {
        _useCustomColor.value = false
        _isDynamicColor.value = false
        prefs.edit().putBoolean("use_custom_color", false).apply()
        prefs.edit().putBoolean("dynamic_color", false).apply()

        _themeColorIndex.value = index
        prefs.edit().putInt("theme_color", index).apply()
    }

    fun updateDynamicColor(isDynamic: Boolean) {
        _isDynamicColor.value = isDynamic
        prefs.edit().putBoolean("dynamic_color", isDynamic).apply()
        if (isDynamic) {
            _useCustomColor.value = false
            prefs.edit().putBoolean("use_custom_color", false).apply()
        }
    }

    fun updateCustomHexColor(hex: String) {
        _isDynamicColor.value = false
        prefs.edit().putBoolean("dynamic_color", false).apply()

        _useCustomColor.value = true
        prefs.edit().putBoolean("use_custom_color", true).apply()

        _customHexColor.value = hex
        prefs.edit().putString("custom_hex_color", hex).apply()
    }

    fun updateUiScale(scale: Float) {
        _uiScale.value = scale
        prefs.edit().putFloat("ui_scale", scale).apply()
    }

    fun updateLanguage(code: String) {
        _languageCode.value = code
        prefs.edit().putString("language", code).apply()
        
        if (_allStoreApps.value.isNotEmpty()) {
            viewModelScope.launch {
                val isArabic = code == "ar"
                storeCacheManager.getStoreApps(forceRefresh = false).onSuccess { dtoList ->
                    val mapped = dtoList.map { it.toDummyApp(languageIsArabic = isArabic) }
                    _allStoreApps.value = mapped
                    _featuredStoreApps.value = mapped
                        .filter { it.featuredTier != null }
                        .sortedWith(compareBy({ it.featuredTier }, { -it.starsCount }))
                        
                    _availableCategories.value = mapped
                        .map { it.category.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sortedWith(compareBy({ it != "Utility" && it != "أدوات" }, { it }))
                        
                    applyStoreFilters()
                }
            }
        }
    }

    fun startGithubOAuth(): String {
        val state = GitHubAuth.generateState()
        pendingState = state
        _authUiState.value = AuthUiState.Idle
        return GitHubAuth.buildAuthorizeUrl(state = state)
    }

    fun currentPendingState(): String = pendingState ?: ""

    fun completeGithubOAuth(code: String) {
        _authUiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val token = withContext(Dispatchers.IO) { exchangeCodeForToken(code) }
                if (token == null) {
                    _authUiState.value = AuthUiState.Error(
                        "تعذر تبادل الكود بتوكن حقيقي. تأكد من إعدادات GITHUB_CLIENT_ID و GITHUB_CLIENT_SECRET في Cloudflare Worker."
                    )
                    return@launch
                }
                tokenStore.saveToken(token)
                refreshProfile()
                signInToFirebase(token)
                _authUiState.value = AuthUiState.Idle
            } catch (e: Exception) {
                _authUiState.value = AuthUiState.Error(e.message ?: "فشل تسجيل الدخول.")
            } finally {
                pendingState = null
            }
        }
    }

    private suspend fun signInToFirebase(githubToken: String) {
        try {
            val body = FormBody.Builder()
                .add("action", "firebase_token")
                .add("github_token", githubToken)
                .build()
            val request = Request.Builder()
                .url(GitHubAuth.FIREBASE_TOKEN_EXCHANGE_URL)
                .header("Accept", "application/json")
                .post(body)
                .build()
            val customToken = withContext(Dispatchers.IO) {
                NetworkModule.plainOkHttpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: return@use null
                    if (!response.isSuccessful) return@use null
                    val json = JSONObject(bodyStr)
                    json.optString("firebase_token").ifBlank { null }
                }
            } ?: return
            com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCustomToken(customToken).await()
        } catch (_: Exception) {
        }
    }

    fun cancelGithubOAuth() {
        pendingState = null
        _authUiState.value = AuthUiState.Idle
    }

    fun loginWithPersonalToken(token: String) {
        if (token.isBlank()) return
        _authUiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { api.getAuthenticatedUser(bearer(token)) }
                if (resp.isSuccessful && resp.body() != null) {
                    tokenStore.saveToken(token)
                    applyUser(resp.body()!!)
                    fetchUserRepos()
                    signInToFirebase(token)
                    _authUiState.value = AuthUiState.Idle
                } else {
                    _authUiState.value = AuthUiState.Error("التوكن غير صالح أو ليس له صلاحيات كافية (HTTP ${resp.code()}).")
                }
            } catch (e: Exception) {
                _authUiState.value = AuthUiState.Error(e.message ?: "تعذر التحقق من التوكن، تحقق من الإنترنت.")
            }
        }
    }

    private fun exchangeCodeForToken(code: String): String? {
        val exchangeUrl = GitHubAuth.BACKEND_TOKEN_EXCHANGE_URL

        val formBuilder = FormBody.Builder()
            .add("action", "oauth")
            .add("code", code)
        val requestBody = formBuilder.build()

        val request = Request.Builder()
            .url(exchangeUrl)
            .header("Accept", "application/json")
            .post(requestBody)
            .build()

        NetworkModule.plainOkHttpClient.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string() ?: return null
            if (!response.isSuccessful) return null
            val json = JSONObject(bodyStr)
            if (json.has("error")) return null
            return json.optString("access_token").ifBlank { null }
        }
    }

    private suspend fun refreshProfile() {
        val token = tokenStore.getToken() ?: return
        try {
            val resp = withContext(Dispatchers.IO) { api.getAuthenticatedUser(bearer(token)) }
            if (resp.isSuccessful && resp.body() != null) {
                applyUser(resp.body()!!)
                fetchUserRepos()
            } else if (resp.code() == 401) {
                logoutGithub()
            }
        } catch (_: Exception) {
        }
    }

    private fun applyUser(user: xyz.siwane.shizucorefetch.network.GitHubUser) {
        _isGithubLoggedIn.value = true
        _isTokenActive.value = true
        _githubUsername.value = user.login
        _githubAvatarUrl.value = user.avatar_url ?: ""
        _githubName.value = user.name ?: user.login
        _githubEmail.value = user.email ?: ""
        _githubBio.value = user.bio ?: ""
        _githubProfileUrl.value = user.html_url ?: "https://github.com/${user.login}"
        
        _githubFollowers.value = user.followers ?: 0
        _githubFollowing.value = user.following ?: 0

        prefs.edit()
            .putBoolean("github_logged_in", true)
            .putBoolean("token_active", true)
            .putString("github_username", user.login)
            .putString("github_avatar_url", user.avatar_url ?: "")
            .putString("github_name", user.name ?: user.login)
            .putString("github_email", user.email ?: "")
            .putString("github_bio", user.bio ?: "")
            .putString("github_profile_url", user.html_url ?: "")
            .putInt("github_followers", user.followers ?: 0)
            .putInt("github_following", user.following ?: 0)
            .apply()
    }

    private suspend fun fetchUserRepos() {
        val token = tokenStore.getToken() ?: return
        try {
            val resp = withContext(Dispatchers.IO) { api.getAuthenticatedUserRepos(bearer(token)) }
            if (resp.isSuccessful) {
                _userRepos.value = resp.body() ?: emptyList()
            }
        } catch (_: Exception) {
        }
    }

    fun logoutGithub() {
        tokenStore.clearToken()
        _isGithubLoggedIn.value = false
        _isTokenActive.value = false
        _githubUsername.value = ""
        _githubAvatarUrl.value = ""
        _githubName.value = ""
        _githubEmail.value = ""
        _githubBio.value = ""
        _githubProfileUrl.value = ""
        _userRepos.value = emptyList()
        
        _githubFollowers.value = 0
        _githubFollowing.value = 0

        prefs.edit()
            .putBoolean("github_logged_in", false)
            .putBoolean("token_active", false)
            .putString("github_username", "")
            .putString("github_avatar_url", "")
            .putString("github_name", "")
            .putString("github_email", "")
            .putString("github_bio", "")
            .putString("github_profile_url", "")
            .putInt("github_followers", 0)
            .putInt("github_following", 0)
            .apply()
    }

    private val ratingsRepository = RatingsRepository()
    private var ratingsJob: kotlinx.coroutines.Job? = null

    private val _currentAppRating = MutableStateFlow(AppRatingSummary())
    val currentAppRating: StateFlow<AppRatingSummary> = _currentAppRating.asStateFlow()

    private val _myRating = MutableStateFlow<Int?>(null)
    val myRating: StateFlow<Int?> = _myRating.asStateFlow()

    private val _isSubmittingRating = MutableStateFlow(false)
    val isSubmittingRating: StateFlow<Boolean> = _isSubmittingRating.asStateFlow()

    private val _ratingError = MutableStateFlow<String?>(null)
    val ratingError: StateFlow<String?> = _ratingError.asStateFlow()

    fun observeAppRating(owner: String, repo: String) {
        ratingsJob?.cancel()
        _myRating.value = null
        _ratingError.value = null
        if (owner.isBlank() || repo.isBlank()) return

        ratingsJob = viewModelScope.launch {
            ratingsRepository.observeRating(owner, repo).collect { summary ->
                _currentAppRating.value = summary
            }
        }

        val username = _githubUsername.value
        if (username.isNotBlank()) {
            viewModelScope.launch {
                try {
                    _myRating.value = ratingsRepository.getMyRating(owner, repo, username)
                } catch (_: Exception) { }
            }
        }
    }

    fun submitRating(owner: String, repo: String, stars: Int) {
        val username = _githubUsername.value
        if (username.isBlank()) {
            _ratingError.value = "يجب تسجيل الدخول أولاً لإرسال تقييم."
            return
        }
        _isSubmittingRating.value = true
        _ratingError.value = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { ratingsRepository.submitRating(owner, repo, username, stars) }
                _myRating.value = stars
            } catch (e: Exception) {
                _ratingError.value = e.message ?: "تعذر إرسال التقييم، تحقق من الإنترنت."
            } finally {
                _isSubmittingRating.value = false
            }
        }
    }

    private val _comments = MutableStateFlow<List<GitHubComment>>(emptyList())
    val comments: StateFlow<List<GitHubComment>> = _comments.asStateFlow()

    private val _commentsIssueNumber = MutableStateFlow<Int?>(null)
    val commentsIssueNumber: StateFlow<Int?> = _commentsIssueNumber.asStateFlow()

    private val _isLoadingComments = MutableStateFlow(false)
    val isLoadingComments: StateFlow<Boolean> = _isLoadingComments.asStateFlow()

    private val _commentsError = MutableStateFlow<String?>(null)
    val commentsError: StateFlow<String?> = _commentsError.asStateFlow()

    private val _isPostingComment = MutableStateFlow(false)
    val isPostingComment: StateFlow<Boolean> = _isPostingComment.asStateFlow()

    private val _commentReactions = MutableStateFlow<Map<Long, List<GitHubReaction>>>(emptyMap())
    val commentReactions: StateFlow<Map<Long, List<GitHubReaction>>> = _commentReactions.asStateFlow()

    private val _hiddenCommentIds = MutableStateFlow(
        (prefs.getStringSet("hidden_comment_ids", emptySet()) ?: emptySet()).mapNotNull { it.toLongOrNull() }.toSet()
    )
    val hiddenCommentIds: StateFlow<Set<Long>> = _hiddenCommentIds.asStateFlow()

    val isCurrentUserAdmin: Boolean
        get() = _githubUsername.value.equals(xyz.siwane.shizucorefetch.data.StoreConfig.ADMIN_USERNAME, ignoreCase = true)

    fun loadComments(owner: String, repo: String) {
        _isLoadingComments.value = true
        _commentsError.value = null
        _comments.value = emptyList()
        _commentsIssueNumber.value = null
        viewModelScope.launch {
            try {
                val issuesResp = withContext(Dispatchers.IO) {
                    api.listIssues(owner, repo, state = "open", labels = null)
                }
                if (!issuesResp.isSuccessful) {
                    _commentsError.value = "تعذر تحميل التعليقات (HTTP ${issuesResp.code()})."
                    return@launch
                }
                val issue = issuesResp.body()?.firstOrNull { it.title.contains("Store Comments", ignoreCase = true) }
                if (issue == null) {
                    _commentsIssueNumber.value = null
                    _comments.value = emptyList()
                    return@launch
                }
                _commentsIssueNumber.value = issue.number
                fetchCommentsAndReactions(owner, repo, issue.number)
            } catch (e: Exception) {
                _commentsError.value = e.message ?: "تعذر الاتصال بـ GitHub."
            } finally {
                _isLoadingComments.value = false
            }
        }
    }

    private suspend fun fetchCommentsAndReactions(owner: String, repo: String, issueNumber: Int) {
        val resp = withContext(Dispatchers.IO) { api.listComments(owner, repo, issueNumber) }
        if (resp.isSuccessful) {
            val list = resp.body() ?: emptyList()
            _comments.value = list
            val reactionsMap = mutableMapOf<Long, List<GitHubReaction>>()
            withContext(Dispatchers.IO) {
                list.forEach { comment ->
                    try {
                        val rResp = api.listCommentReactions(owner, repo, comment.id)
                        if (rResp.isSuccessful) {
                            reactionsMap[comment.id] = rResp.body() ?: emptyList()
                        }
                    } catch (_: Exception) { }
                }
            }
            _commentReactions.value = reactionsMap
        } else {
            _commentsError.value = "تعذر تحميل التعليقات (HTTP ${resp.code()})."
        }
    }

    fun postComment(owner: String, repo: String, appName: String, body: String) {
        val token = tokenStore.getToken()
        if (token.isNullOrBlank()) {
            _commentsError.value = "يجب تسجيل الدخول أولاً لنشر تعليق."
            return
        }
        if (body.isBlank()) return
        _isPostingComment.value = true
        viewModelScope.launch {
            try {
                var issueNumber = _commentsIssueNumber.value
                if (issueNumber == null) {
                    val createResp = withContext(Dispatchers.IO) {
                        api.createIssue(
                            bearer(token),
                            owner,
                            repo,
                            CreateIssueRequest(
                                title = "💬 Store Comments — $appName",
                                body = "هذا الـ issue مخصص لتعليقات مستخدمي متجر ShizuCoreFetch على تطبيق $appName. يُدار تلقائيًا من التطبيق.",
                                labels = null
                            )
                        )
                    }
                    if (!createResp.isSuccessful || createResp.body() == null) {
                        val err = createResp.errorBody()?.string() ?: ""
                        _commentsError.value = "تعذر إنشاء صندوق تعليقات جديد (HTTP ${createResp.code()}): $err"
                        return@launch
                    }
                    issueNumber = createResp.body()!!.number
                    _commentsIssueNumber.value = issueNumber
                }

                val resp = withContext(Dispatchers.IO) {
                    api.addComment(bearer(token), owner, repo, issueNumber!!, CreateCommentRequest(body))
                }
                if (resp.isSuccessful) {
                    fetchCommentsAndReactions(owner, repo, issueNumber)
                } else {
                    val err = resp.errorBody()?.string() ?: ""
                    _commentsError.value = "تعذر نشر التعليق (HTTP ${resp.code()}): $err"
                }
            } catch (e: Exception) {
                _commentsError.value = e.message ?: "تعذر الاتصال بـ GitHub."
            } finally {
                _isPostingComment.value = false
            }
        }
    }

    fun editComment(owner: String, repo: String, commentId: Long, newBody: String) {
        val token = tokenStore.getToken() ?: return
        viewModelScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    api.editComment(bearer(token), owner, repo, commentId, CreateCommentRequest(newBody))
                }
                if (resp.isSuccessful) {
                    _commentsIssueNumber.value?.let { fetchCommentsAndReactions(owner, repo, it) }
                } else {
                    _commentsError.value = "تعذر تعديل التعليق (HTTP ${resp.code()})."
                }
            } catch (e: Exception) {
                _commentsError.value = e.message ?: "تعذر الاتصال بـ GitHub."
            }
        }
    }

    fun deleteComment(owner: String, repo: String, commentId: Long) {
        val token = tokenStore.getToken() ?: return
        viewModelScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { api.deleteComment(bearer(token), owner, repo, commentId) }
                if (resp.isSuccessful) {
                    _comments.value = _comments.value.filterNot { it.id == commentId }
                } else {
                    _commentsError.value = "تعذر حذف التعليق (HTTP ${resp.code()})."
                }
            } catch (e: Exception) {
                _commentsError.value = e.message ?: "تعذر الاتصال بـ GitHub."
            }
        }
    }

    fun toggleCommentReaction(owner: String, repo: String, commentId: Long, content: String) {
        val token = tokenStore.getToken() ?: return
        val username = _githubUsername.value
        viewModelScope.launch {
            try {
                val existing = _commentReactions.value[commentId].orEmpty()
                val mine = existing.firstOrNull { it.content == content && it.user?.login == username }
                if (mine != null) {
                    withContext(Dispatchers.IO) {
                        api.deleteCommentReaction(bearer(token), owner, repo, commentId, mine.id)
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        api.addCommentReaction(bearer(token), owner, repo, commentId, CreateReactionRequest(content))
                    }
                }
                val rResp = withContext(Dispatchers.IO) { api.listCommentReactions(owner, repo, commentId) }
                if (rResp.isSuccessful) {
                    val updated = _commentReactions.value.toMutableMap()
                    updated[commentId] = rResp.body() ?: emptyList()
                    _commentReactions.value = updated
                }
            } catch (e: Exception) {
                _commentsError.value = e.message ?: "تعذر تحديث الريأكشن."
            }
        }
    }

    fun adminHideCommentFromStore(commentId: Long) {
        if (!isCurrentUserAdmin) return
        val updated = _hiddenCommentIds.value + commentId
        _hiddenCommentIds.value = updated
        prefs.edit().putStringSet("hidden_comment_ids", updated.map { it.toString() }.toSet()).apply()
    }

    fun adminUnhideCommentFromStore(commentId: Long) {
        if (!isCurrentUserAdmin) return
        val updated = _hiddenCommentIds.value - commentId
        _hiddenCommentIds.value = updated
        prefs.edit().putStringSet("hidden_comment_ids", updated.map { it.toString() }.toSet()).apply()
    }
    
    fun toggleBookmark(appName: String) {
        val currentSet = _bookmarkedApps.value.toMutableSet()
        if (currentSet.contains(appName)) {
            currentSet.remove(appName)
        } else {
            currentSet.add(appName)
        }
        _bookmarkedApps.value = currentSet
        prefs.edit().putStringSet("bookmarked_apps", currentSet).apply()
    }

    fun isBookmarked(appName: String): Boolean {
        return _bookmarkedApps.value.contains(appName)
    }
}
