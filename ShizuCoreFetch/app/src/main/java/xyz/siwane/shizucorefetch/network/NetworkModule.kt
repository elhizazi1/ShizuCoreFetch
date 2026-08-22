package xyz.siwane.shizucorefetch.network

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import xyz.siwane.shizucorefetch.data.TokenStore

object NetworkModule {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private const val CACHE_TTL_SECONDS = 120

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * معترض ذكي بنظام طبقات الحماية الثلاثي (Failover Interceptor):
     * 1. المستخدم مسجل: استخدام التوكن الشخصي وطلب GitHub مباشرة.
     * 2. غير مسجل (الطبقة الأساسية): محاولة الطلب عبر Cloudflare Worker Proxy (استهلاك التوكنات الاحتياطية).
     * 3. خطة الهروب (الملاذ الأخير): إذا فشل البروكسي (استنزاف الحصص)، يتم إعادة المحاولة مباشرة لـ GitHub بدون توكن (للاكتفاء بـ 60 طلب الخاصة بـ IP المستخدم).
     */
    private val authInjectingInterceptor = okhttp3.Interceptor { chain ->
        val original = chain.request()
        val urlString = original.url.toString()

        // نطبق نظام الطبقات فقط على طلبات GitHub API
        if (urlString.startsWith("https://api.github.com/")) {
            val token = appContext?.let { TokenStore(it).getToken() }

            if (!token.isNullOrBlank()) {
                // الطبقة الأولى: المستخدم مسجل دخوله
                val builder = original.newBuilder()
                
                // التعديل الجراحي: التحقق من عدم وجود الترويسة مسبقاً لتفادي تكرارها وكسر الاتصال
                if (original.header("Authorization") == null) {
                    builder.addHeader("Authorization", "Bearer $token")
                }
                
                val authed = builder.build()
                return@Interceptor chain.proceed(authed)
            } else {
                // الطبقة الثانية: محاولة الطلب عبر بروكسي Cloudflare
                val encodedTarget = java.net.URLEncoder.encode(urlString, "UTF-8")
                val proxyUrl = "https://corefetch.siwane.xyz/?action=proxy&target=$encodedTarget"
                
                val proxyRequest = original.newBuilder()
                    .url(proxyUrl)
                    .build()

                val proxyResponse = chain.proceed(proxyRequest)

                // الطبقة الثالثة (خطة الهروب): إذا فشل البروكسي (مثل 429 أو 503)، نلجأ فوراً للطلب المباشر بدون توكن (IP Rate Limit)
                if (!proxyResponse.isSuccessful && (proxyResponse.code == 429 || proxyResponse.code == 503 || proxyResponse.code == 502 || proxyResponse.code == 504)) {
                    proxyResponse.close() // إغلاق الاستجابة الفاشلة لمنع تسرب الموارد
                    
                    // إرسال الطلب مباشرة لـ GitHub كزائر مجهول ليعتمد على IP المستخدم
                    return@Interceptor chain.proceed(original)
                }

                return@Interceptor proxyResponse
            }
        }

        chain.proceed(original)
    }

    private val diskCache: Cache? by lazy {
        appContext?.let { ctx ->
            Cache(File(ctx.cacheDir, "github_http_cache"), 10L * 1024 * 1024)
        }
    }

    private val cacheEnforcingInterceptor = okhttp3.Interceptor { chain ->
        val response = chain.proceed(chain.request())
        if (chain.request().method == "GET") {
            response.newBuilder()
                .header("Cache-Control", "public, max-age=$CACHE_TTL_SECONDS")
                .removeHeader("Pragma")
                .build()
        } else {
            response
        }
    }

    private val cacheReadInterceptor = okhttp3.Interceptor { chain ->
        var request = chain.request()
        if (request.method == "GET") {
            request = request.newBuilder()
                .header("Cache-Control", "public, max-age=$CACHE_TTL_SECONDS")
                .build()
        }
        chain.proceed(request)
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .apply { diskCache?.let { cache(it) } }
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInjectingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/vnd.github+json")
                    .addHeader("X-GitHub-Api-Version", "2022-11-28")
                    .addHeader("User-Agent", "ShizuCoreFetch-App")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(cacheReadInterceptor)
            .addNetworkInterceptor(cacheEnforcingInterceptor)
            .build()
    }

    val githubApi: GitHubApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApiService::class.java)
    }

    // تعزيز العميل لتجاوز حظر Cloudflare برأس User-Agent شامل و وقت أطول
    val plainOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36")
                    .addHeader("Accept", "*/*")
                    .addHeader("Connection", "close")
                    .build()
                chain.proceed(request)
            }
            .build()
    }
}
