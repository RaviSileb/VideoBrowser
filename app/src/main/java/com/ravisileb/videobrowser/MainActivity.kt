package com.ravisileb.videobrowser

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URI

class MainActivity : ComponentActivity() {
    private lateinit var displayRotationController: DisplayRotationController
    private val uiHandler = Handler(Looper.getMainLooper())
    private val hideUiRunnable = Runnable { hideSystemUi() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        displayRotationController = DisplayRotationController(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            VideoBrowserTheme {
                VideoBrowserApp(
                    orientationController = displayRotationController,
                    onRequestHideSystemUi = { scheduleHideSystemUi() },
                )
            }
        }
        hideSystemUi()
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        scheduleHideSystemUi()
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun scheduleHideSystemUi() {
        uiHandler.removeCallbacks(hideUiRunnable)
        uiHandler.postDelayed(hideUiRunnable, 120L)
    }

    private fun hideSystemUi() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }
}

@Composable
private fun VideoBrowserTheme(content: @Composable () -> Unit) {
    Surface(color = ComposeColor(0xFF111111)) {
        content()
    }
}

@Composable
fun VideoBrowserApp(
    orientationController: DisplayRotationController,
    onRequestHideSystemUi: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val defaultUrl = "https://www.google.com"
    val guard = remember { AdRedirectGuard(4000L) }
    var tabs by remember { mutableStateOf(listOf(defaultUrl)) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var addressText by remember(tabs[selectedTabIndex]) { mutableStateOf(tabs[selectedTabIndex]) }
    var loading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(defaultUrl) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var currentOrientation by remember { mutableStateOf(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) }

    val normalizeUrl: (String) -> String = { raw ->
        val trimmed = raw.trim()
        when {
            trimmed.isEmpty() -> defaultUrl
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            else -> "https://$trimmed"
        }
    }

    fun openUrl(rawUrl: String) {
        val newUrl = normalizeUrl(rawUrl)
        val updatedTabs = tabs.toMutableList()
        if (selectedTabIndex < updatedTabs.size) {
            updatedTabs[selectedTabIndex] = newUrl
        }
        tabs = updatedTabs
        addressText = newUrl
        currentUrl = newUrl
        webViewRef.value?.loadUrl(newUrl)
        onRequestHideSystemUi()
    }

    fun toggleRotation() {
        currentOrientation = orientationController.nextOrientation()
        activity?.requestedOrientation = currentOrientation
        onRequestHideSystemUi()
    }

    fun updateFromWebView(webView: WebView) {
        val targetUrl = tabs.getOrElse(selectedTabIndex) { defaultUrl }
        if (webView.url != targetUrl) {
            webView.loadUrl(targetUrl)
        }
        webViewRef.value = webView
        canGoBack = webView.canGoBack()
        canGoForward = webView.canGoForward()
        currentUrl = targetUrl
        addressText = targetUrl
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor(0xFF101010)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ComposeColor(0xFF1A1A1A))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { webViewRef.value?.goBack() },
                enabled = canGoBack,
                modifier = Modifier.width(78.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ComposeColor.White,
                    containerColor = ComposeColor(0xFF232323),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Back")
            }

            OutlinedButton(
                onClick = { webViewRef.value?.goForward() },
                enabled = canGoForward,
                modifier = Modifier.width(90.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ComposeColor.White,
                    containerColor = ComposeColor(0xFF232323),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Next")
            }

            OutlinedButton(
                onClick = { webViewRef.value?.reload() },
                modifier = Modifier.width(88.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ComposeColor.White,
                    containerColor = ComposeColor(0xFF232323),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Reload")
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(ComposeColor(0xFF2A2A2A), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                BasicTextField(
                    value = addressText,
                    onValueChange = { addressText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                ) { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (addressText.isEmpty()) {
                            Text("https://", color = ComposeColor(0xFFB0B0B0))
                        }
                        innerTextField()
                    }
                }
            }

            OutlinedButton(
                onClick = { toggleRotation() },
                modifier = Modifier.width(92.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ComposeColor.White,
                    containerColor = ComposeColor(0xFF2D4AB5),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("switch")
            }

            OutlinedButton(
                onClick = {
                    val nextUrl = defaultUrl
                    tabs = tabs + nextUrl
                    selectedTabIndex = tabs.lastIndex
                    addressText = nextUrl
                    currentUrl = nextUrl
                    webViewRef.value?.loadUrl(nextUrl)
                    onRequestHideSystemUi()
                },
                modifier = Modifier.width(72.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ComposeColor.White,
                    containerColor = ComposeColor(0xFF1F5F2F),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("+")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(ComposeColor(0xFF2B2B2B)),
        )

        AndroidView(
            modifier = Modifier
                .fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    val activityWindow = (context as? Activity)?.window
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadsImagesAutomatically = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                        allowFileAccess = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mediaPlaybackRequiresUserGesture = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0"
                    }
                    CookieManager.getInstance().setAcceptCookie(true)
                    setKeepScreenOn(true)
                    var pageStartedAtMs: Long = 0L
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            return url.contains("doubleclick.net") ||
                                url.contains("googlesyndication.com") ||
                                url.contains("googleadservices.com") ||
                                url.contains("adnxs.com")
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            loading = true
                            pageStartedAtMs = System.currentTimeMillis()
                            val safeUrl = url ?: currentUrl
                            guard.markPage(safeUrl)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            loading = false
                            if (!url.isNullOrEmpty()) {
                                val elapsed = System.currentTimeMillis() - pageStartedAtMs
                                val normalized = normalizeUrl(url)
                                val shouldRecover = guard.shouldRecoverRedirect(url, elapsed)
                                if (shouldRecover && view != null) {
                                    val fallback = tabs.getOrElse(selectedTabIndex) { defaultUrl }
                                    if (fallback.isNotEmpty()) {
                                        view.loadUrl(fallback)
                                        return
                                    }
                                }
                                addressText = normalized
                                currentUrl = normalized
                                if (selectedTabIndex < tabs.size) {
                                    val updatedTabs = tabs.toMutableList()
                                    updatedTabs[selectedTabIndex] = normalized
                                    tabs = updatedTabs
                                }
                            }
                            canGoBack = view?.canGoBack() == true
                            canGoForward = view?.canGoForward() == true
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            activityWindow?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            setKeepScreenOn(true)
                        }

                        override fun onHideCustomView() {
                            activityWindow?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            setKeepScreenOn(true)
                        }
                    }
                    webViewRef.value = this
                    loadUrl(tabs[selectedTabIndex])
                }
            },
            update = { webView ->
                updateFromWebView(webView)
            },
        )
    }
}
