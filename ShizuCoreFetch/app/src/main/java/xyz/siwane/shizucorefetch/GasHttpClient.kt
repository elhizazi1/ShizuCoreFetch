package xyz.siwane.shizucorefetch

import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * طبقة موحدة لتنفيذ طلبات POST نحو خادم Google Apps Script (GAS_URL).
 *
 * 🔴 المشكلة الجذرية التي تحلّها هذه الدالة (سبب فشل تسجيل الدخول دائماً):
 * روابط تنفيذ Apps Script (/exec) لا تُرجع المحتوى مباشرة، بل ترد أولاً بإعادة
 * توجيه HTTP (301/302/303) نحو نطاق script.googleusercontent.com حيث يُقدَّم
 * المحتوى الفعلي. السلوك الافتراضي لـ HttpURLConnection في جافا/أندرويد عند
 * ملاقاة إعادة توجيه بعد طلب POST هو تحويل الطلب التالي تلقائياً إلى GET
 * وإسقاط جسم الطلب (body) بالكامل - وهذا سلوك موثّق ومتوقع من HttpURLConnection.
 *
 * النتيجة العملية: كل طلب POST كنا نرسله إلى GAS (تبادل كود GitHub OAuth
 * بتوكن دخول، أو حظر تعليق) كان يصل فعلياً للخادم كطلب GET بلا أي بيانات
 * (بلا "code"، بلا "action")، فيقوم GAS بتنفيذ doGet الافتراضي (يُرجع بيانات
 * المتجر) بدل doPost المطلوب - فتفشل عملية الدخول أو حظر التعليق بصمت وبشكل
 * دائم مهما أعاد المستخدم المحاولة.
 *
 * الحل: نعطّل إعادة التوجيه التلقائي (instanceFollowRedirects = false) ونتابع
 * أي إعادة توجيه يدوياً، مع الحفاظ على طريقة POST وجسم الطلب في كل قفزة.
 */
object GasHttpClient {

    data class Result(val code: Int, val body: String?)

    fun postForm(urlString: String, formBody: String, maxHops: Int = 3): Result {
        var currentUrl = urlString
        var hops = 0
        var isFirstHop = true // فقط الطلب الأول هو POST الحقيقي الذي ينفّذ doPost على الخادم

        while (hops < maxHops) {
            hops++
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    // 🔴 التصحيح: doPost يُنفَّذ فعلياً على القفزة الأولى فقط. رابط
                    // إعادة التوجيه (script.googleusercontent.com) لا يُنفّذ أي كود من
                    // جديد، بل يجلب النتيجة الجاهزة فقط، ويتوقّع GET وليس POST -
                    // إعادة إرسال POST إليه تُرجع 405 Method Not Allowed دائماً.
                    requestMethod = if (isFirstHop) "POST" else "GET"
                    instanceFollowRedirects = false // نتحكم بإعادة التوجيه يدوياً، انظر التعليق أعلاه
                    connectTimeout = 15000
                    readTimeout = 15000
                    if (isFirstHop) {
                        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                        doOutput = true
                    }
                }

                if (isFirstHop) {
                    OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(formBody)
                        writer.flush()
                    }
                }

                val code = connection.responseCode

                if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                    val location = connection.getHeaderField("Location")
                    if (location.isNullOrEmpty()) return Result(-1, null)
                    currentUrl = location
                    isFirstHop = false // القفزات التالية كلها GET بدون جسم
                    continue
                }

                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader(Charsets.UTF_8)?.readText()
                return Result(code, body)
            } catch (e: Exception) {
                return Result(-1, null)
            } finally {
                connection?.disconnect()
            }
        }
        return Result(-1, null)
    }
}