package xyz.siwane.shizucorefetch.data

/**
 * أدابتر خالص (دالة نقية، بلا أي حالة أو side effects): كياخد معلومات تطبيق
 * من المتجر (اسمه، اسم الحزمة كيفما جا من الباكند، اسم الحزمة المتذكَّر محليًا
 * من تثبيت سابق عبر التطبيق، ورقم الإصدار البعيد)، وكيقارنو مع خريطتي
 * التطبيقات المثبتة الحقيقية (InstalledAppsScanner)، ويرجع الحالة النهائية.
 *
 * أولوية المطابقة (كيفما طلب المستخدم صراحة): (1) اسم الحزمة الصريح من
 * الباكند — الأدق. (2) اسم الحزمة المتذكَّر محليًا من تثبيت سابق عبر هاد
 * التطبيق. (3) وأخيرًا اسم التطبيق نفسه (بعد تطبيع النص) — احتياطي ضروري
 * لما تكون كل مصادر اسم الحزمة غايبة، حتى التطبيقات المثبتة قبل يتعرف عليها
 * التطبيق بدل ما يبقى الزر "تثبيت" بالخطأ.
 */
object InstallStateResolver {

    fun resolve(
        appName: String,
        declaredPackageName: String,
        rememberedPackageName: String?,
        versionTag: String,
        installedByPackage: Map<String, InstalledAppInfo>,
        installedByName: Map<String, InstalledAppInfo>
    ): ResolvedInstallStatus {
        var matched: InstalledAppInfo? = null
        var matchedBy = "none"

        val packageCandidates = listOfNotNull(
            declaredPackageName.takeIf { it.isNotBlank() },
            rememberedPackageName?.takeIf { it.isNotBlank() }
        )
        for (candidate in packageCandidates) {
            val hit = installedByPackage[candidate]
            if (hit != null) {
                matched = hit
                matchedBy = "package"
                break
            }
        }

        if (matched == null && appName.isNotBlank()) {
            val key = InstalledAppsScanner.normalizeName(appName)
            val hit = installedByName[key]
            if (hit != null) {
                matched = hit
                matchedBy = "name"
            } else if (key.length >= 4) {
                // مطابقة جزئية احتياطية أخيرة: بعض التطبيقات (خصوصًا اللي
                // اتثبتو من برا المتجر) عندها اسم معروض على الجهاز مختلف شوية
                // عن اسم التطبيق المُعلن فبيانات المتجر (مثلاً "Drawix" مقابل
                // "Drawix Pro")، فالتطابق الكامل (===) كيفشل رغم أن التطبيق
                // مثبت فعليًا. هادي مرتبة أضعف من التطابق الكامل، فكتُستعمل
                // غير كملاذ أخير.
                val partial = installedByName.entries.firstOrNull { (installedKey, _) ->
                    installedKey.length >= 4 && (installedKey.contains(key) || key.contains(installedKey))
                }
                if (partial != null) {
                    matched = partial.value
                    matchedBy = "name_partial"
                }
            }
        }

        val finalMatch = matched ?: return ResolvedInstallStatus(
            state = InstallUiState.Install,
            matchedPackageName = null,
            installedVersionName = null,
            matchedBy = "none"
        )

        val installedVersion = finalMatch.versionName
        val needsUpdate = versionTag.isNotBlank() && installedVersion != null &&
            !versionTag.contains(installedVersion, ignoreCase = true) &&
            !installedVersion.contains(versionTag.removePrefix("v"), ignoreCase = true)

        return ResolvedInstallStatus(
            state = if (needsUpdate) InstallUiState.Update else InstallUiState.Open,
            matchedPackageName = finalMatch.packageName,
            installedVersionName = installedVersion,
            matchedBy = matchedBy
        )
    }
}
