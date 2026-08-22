package xyz.siwane.shizucorefetch.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import xyz.siwane.shizucorefetch.R
import xyz.siwane.shizucorefetch.auth.GitHubAuth

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GitHubLoginWebViewScreen(
    authorizeUrl: String,
    expectedState: String,
    onAuthCode: (code: String) -> Unit,
    onCancel: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    val handled = remember { mutableStateOf(false) }

    // جلب النصوص لتفادي النصوص الثابتة
    val titleText = stringResource(id = R.string.github_login_title)
    val errorConnection = stringResource(id = R.string.github_error_connection)
    val errorMismatch = stringResource(id = R.string.github_error_state_mismatch)
    val errorNoCode = stringResource(id = R.string.github_error_no_code)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(titleText) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        removeAllCookies(null)
                    }
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                if (url != null && GitHubAuth.isRedirect(url) && !handled.value) {
                                    interceptRedirect(url, expectedState, handled, onAuthCode, onError, errorMismatch, errorNoCode)
                                }
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: android.webkit.WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false
                                if (GitHubAuth.isRedirect(url)) {
                                    if (!handled.value) {
                                        interceptRedirect(url, expectedState, handled, onAuthCode, onError, errorMismatch, errorNoCode)
                                    }
                                    return true 
                                }
                                return false
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: android.webkit.WebResourceRequest?,
                                error: android.webkit.WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true && !handled.value) {
                                    onError(errorConnection)
                                }
                            }
                        }
                        loadUrl(authorizeUrl)
                    }
                }
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private fun interceptRedirect(
    url: String,
    expectedState: String,
    handled: MutableState<Boolean>,
    onAuthCode: (String) -> Unit,
    onError: (String) -> Unit,
    errorMismatchMsg: String,
    errorNoCodeMsg: String
) {
    handled.value = true
    val error = GitHubAuth.extractParam(url, "error")
    if (error != null) {
        val desc = GitHubAuth.extractParam(url, "error_description") ?: error
        onError(desc)
        return
    }
    val returnedState = GitHubAuth.extractParam(url, "state")
    val code = GitHubAuth.extractParam(url, "code")
    
    if (returnedState == null || returnedState != expectedState) {
        onError(errorMismatchMsg)
        return
    }
    if (code.isNullOrBlank()) {
        onError(errorNoCodeMsg)
        return
    }
    onAuthCode(code)
}
