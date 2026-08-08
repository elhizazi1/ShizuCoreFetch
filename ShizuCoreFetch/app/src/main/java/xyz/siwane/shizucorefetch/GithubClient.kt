package xyz.siwane.shizucorefetch

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * طبقة موحدة لعمليات القراءة (GET) من GitHub API.
 *
 * - مستخدم مسجل (لديه OAuth token شخصي): اتصال مباشر بـ GitHub بتوكنه الخاص (حصة 5000 طلب/ساعة).
 * - زائر (غير مسجل): الطلب يمر عبر بروكسي GAS (action=proxy)، الذي يختار توكناً احتياطياً
 *   من مجموعة توكنات مخزّنة بأمان في Script Properties على الخادم فقط (Round-Robin)،
 *   وبهذا لا يُشحن أي توكن داخل التطبيق نفسه.
 *
 * عمليات الكتابة (نشر/تعديل/حذف تعليق، إضافة reaction) تبقى كما هي: تتطلب توكن المستخدم
 * الحقيقي مباشرة (AuthManager.getToken) ولا تمر عبر هذا البروكسي إطلاقاً.
 */
object GithubClient {

    data class Result(val code: Int, val body: String?)

    fun get(context: Context, apiUrl: String, accept: String = "application/vnd.github.v3+json"): Result {
        val userToken = AuthManager.getToken(context)

        // 1) مستخدم مسجل: اتصال مباشر بتوكنه الشخصي (حصة 5000 طلب/ساعة)
        if (!userToken.isNullOrEmpty()) {
            val direct = directGet(apiUrl, userToken, accept)
            if (direct.code in 200..299) return direct
            // التوكن المحفوظ قد يكون منتهياً/ملغى من GitHub - نكمل كزائر بدل ترك الشاشة فارغة
        }

        // 2) زائر (أو فشل توكن المستخدم): نمر عبر بروكسي GAS للاستفادة من تجمع التوكنات
        val proxyResult = proxyGet(apiUrl, accept)
        if (proxyResult.code in 200..299) return proxyResult

        // 3) 🔴 خط دفاع أخير - يحل مشكلة "تفاصيل التطبيق لا تظهر":
        // إن فشل بروكسي GAS لأي سبب (توكنات احتياطية غير صالحة على الخادم،
        // تجاوز حصة الطلبات اليومية لـ Apps Script، عطل مؤقت...)، نطلب البيانات
        // مباشرة وبشكل مجهول من GitHub API. المحتوى العام (README، shizu_store.json،
        // الإصدارات) يعمل بدون أي توكن، بحد أقصى 60 طلب/ساعة لكل IP - أفضل بكثير
        // من عدم إظهار أي بيانات إطلاقاً للزائر.
        return directGet(apiUrl, null, accept)
    }

    private fun directGet(apiUrl: String, token: String?, accept: String): Result {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", accept)
                if (!token.isNullOrEmpty()) setRequestProperty("Authorization", "token $token")
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.readText()
            Result(code, body)
        } catch (e: Exception) {
            Result(-1, null)
        } finally {
            connection?.disconnect()
        }
    }

    private fun proxyGet(apiUrl: String, accept: String): Result {
        return try {
            val encodedTarget = URLEncoder.encode(apiUrl, "UTF-8")
            val encodedAccept = URLEncoder.encode(accept, "UTF-8")
            val proxyUrl = "${Constants.GAS_URL}?action=proxy&target=$encodedTarget&accept=$encodedAccept"

            var connection = (URL(proxyUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = false // إيقاف التتبع التلقائي للتعامل مع توجيه جوجل يدوياً
                connectTimeout = 15000
                readTimeout = 15000
            }

            var httpCode = connection.responseCode

            // معالجة إعادة التوجيه (Redirect 301/302/303) من script.google.com إلى script.googleusercontent.com
            if (httpCode == 301 || httpCode == 302 || httpCode == 303) {
                val redirectUrl = connection.getHeaderField("Location")
                if (redirectUrl.isNullOrEmpty()) return Result(-1, null)
                connection = (URL(redirectUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                httpCode = connection.responseCode
            }

            if (httpCode != HttpURLConnection.HTTP_OK) return Result(httpCode, null)

            // البروكسي يرجع دائماً كود HTTP 200 من Apps Script نفسه (قيد معروف في GAS)،
            // لذلك يضع كود استجابة GitHub الحقيقي داخل الجسم بصيغة { status, body }
            val envelopeText = connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
            val envelope = JSONObject(envelopeText)
            val status = envelope.optInt("status", -1)
            
            // تم حل التحذير باستخدام getString بدلاً من optString
            val body = if (envelope.isNull("body")) null else envelope.getString("body")
            
            Result(status, body)
        } catch (e: Exception) {
            Result(-1, null)
        }
    }
}
