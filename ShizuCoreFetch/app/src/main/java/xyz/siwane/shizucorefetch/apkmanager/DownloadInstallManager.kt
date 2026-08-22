package xyz.siwane.shizucorefetch.apkmanager

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.siwane.shizucorefetch.R
import xyz.siwane.shizucorefetch.data.InstallUiState
import xyz.siwane.shizucorefetch.notifications.NotificationHelper

/**
 * محرك تحميل وتثبيت موحّد ومستقل تمامًا عن أي شاشة أو Composable.
 *
 * ليش كان التحميل كيتوقف قبل عند مغادرة الشاشة أو الخروج من التطبيق؟ لأن كل
 * شاشة (StoreScreen وAppDetailsScreen) كانت كتدير `coroutineScope.launch` بـ
 * `rememberCoroutineScope()` الخاص بيها، وهاد الـ scope كيتلغى (cancel)
 * تلقائيًا من طرف Compose لما الـ Composable يخرج من composition (تنقل
 * لشاشة أخرى، أو إغلاق التطبيق). العملية كانت مربوطة بعمر الشاشة، ماشي
 * بعمر عملية التحميل الحقيقية.
 *
 * الحل هنا: العمليات كلها كتخدم على coroutine scope واحد، معرّف مرة وحدة هنا
 * (SupervisorJob) وكيعيش طول ما تعيش العملية (process) نفسها — بلا ارتباط
 * بأي Activity/Composable. الحالة (operations) هي مصدر الحقيقة الوحيد لأي
 * عملية جارية، وكل الشاشات كتقرا منها مباشرة (عبر MainViewModel.installStates)
 * عوض ما يكون عند كل واحدة نسخة خاصة بها.
 *
 * [DownloadForegroundService] كيربط مع هاد المدير باش يخلي العملية (process)
 * حية بأولوية عالية أثناء التحميل، حتى إذا التطبيق مبعّد للخلفية بالكامل.
 */
object DownloadInstallManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _operations = MutableStateFlow<Map<String, InstallUiState>>(emptyMap())
    /** appId -> الحالة الحية (Downloading/Installing) فقط. تطبيق ماعندوش عملية جارية غايب من هاد الخريطة نهائيًا. */
    val operations: StateFlow<Map<String, InstallUiState>> = _operations.asStateFlow()

    sealed class InstallEvent {
        /** operation: "install" أو "uninstall" — باش المستمع (ViewModel) يعرف أي رسالة توست يعرض عند النجاح. */
        data class Finished(val appId: String, val success: Boolean, val errorMessage: String?, val operation: String = "install") : InstallEvent()
    }

    private val _events = MutableSharedFlow<InstallEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<InstallEvent> = _events

    private fun setState(appId: String, state: InstallUiState?) {
        val current = _operations.value.toMutableMap()
        if (state == null) current.remove(appId) else current[appId] = state
        _operations.value = current
    }

    fun isBusy(appId: String): Boolean = _operations.value.containsKey(appId)

    /**
     * يبدأ عملية تحميل + تثبيت كاملة لتطبيق. آمنة الاستدعاء من أي مكان (شاشة
     * المتجر، شاشة التفاصيل، المكتبة) — إذا كانت هناك عملية جارية بالفعل لنفس
     * appId، الاستدعاء الثاني كيتجاهل بلا ما يكرر التحميل.
     */
    fun startInstall(
        context: Context,
        appId: String,
        appName: String,
        repoName: String,
        apkUrl: String,
        versionTag: String,
        isShizukuGranted: Boolean,
        isSilentInstallEnabled: Boolean
    ) {
        if (appId.isBlank() || isBusy(appId)) return
        if (apkUrl.isBlank()) {
            _events.tryEmit(InstallEvent.Finished(appId, false, null))
            return
        }

        val appContext = context.applicationContext
        val notificationId = appId.hashCode()

        DownloadForegroundService.notifyOperationStarted(appContext)

        scope.launch {
            try {
                setState(appId, InstallUiState.Downloading(0f))
                val fileName = "$repoName-${versionTag.ifBlank { "latest" }}.apk"
                val file = ApkFileManager.downloadApk(appContext, apkUrl, fileName) { progress ->
                    setState(appId, InstallUiState.Downloading(progress))
                    NotificationHelper.showDownloadProgress(appContext, notificationId, appName, (progress * 100).toInt())
                }

                setState(appId, InstallUiState.Installing)
                NotificationHelper.showDownloadProgress(appContext, notificationId, appName, 100)

                val parsedPkg = withContext(Dispatchers.IO) {
                    appContext.packageManager.getPackageArchiveInfo(file.absolutePath, 0)?.packageName
                }
                if (!parsedPkg.isNullOrBlank()) {
                    // كنسجلو اسم الحزمة الحقيقي مباشرة من الـ APK المحمَّل — مصدر
                    // 100% موثوق، كيغطي حتى التطبيقات اللي الباكند ما قدرش يحدد
                    // اسم الحزمة ديالها تلقائيًا.
                    ApkFileManager.rememberPackageName(appContext, appId, parsedPkg)
                }

                var installedSilently = false
                var silentFailureReason: String? = null
                if (isShizukuGranted && isSilentInstallEnabled) {
                    when (val result = ApkFileManager.installSilentlyViaShizuku(file)) {
                        is ApkFileManager.SilentOpResult.Success -> installedSilently = true
                        is ApkFileManager.SilentOpResult.Failure -> silentFailureReason = result.reason
                    }
                }

                if (installedSilently) {
                    NotificationHelper.showStatusNotification(
                        appContext, notificationId, appName,
                        appContext.getString(R.string.toast_installed), true
                    )
                } else {
                    // إذا كان التثبيت الصامت مفعّل ولكنو فشل، كنسجلو السبب الحقيقي فـ
                    // Logcat (مفيد باش نعرفو بالضبط أي وضع تفعيل شيزوكو (Root/ADB سلكي/
                    // ADB لاسلكي) عطا المشكلة ولاش) قبل ما نرجعو لحوار التثبيت العادي.
                    if (silentFailureReason != null) {
                        android.util.Log.w("DownloadInstallManager", "Silent install failed for $appId: $silentFailureReason")
                    }
                    appContext.startActivity(ApkFileManager.buildInstallIntent(appContext, file))
                    NotificationHelper.cancelNotification(appContext, notificationId)
                }
                _events.tryEmit(InstallEvent.Finished(appId, true, null))
            } catch (e: Exception) {
                val errorMsg = e.message ?: appContext.getString(R.string.store_error_generic)
                NotificationHelper.showStatusNotification(appContext, notificationId, appName, errorMsg, false)
                _events.tryEmit(InstallEvent.Finished(appId, false, errorMsg))
            } finally {
                setState(appId, null)
                DownloadForegroundService.notifyOperationFinished(appContext)
            }
        }
    }

    fun startUninstall(context: Context, appId: String, packageName: String, isShizukuGranted: Boolean) {
        if (packageName.isBlank() || isBusy(appId)) return
        val appContext = context.applicationContext
        scope.launch {
            try {
                var success = false
                if (isShizukuGranted) {
                    val result = ApkFileManager.uninstallSilentlyViaShizuku(packageName)
                    if (result is ApkFileManager.SilentOpResult.Success) {
                        success = true
                    } else if (result is ApkFileManager.SilentOpResult.Failure) {
                        android.util.Log.w("DownloadInstallManager", "Silent uninstall failed for $appId: ${result.reason}")
                    }
                }
                if (!success) {
                    appContext.startActivity(ApkFileManager.buildUninstallIntent(packageName))
                }
                _events.tryEmit(InstallEvent.Finished(appId, success, null, operation = "uninstall"))
            } catch (e: Exception) {
                _events.tryEmit(InstallEvent.Finished(appId, false, e.message, operation = "uninstall"))
            }
        }
    }
}
