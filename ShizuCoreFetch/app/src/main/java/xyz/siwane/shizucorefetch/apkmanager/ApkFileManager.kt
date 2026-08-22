package xyz.siwane.shizucorefetch.apkmanager

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

/** بيانات حقيقية لملف APK واحد على القرص */
data class ApkFileInfo(
    val file: File,
    val fileName: String,
    val sizeDisplay: String,
    val packageName: String?,
    val appLabel: String?,
    val versionName: String?,
    val isInstalled: Boolean
)

object ApkFileManager {

    fun downloadDir(context: Context): File {
        // تم التحديث للحفظ في المسار المطلوب Download/ShizuCoreFetch
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ShizuCoreFetch")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun listApkFiles(context: Context): List<ApkFileInfo> {
        val dir = downloadDir(context)
        val files = dir.listFiles { f -> f.isFile && f.extension.equals("apk", ignoreCase = true) } ?: emptyArray()
        val pm = context.packageManager

        return files.sortedByDescending { it.lastModified() }.map { file ->
            val packageInfo = try {
                pm.getPackageArchiveInfo(file.absolutePath, 0)
            } catch (_: Exception) {
                null
            }
            val packageName = packageInfo?.packageName
            val appLabel = packageInfo?.applicationInfo?.let {
                try {
                    it.sourceDir = file.absolutePath
                    it.publicSourceDir = file.absolutePath
                    pm.getApplicationLabel(it).toString()
                } catch (_: Exception) {
                    null
                }
            }
            val versionName = packageInfo?.versionName
            val isInstalled = packageName != null && isPackageInstalled(context, packageName)

            ApkFileInfo(file, file.name, formatFileSize(file.length()), packageName, appLabel, versionName, isInstalled)
        }
    }

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * نتيجة تفصيلية للتثبيت/الحذف الصامت عبر Shizuku — ماشي Boolean بسيط، حتى نقدر
     * نشوف بالضبط شنو وقع لما يفشل (بلا هاد التفاصيل، مستحيل نفرقو بين "شيزوكو ماعندوش
     * صلاحية"، "pm مالقيناهش" (مشكلة PATH فوضع Root)، أو رسالة خطأ حقيقية من pm نفسو
     * زي INSTALL_FAILED_TEST_ONLY أو INSTALL_FAILED_VERIFICATION_FAILURE).
     */
    sealed class SilentOpResult {
        data object Success : SilentOpResult()
        data class Failure(val reason: String) : SilentOpResult()
    }

    fun buildInstallIntent(context: Context, file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun runShizukuProcess(command: String): Process? {
        return try {
            val method = rikka.shizuku.Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * يستهلك stream معين (stdout أو stderr) بالكامل فـ thread منفصل ويجمع محتواه.
     *
     * السبب الحقيقي وراء نجاح التثبيت الصامت عند تفعيل شيزوكو عبر Root وفشله عبر
     * شيزوكو اللاسلكي (Wireless adb / Wireless debugging): عملية `pm install` البعيدة
     * (المُنشأة عبر Shizuku.newProcess) تكتب سطور تشخيصية على stdout/stderr أثناء
     * التثبيت الصامت (streaming install)، وخصوصًا عبر جلسة adb (shell) اللي عادة كتكون
     * أكثر إسهابًا فالإخراج من جلسة root المباشرة. الكود القديم كان كيكتب ملف الـ APK
     * كاملاً لـ stdin أولاً (input.copyTo(output)) ثم يستدعي process.waitFor() بلا ما
     * يقرا خط واحد من stdout/stderr. لما يمتلئ buffer الأنبوب (pipe buffer، عادة 64KB
     * على لينكس) لأي واحد من الاثنين، العملية البعيدة كتوقف (block) عند الكتابة، وبيناتنا
     * نحنا واقفين (blocked) كنكتبو باقي الـ APK لـ stdin → deadlock تام بين الطرفين، وهو
     * تفسير تقني حقيقي (pipe buffer deadlock)، ماشي تخمين. الحل الصحيح والمعروف مع
     * java.lang.Process: استهلاك كل الـ streams الثلاثة (stdin/stdout/stderr) بالتوازي،
     * فـ threads منفصلة، بلا انتظار.
     */
    private fun drainStreamAsync(stream: java.io.InputStream, sink: StringBuilder): Thread {
        val thread = Thread {
            try {
                stream.bufferedReader().use { reader ->
                    val buffer = CharArray(4096)
                    while (true) {
                        val read = reader.read(buffer)
                        if (read == -1) break
                        synchronized(sink) { sink.append(buffer, 0, read) }
                    }
                }
            } catch (_: Throwable) {
                // انقطاع الأنبوب (broken pipe) عند إنهاء العملية أمر متوقع، ماشي خطأ حقيقي
            }
        }
        thread.isDaemon = true
        thread.start()
        return thread
    }

    // pm بالمسار الكامل، ماشي بالاسم المجرد: لما شيزوكو كيخدم فوضع Root (خصوصًا عبر
    // su من Magisk/KernelSU)، الـ process الجديد ممكن يتصنع بـ PATH ناقصة (بيئة su
    // مجردة، ماشي بيئة adb shell الكاملة)، فالاسم المجرد "pm" ممكن يعطي "not found"
    // رغم أن الصلاحية موجودة فعليًا. المسار الكامل كيخدم فكل الحالات الثلاث (Root،
    // ADB سلكي، ADB لاسلكي) لأنو ماكيعتمدش على PATH نهائيًا.
    private const val PM_BIN = "/system/bin/pm"

    suspend fun installSilentlyViaShizuku(file: File): SilentOpResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (!rikka.shizuku.Shizuku.pingBinder()) {
            return@withContext SilentOpResult.Failure("Shizuku binder not connected")
        }
        if (rikka.shizuku.Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            return@withContext SilentOpResult.Failure("Shizuku permission not granted")
        }
        // uid Shizuku الحالي (0 = root، 2000 = shell عبر ADB سلكي أو لاسلكي — نفس
        // الصلاحية بالضبط فالحالتين). كنرفقوه مع أي خطأ باش يبان بالضبط أي وضع فشل.
        val uid = try { rikka.shizuku.Shizuku.getUid() } catch (_: Throwable) { -1 }

        try {
            // -r: السماح بإعادة التثبيت فوق نسخة موجودة. -d: السماح بالتنزيل لإصدار
            // أقدم إذا لزم. -t: السماح بحزم test-only — شائعة فبنايات CI مفتوحة
            // المصدر من GitHub Actions، وبدونها كيفشل التثبيت بـ INSTALL_FAILED_TEST_ONLY
            // رغم أن صلاحية شيزوكو سليمة 100%.
            val process = runShizukuProcess("$PM_BIN install -r -d -t -S ${file.length()}")
                ?: return@withContext SilentOpResult.Failure("تعذّر إنشاء عملية Shizuku (uid=$uid)")

            val stdout = StringBuilder()
            val stderr = StringBuilder()
            // لازم نبدأو استهلاك stdout و stderr *قبل* ما نكتبو stdin، حتى لا يمتلئ
            // الـ buffer ديالهم أثناء عملية الكتابة الطويلة (ملف APK قد يكون كبير)
            val stdoutThread = drainStreamAsync(process.inputStream, stdout)
            val stderrThread = drainStreamAsync(process.errorStream, stderr)

            try {
                file.inputStream().use { input ->
                    process.outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (_: java.io.IOException) {
                // إذا سكّر pm جلسة التثبيت (session) بشكل مبكر بسبب فشل حقيقي (مثلاً صلاحية
                // مرفوضة)، ممكن stdin يتسكر قبل ما نكمّلو الكتابة (broken pipe). هادشي ماشي
                // بالضرورة فشل صامت للعملية كاملة، فكنكملو للـ exit code الحقيقي بدل ما
                // نرجعو false مباشرة بلا تحقق.
            }

            val exitCode = process.waitFor()
            stdoutThread.join(5000)
            stderrThread.join(5000)

            // بعض إصدارات pm عبر adb shell كترجع exit code غير صفري فبعض الحالات رغم
            // نجاح التثبيت فعليًا (رسالة "Success" مكتوبة فـ stdout) — كنتحققو من الاثنين
            // معًا بدل الاعتماد على exit code وحدها فقط.
            val combinedOutput = (synchronized(stdout) { stdout.toString() } + synchronized(stderr) { stderr.toString() }).trim()
            if (exitCode == 0 || combinedOutput.contains("Success", ignoreCase = true)) {
                SilentOpResult.Success
            } else {
                SilentOpResult.Failure("pm install فشل (uid=$uid, exit=$exitCode): ${combinedOutput.ifBlank { "بلا أي إخراج" }}")
            }
        } catch (e: Throwable) {
            SilentOpResult.Failure("${e.javaClass.simpleName}: ${e.message} (uid=$uid)")
        }
    }

    suspend fun uninstallSilentlyViaShizuku(packageName: String): SilentOpResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (!rikka.shizuku.Shizuku.pingBinder()) {
            return@withContext SilentOpResult.Failure("Shizuku binder not connected")
        }
        if (rikka.shizuku.Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            return@withContext SilentOpResult.Failure("Shizuku permission not granted")
        }
        val uid = try { rikka.shizuku.Shizuku.getUid() } catch (_: Throwable) { -1 }

        try {
            val process = runShizukuProcess("$PM_BIN uninstall '$packageName'")
                ?: return@withContext SilentOpResult.Failure("تعذّر إنشاء عملية Shizuku (uid=$uid)")

            val stdout = StringBuilder()
            val stderr = StringBuilder()
            val stdoutThread = drainStreamAsync(process.inputStream, stdout)
            val stderrThread = drainStreamAsync(process.errorStream, stderr)

            val exitCode = process.waitFor()
            stdoutThread.join(5000)
            stderrThread.join(5000)

            val combinedOutput = (synchronized(stdout) { stdout.toString() } + synchronized(stderr) { stderr.toString() }).trim()
            if (exitCode == 0 || combinedOutput.contains("Success", ignoreCase = true)) {
                SilentOpResult.Success
            } else {
                SilentOpResult.Failure("pm uninstall فشل (uid=$uid, exit=$exitCode): ${combinedOutput.ifBlank { "بلا أي إخراج" }}")
            }
        } catch (e: Throwable) {
            SilentOpResult.Failure("${e.javaClass.simpleName}: ${e.message} (uid=$uid)")
        }
    }

    fun buildUninstallIntent(packageName: String): Intent {
        return Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildLaunchIntent(context: Context, packageName: String): Intent? {
        return context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun deleteApk(file: File): Boolean = try {
        file.delete()
    } catch (_: Exception) {
        false
    }

    suspend fun downloadApk(
        context: Context,
        url: String,
        fileName: String,
        onProgress: (Float) -> Unit = {}
    ): File = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val targetFile = File(downloadDir(context), safeName)
        val request = okhttp3.Request.Builder().url(url).get().build()
        xyz.siwane.shizucorefetch.network.NetworkModule.plainOkHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("فشل تحميل الملف (HTTP ${response.code}).")
            val body = response.body ?: throw Exception("استجابة تحميل فارغة.")
            val totalBytes = body.contentLength().takeIf { it > 0 } ?: -1L
            var readBytes = 0L
            body.byteStream().use { input ->
                targetFile.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        readBytes += read
                        if (totalBytes > 0) onProgress((readBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                    }
                }
            }
        }
        onProgress(1f)
        targetFile
    }

    private const val PACKAGE_NAME_MAP_PREFS = "resolved_package_names"

    fun rememberPackageName(context: Context, appId: String, packageName: String) {
        if (appId.isBlank() || packageName.isBlank()) return
        context.getSharedPreferences(PACKAGE_NAME_MAP_PREFS, Context.MODE_PRIVATE)
            .edit().putString(appId, packageName).apply()
    }

    fun getKnownPackageName(context: Context, appId: String): String? {
        if (appId.isBlank()) return null
        return context.getSharedPreferences(PACKAGE_NAME_MAP_PREFS, Context.MODE_PRIVATE)
            .getString(appId, null)
    }

    private fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        return if (kb >= 1024) String.format("%.1f MB", kb / 1024.0) else String.format("%.0f KB", kb)
    }
}
