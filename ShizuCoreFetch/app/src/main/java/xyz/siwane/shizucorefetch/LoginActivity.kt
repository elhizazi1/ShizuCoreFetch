package xyz.siwane.shizucorefetch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import xyz.siwane.shizucorefetch.databinding.ActivityLoginBinding
import java.net.HttpURLConnection
import kotlin.concurrent.thread

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var authWebView: WebView? = null
    private var pendingOAuthState: String? = null

    // يمنع أي محاولة دخول ثانية (نقر متكرر أو onResume مكرر) أثناء تبادل التوكن،
    // وهو السبب الأساسي الذي كان يؤدي إلى Secondary Rate Limit من GitHub
    private var isExchangingToken = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // تفعيل الـ Edge-to-Edge الذكي
        EdgeToEdgeHelper.setup(this, binding.root)

        // التعديل الأول: التحقق من أن التوكن ليس Null وليس نصاً فارغاً
        val token = AuthManager.getToken(this)
        if (!token.isNullOrEmpty()) {
            navigateToHome()
            return
        }

        // التعديل الثاني: التحقق مما إذا كان هناك أمر ببدء تسجيل الدخول فوراً
        if (intent.getBooleanExtra("auto_start_github", false)) {
            startNativeGitHubLogin()
        }

        // زر تسجيل الدخول
        binding.btnLoginGithub.setOnClickListener {
            startNativeGitHubLogin()
        }

        // زر الدخول كزائر
        binding.btnGuestLogin.setOnClickListener {
            navigateToHome()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isExchangingToken) {
                    // تجاهل الرجوع أثناء تبادل التوكن لمنع حالة تسجيل دخول معلّقة
                    return
                }
                if (authWebView != null) {
                    if (authWebView!!.canGoBack()) {
                        authWebView!!.goBack()
                    } else {
                        closeAuthWebView()
                    }
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun startNativeGitHubLogin() {
        if (isExchangingToken) return // تجاهل أي طلب دخول جديد أثناء معالجة طلب سابق
        binding.btnLoginGithub.isEnabled = false
        binding.btnGuestLogin.isEnabled = false // تعطيل زر الزائر أثناء التحميل
        Toast.makeText(this, "Connecting to GitHub...", Toast.LENGTH_SHORT).show()
        
        // state عشوائي للحماية من CSRF: يجب أن يعود بنفس القيمة عند الرجوع من GitHub
        val state = java.util.UUID.randomUUID().toString()
        pendingOAuthState = state

        val authUrl = Uri.parse("https://github.com/login/oauth/authorize").buildUpon()
            .appendQueryParameter("client_id", Constants.GITHUB_CLIENT_ID)
            // public_repo كافٍ لنشر/حذف تعليقات وreactions على مستودعات عامة فقط،
            // بدل "repo" الذي يمنح وصولاً كاملاً (قراءة وكتابة) للمستودعات الخاصة أيضاً
            .appendQueryParameter("scope", "public_repo")
            .appendQueryParameter("redirect_uri", Constants.GITHUB_REDIRECT_URI)
            .appendQueryParameter("state", state)
            .build()
            .toString()

        authWebView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            elevation = 100f 
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            CookieManager.getInstance().setAcceptCookie(true)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url.toString()
                    
                    if (url.startsWith(Constants.GITHUB_REDIRECT_URI)) {
                        val code = request?.url?.getQueryParameter("code")
                        val returnedState = request?.url?.getQueryParameter("state")
                        if (code != null && returnedState != null && returnedState == pendingOAuthState) {
                            pendingOAuthState = null
                            Toast.makeText(this@LoginActivity, "Authenticating safely...", Toast.LENGTH_SHORT).show()
                            exchangeCodeForTokenSafely(code)
                        } else {
                            Toast.makeText(this@LoginActivity, "Login failed.", Toast.LENGTH_SHORT).show()
                            binding.btnGuestLogin.isEnabled = true
                        }
                        closeAuthWebView()
                        return true
                    }
                    return super.shouldOverrideUrlLoading(view, request)
                }
            }
        }

        (binding.root as ViewGroup).addView(authWebView)
        authWebView?.loadUrl(authUrl)
    }

    private fun closeAuthWebView() {
        authWebView?.let {
            (binding.root as ViewGroup).removeView(it)
            it.destroy()
            authWebView = null
        }
        binding.btnLoginGithub.isEnabled = true
        binding.btnGuestLogin.isEnabled = true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val uri = intent.data
        if (uri != null && uri.scheme == "shizufetch") {
            val code = uri.getQueryParameter("code")
            val returnedState = uri.getQueryParameter("state")
            if (isExchangingToken) {
                // طلب دخول قيد المعالجة بالفعل، تجاهل أي نداء onResume مكرر لنفس الـ code
                intent.data = null
                return
            }
            if (code != null && returnedState != null && returnedState == pendingOAuthState) {
                pendingOAuthState = null
                binding.btnLoginGithub.isEnabled = false
                binding.btnGuestLogin.isEnabled = false
                exchangeCodeForTokenSafely(code)
            } else if (code != null) {
                Toast.makeText(this, "Login failed.", Toast.LENGTH_SHORT).show()
            }
            intent.data = null
        }
    }

    // عدد المحاولات القصوى وتأخير Exponential Backoff (1s, 2s, 4s) كما طُلب في التقرير
    private val maxTokenExchangeAttempts = 4
    private val backoffBaseMillis = 1000L

    private fun exchangeCodeForTokenSafely(code: String) {
        isExchangingToken = true
        showLoadingOverlay(getString(R.string.login_authenticating))
        thread {
            attemptTokenExchange(code, attempt = 1)
        }
    }

    private fun attemptTokenExchange(code: String, attempt: Int) {
        val outcome = performTokenExchangeRequest(code)

        when (outcome) {
            is TokenExchangeOutcome.Success -> {
                mainHandler.post {
                    AuthManager.saveToken(this@LoginActivity, outcome.token)
                    hideLoadingOverlay()
                    isExchangingToken = false
                    navigateToHome()
                }
            }
            is TokenExchangeOutcome.NonRetryable -> {
                mainHandler.post {
                    finishExchangeWithFailure(outcome.message)
                }
            }
            is TokenExchangeOutcome.Retryable -> {
                if (attempt >= maxTokenExchangeAttempts) {
                    mainHandler.post {
                        finishExchangeWithFailure(getString(R.string.login_failed_final))
                    }
                    return
                }
                // تأخير تصاعدي صامت في الخلفية: 1s, 2s, 4s... بدون إزعاج المستخدم
                // بمحاولات دخول متكررة قد تفعّل Secondary Rate Limit من GitHub
                val delayMillis = backoffBaseMillis * (1L shl (attempt - 1))
                mainHandler.post {
                    updateLoadingStatus(getString(R.string.login_retrying, attempt, maxTokenExchangeAttempts - 1))
                }
                Thread.sleep(delayMillis)
                attemptTokenExchange(code, attempt + 1)
            }
        }
    }

    private sealed class TokenExchangeOutcome {
        data class Success(val token: String) : TokenExchangeOutcome()
        data class Retryable(val reason: String) : TokenExchangeOutcome()
        data class NonRetryable(val message: String) : TokenExchangeOutcome()
    }

    private fun performTokenExchangeRequest(code: String): TokenExchangeOutcome {
        return try {
            // 🔴 تصحيح الخلل الأساسي: كنا نستخدم HttpURLConnection مباشرة، والذي
            // يحوّل تلقائياً أي POST تتم إعادة توجيهه (وهو ما يحدث دوماً مع روابط
            // Apps Script) إلى GET فارغ الجسم، فيفقد الخادم قيمة "code" ولا ينفّذ
            // doPost إطلاقاً - وهو السبب الحقيقي وراء فشل تسجيل الدخول دائماً.
            // GasHttpClient يحافظ على طريقة POST وجسم الطلب عبر أي إعادة توجيه.
            val encodedCode = java.net.URLEncoder.encode(code, "UTF-8")
            val result = GasHttpClient.postForm(Constants.GAS_URL, "code=$encodedCode")

            when {
                result.code == HttpURLConnection.HTTP_OK && result.body != null -> {
                    val jsonObject = JSONObject(result.body)
                    if (jsonObject.has("access_token")) {
                        TokenExchangeOutcome.Success(jsonObject.getString("access_token"))
                    } else {
                        // الخادم رد لكن بدون توكن (كود مستخدم مسبقاً، أو منتهي الصلاحية) - لا فائدة من إعادة المحاولة
                        TokenExchangeOutcome.NonRetryable(getString(R.string.login_failed_final))
                    }
                }
                result.code == 429 || result.code >= 500 || result.code == -1 -> {
                    // تجاوز حد الطلبات، خطأ خادم مؤقت، أو انقطاع شبكة/مهلة اتصال: يستحق إعادة المحاولة
                    TokenExchangeOutcome.Retryable("HTTP ${result.code}")
                }
                else -> TokenExchangeOutcome.NonRetryable(getString(R.string.login_failed_final))
            }
        } catch (e: Exception) {
            TokenExchangeOutcome.Retryable(e.message ?: "error")
        }
    }

    private fun finishExchangeWithFailure(message: String) {
        isExchangingToken = false
        hideLoadingOverlay()
        binding.btnLoginGithub.isEnabled = true
        binding.btnGuestLogin.isEnabled = true
        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
    }

    private fun showLoadingOverlay(status: String) {
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.tvLoadingStatus.text = status
    }

    private fun updateLoadingStatus(status: String) {
        binding.tvLoadingStatus.text = status
    }

    private fun hideLoadingOverlay() {
        binding.loadingOverlay.visibility = View.GONE
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
