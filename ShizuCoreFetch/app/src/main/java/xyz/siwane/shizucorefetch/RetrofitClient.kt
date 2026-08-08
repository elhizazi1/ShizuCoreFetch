package xyz.siwane.shizucorefetch

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // رابط أساسي وهمي مؤقتاً لكي تعمل المكتبة، سيتم تجاوزه في ApiService
    private const val BASE_URL = "https://google.com/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}
