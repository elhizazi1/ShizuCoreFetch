package xyz.siwane.shizucorefetch

object Constants {
    // 🔴 تم التحديث: الرابط الآن يشير إلى Cloudflare Worker لتوفير سرعة فائقة وتجنب حصص جوجل
    const val GAS_URL = "https://corefetch.siwane.xyz"
    
    const val GITHUB_CLIENT_ID = "Ov23liLCIpJ2DnLo2wCc"
    const val GITHUB_REDIRECT_URI = "shizufetch://callback"

    // ملاحظة أمنية: لم يعد هناك أي توكن GitHub مكتوب داخل التطبيق.
    // طلبات الزوار (غير المسجلين) تمر الآن عبر بروكسي Worker (action=proxy) الذي
    // يدير مجموعة توكنات احتياطية على الخادم فقط، انظر GithubClient.kt
}
