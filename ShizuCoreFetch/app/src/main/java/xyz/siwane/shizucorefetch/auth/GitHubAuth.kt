package xyz.siwane.shizucorefetch.auth

import android.net.Uri
import java.net.URLEncoder

/**
 * إعدادات مصادقة GitHub الحقيقية عبر OAuth App الكلاسيكي.
 * هذا النظام يسمح للتطبيق بالحصول على صلاحية public_repo الشاملة،
 * مما يتيح للمستخدمين نشر التعليقات على أي مستودع في المتجر دون قيود.
 */
object GitHubAuth {

    // ⚠️ تنبيه: ضع هنا الـ Client ID الخاص بتطبيق الـ (OAuth App) وليس الـ (GitHub App)
    const val CLIENT_ID = "Ov23liLCIpJ2DnLo2wCc" 
    const val REDIRECT_URI = "shizufetch://callback"

    // الصلاحيات المطلوبة: قراءة بيانات المستخدم + التعامل مع الـ Issues/التعليقات + الريبوهات العامة
    const val SCOPE = "read:user user:email public_repo"

    const val AUTHORIZE_URL = "https://github.com/login/oauth/authorize"
    const val TOKEN_URL = "https://github.com/login/oauth/access_token"

    // مسار الوركر الكلاسيكي OAuth المخصص للتبادل الآمن
    const val BACKEND_TOKEN_EXCHANGE_URL = "https://corefetch.siwane.xyz/?action=oauth"

    // مسار تبادل التوكن مع Firebase
    const val FIREBASE_TOKEN_EXCHANGE_URL = "https://corefetch.siwane.xyz/?action=firebase_token"

    fun generateState(): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..24).map { allowedChars.random() }.joinToString("")
    }

    fun buildAuthorizeUrl(state: String): String {
        val enc = { s: String -> URLEncoder.encode(s, "UTF-8") }
        return buildString {
            append(AUTHORIZE_URL)
            append("?client_id=").append(enc(CLIENT_ID))
            append("&redirect_uri=").append(enc(REDIRECT_URI))
            append("&scope=").append(enc(SCOPE))
            append("&state=").append(enc(state))
            append("&allow_signup=true")
        }
    }

    fun isRedirect(url: String): Boolean = url.startsWith(REDIRECT_URI)

    fun extractParam(url: String, name: String): String? {
        return Uri.parse(url).getQueryParameter(name)
    }
}
