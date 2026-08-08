package xyz.siwane.shizucorefetch

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface ApiService {
    // استخدمنا @Url لكي نتمكن من وضع الرابط كاملاً لاحقاً بسهولة
    @GET
    fun getApps(@Url url: String): Call<List<AppModel>>
}
