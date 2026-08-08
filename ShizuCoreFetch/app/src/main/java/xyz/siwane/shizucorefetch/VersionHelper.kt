package xyz.siwane.shizucorefetch

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object VersionHelper {

    fun getLocalVersion(context: Context, packageName: String): String? {
        return try {
            val pInfo = context.packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun checkUpdateAvailable(
        context: Context,
        developer: String,
        repoName: String,
        localVersion: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        thread {
            try {
                val apiUrl = "https://api.github.com/repos/$developer/$repoName/releases/latest"
                val result = GithubClient.get(context, apiUrl, "application/vnd.github.v3+json")

                if (result.code == 200 && result.body != null) {
                    val jsonObject = JSONObject(result.body)
                    val remoteVersion = jsonObject.getString("tag_name")

                    val isNewer = isVersionNewer(localVersion, remoteVersion)
                    Handler(Looper.getMainLooper()).post {
                        onResult(isNewer, remoteVersion)
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        onResult(false, null)
                    }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    onResult(false, null)
                }
            }
        }
    }

    // تم إزالة كلمة private لكي نستخدمها بذكاء في شاشة الحساب
    fun isVersionNewer(local: String, remote: String): Boolean {
        val cleanLocal = local.replace(Regex("[^0-9.]"), "")
        val cleanRemote = remote.replace(Regex("[^0-9.]"), "")

        val localParts = cleanLocal.split(".").map { it.toIntOrNull() ?: 0 }
        val remoteParts = cleanRemote.split(".").map { it.toIntOrNull() ?: 0 }

        val length = maxOf(localParts.size, remoteParts.size)
        for (i in 0 until length) {
            val l = if (i < localParts.size) localParts[i] else 0
            val r = if (i < remoteParts.size) remoteParts[i] else 0
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
