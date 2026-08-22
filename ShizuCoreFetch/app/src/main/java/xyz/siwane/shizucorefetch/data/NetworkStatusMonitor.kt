package xyz.siwane.shizucorefetch.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge

/**
 * مراقبة حقيقية لحالة الاتصال بالإنترنت عبر ConnectivityManager.NetworkCallback —
 * ماشي قيمة ثابتة (كانت شارة "Online" فالشاشة الرئيسية دايمًا خضراء بلا أي فحص
 * حقيقي للشبكة). بيرجع Flow<Boolean> كيبعث true/false فوريًا مع كل تغيير حقيقي
 * فحالة الشبكة (تفعيل/تعطيل الطيران، فقدان الواي فاي، الخ)، بلا حاجة لـ polling
 * وحيد (شوف أسفله ليش زدنا polling احتياطي أيضًا).
 *
 * الفحص هنا صارم: الشبكة تعتبر "متصلة" فقط إذا كان عندها فعليًا قدرة الإنترنت
 * (NET_CAPABILITY_INTERNET) وتم التحقق منها فعليًا (NET_CAPABILITY_VALIDATED) —
 * ماشي غير مرتبطة بواي فاي بلا إنترنت حقيقي (بوابة تسجيل دخول واي فاي عمومي مثلاً).
 */
object NetworkStatusMonitor {

    private fun hasRealInternet(caps: NetworkCapabilities?): Boolean {
        if (caps == null) return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun checkNow(connectivityManager: ConnectivityManager): Boolean =
        hasRealInternet(connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork))

    /** المسار السريع: NetworkCallback حقيقي، كيبعث فوريًا مع كل تغيير حقيقي. */
    private fun callbackBased(connectivityManager: ConnectivityManager): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(hasRealInternet(connectivityManager.getNetworkCapabilities(network)))
            }

            override fun onLost(network: Network) {
                trySend(checkNow(connectivityManager))
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(hasRealInternet(networkCapabilities))
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        trySend(checkNow(connectivityManager))

        // registerDefaultNetworkCallback (بدل registerNetworkCallback بطلب
        // مفلتر عام) كيراقب الشبكة الافتراضية الفعلية للنظام مباشرة — أدق
        // وأسرع فاكتشاف فقدان الاتصال الحقيقي، وهو الأسلوب الموصى به رسميًا
        // من Android لهاد الغرض.
        connectivityManager.registerDefaultNetworkCallback(callback)

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    /**
     * مسار احتياطي (polling كل 3 ثواني): بعض الأجهزة (خصوصًا ROMs مخصصة كيفما
     * MIUI/One UI بتحسينات بطارية عدوانية) كتؤخر أو كتوقف توصيل أحداث
     * NetworkCallback للتطبيقات لما تكون فالمقدمة مباشرة بعد التشغيل، وما
     * كتصحح غير بعد أول انتقال للخلفية والعودة (بالضبط الأعراض اللي وصفتي: أول
     * قطع شبكة بعد فتح التطبيق ما كيتكتشفش حتى تخرج وتدخل، وبعدها كيخدم عادي).
     * هاد الفحص الدوري المباشر (بلا اعتماد كلي على توصيل الـ callback) كيضمن
     * اكتشاف الحالة الصحيحة فأقصى 3 ثواني فكل الحالات، بلا حاجة لأي تدخل يدوي.
     */
    private fun pollBased(connectivityManager: ConnectivityManager): Flow<Boolean> = flow {
        while (true) {
            emit(checkNow(connectivityManager))
            delay(3000)
        }
    }

    fun observe(context: Context): Flow<Boolean> {
        val connectivityManager = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return flow { emit(false) }

        return merge(callbackBased(connectivityManager), pollBased(connectivityManager))
            .distinctUntilChanged()
    }
}
