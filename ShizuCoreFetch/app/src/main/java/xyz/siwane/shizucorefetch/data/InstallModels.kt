package xyz.siwane.shizucorefetch.data

/**
 * الحالة الموحّدة لعملية التثبيت/التحديث لتطبيق معين، مشتركة بين كل شاشات
 * التطبيق (المتجر، تفاصيل التطبيق، المكتبة). كانت هاد الحالة قبل معرّفة بشكل
 * منفصل ومكرر جوج مرات (فـ StoreScreen وAppDetailsScreen)، كل واحدة بمنطق
 * حساب خاص بها — هادشي كان سبب تضارب محتمل بين الشاشتين. دابا معرّفة مرة
 * وحدة هنا، ومحسوبة مرة وحدة فـ MainViewModel.installStates.
 */
sealed class InstallUiState {
    data object Install : InstallUiState()
    data object Open : InstallUiState()
    data object Update : InstallUiState()
    data class Downloading(val progress: Float) : InstallUiState()
    data object Installing : InstallUiState()
}

/** معلومات تطبيق مثبت فعليًا على الجهاز، جاية من فحص حقيقي وإجباري لـ PackageManager (ماشي كاش محلي مبني على تخمين). */
data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long
)

/**
 * نتيجة مطابقة تطبيق من المتجر مع تطبيق مثبت على الجهاز، وحالته الناتجة.
 * matchedBy توضّح كيفاش تمت المطابقة، مفيدة للتشخيص عند الحاجة:
 * "package" (اسم الحزمة، الأدق)، "name" (اسم التطبيق، احتياطي)، أو "none".
 */
data class ResolvedInstallStatus(
    val state: InstallUiState,
    val matchedPackageName: String?,
    val installedVersionName: String?,
    val matchedBy: String
)
