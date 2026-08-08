package xyz.siwane.shizucorefetch

import android.content.Context
import org.json.JSONObject
import java.util.Locale

/**
 * دعم احتياطي (fallback) لبيانات Fastlane-style Android metadata
 * تم تعديله ليدعم البنية المباشرة مثل app/src/foss/fastlane/en-US/ دون اشتراط وجود كلمة android.
 */
object FastlaneMetadataFetcher {

    data class FastlaneData(
        val title: String = "",
        val shortDescription: String = "",
        val fullDescription: String = "",
        val iconDownloadUrl: String = "",
        val featureGraphicDownloadUrl: String = "",
        val screenshotUrls: List<String> = emptyList(),
        val changelog: String = ""
    ) {
        fun hasUsefulContent(): Boolean =
            title.isNotEmpty() || shortDescription.isNotEmpty() || fullDescription.isNotEmpty() ||
                iconDownloadUrl.isNotEmpty() || featureGraphicDownloadUrl.isNotEmpty() ||
                screenshotUrls.isNotEmpty()
    }

    private val PREFERRED_LOCALES = listOf("en-US", "en-us", "en", "ar")

    fun fetch(
        context: Context,
        developer: String,
        repoName: String,
        onFound: (FastlaneData) -> Unit,
        onNotFound: () -> Unit
    ) {
        try {
            // 1. جلب شجرة ملفات المستودع بالكامل بطلب واحد
            var branchToUse = "main"
            var treeUrl = "https://api.github.com/repos/$developer/$repoName/git/trees/$branchToUse?recursive=1"
            var result = GithubClient.get(context, treeUrl, "application/vnd.github.v3+json")

            if (result.code != 200) {
                branchToUse = "master"
                treeUrl = "https://api.github.com/repos/$developer/$repoName/git/trees/$branchToUse?recursive=1"
                result = GithubClient.get(context, treeUrl, "application/vnd.github.v3+json")
            }

            if (result.code != 200 || result.body == null) {
                onNotFound()
                return
            }

            val treeJson = JSONObject(result.body)
            val treeArray = treeJson.optJSONArray("tree") ?: return onNotFound()

            // 2. استخراج جميع المسارات داخل المستودع
            val allPaths = mutableListOf<String>()
            for (i in 0 until treeArray.length()) {
                val entry = treeArray.optJSONObject(i) ?: continue
                if (entry.optString("type") == "blob") {
                    allPaths.add(entry.optString("path"))
                }
            }

            // البحث عن الملفات التي تنتهي بـ title.txt وتكون ضمن هيكل fastlane أو metadata
            val titlePaths = allPaths.filter { path ->
                val lower = path.lowercase()
                lower.endsWith("/title.txt") && (lower.contains("fastlane") || lower.contains("metadata"))
            }

            if (titlePaths.isEmpty()) {
                onNotFound()
                return
            }

            // 3. اختيار أفضل لغة متاحة (تفضيل لغة الجهاز أو en-US)
            val deviceLang = Locale.getDefault().language
            val chosenTitlePath = titlePaths.firstOrNull { path -> PREFERRED_LOCALES.any { path.contains("/$it/", true) } }
                ?: titlePaths.firstOrNull { path -> path.contains("/$deviceLang", true) }
                ?: titlePaths.first()

            // استخراج المسار الأساسي للمجلد (مثال: app/src/foss/fastlane/en-US)
            val localeBasePath = chosenTitlePath.substringBeforeLast("/")

            // فلترة مسارات هذا المجلد فقط لتجنب التداخل
            val localePaths = allPaths.filter { it.startsWith("$localeBasePath/", ignoreCase = true) }

            // 4. جلب النصوص عبر الرابط الخام
            val title = readRawTextFile(context, developer, repoName, branchToUse, "$localeBasePath/title.txt")
            val shortDesc = readRawTextFile(context, developer, repoName, branchToUse, "$localeBasePath/short_description.txt")
            val fullDesc = readRawTextFile(context, developer, repoName, branchToUse, "$localeBasePath/full_description.txt")

            // 5. استخراج الصور والشاشات بدقة من نطاق المجلد المحدد
            val iconPath = localePaths.firstOrNull { it.startsWith("$localeBasePath/images/icon.", ignoreCase = true) }
            val featurePath = localePaths.firstOrNull { it.startsWith("$localeBasePath/images/featureGraphic.", ignoreCase = true) }

            val iconUrl = iconPath?.let { "https://raw.githubusercontent.com/$developer/$repoName/$branchToUse/$it" } ?: ""
            val featureGraphicUrl = featurePath?.let { "https://raw.githubusercontent.com/$developer/$repoName/$branchToUse/$it" } ?: ""

            val screenshots = localePaths
                .filter { it.startsWith("$localeBasePath/images/phoneScreenshots/", ignoreCase = true) }
                .map { "https://raw.githubusercontent.com/$developer/$repoName/$branchToUse/$it" }
                .sorted()

            // 6. استخراج أحدث Changelog
            val changelogPath = localePaths
                .filter { it.startsWith("$localeBasePath/changelogs/", ignoreCase = true) && it.endsWith(".txt") }
                .maxByOrNull { it.substringAfterLast("/").removeSuffix(".txt").toLongOrNull() ?: -1L }

            val changelog = changelogPath?.let { readRawTextFile(context, developer, repoName, branchToUse, it) } ?: ""

            val data = FastlaneData(
                title = title.trim(),
                shortDescription = shortDesc.trim(),
                fullDescription = fullDesc.trim(),
                iconDownloadUrl = iconUrl,
                featureGraphicDownloadUrl = featureGraphicUrl,
                screenshotUrls = screenshots,
                changelog = changelog.trim()
            )

            if (data.hasUsefulContent()) {
                onFound(data)
            } else {
                onNotFound()
            }

        } catch (e: Exception) {
            onNotFound()
        }
    }

    private fun readRawTextFile(context: Context, developer: String, repoName: String, branch: String, path: String): String {
        return try {
            val rawUrl = "https://raw.githubusercontent.com/$developer/$repoName/$branch/$path"
            val result = GithubClient.get(context, rawUrl, "text/plain")
            if (result.code == 200 && result.body != null) {
                var text = result.body
                if (text.startsWith("\uFEFF")) text.substring(1) else text
            } else ""
        } catch (e: Exception) {
            ""
        }
    }
}
