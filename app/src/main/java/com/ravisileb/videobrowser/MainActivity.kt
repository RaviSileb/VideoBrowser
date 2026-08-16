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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URI

private const val DEFAULT_URL = "https://www.bombuj.si/"
private const val CURRENT_URL_KEY = "current_url"
private const val CURRENT_ORIENTATION_KEY = "current_orientation"

class MainActivity : ComponentActivity() {
    private lateinit var displayRotationController: DisplayRotationController
    private var currentPageUrl: String = DEFAULT_URL
    private val uiHandler = Handler(Looper.getMainLooper())
    private val hideUiRunnable = Runnable { hideSystemUi() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentPageUrl = savedInstanceState?.getString(CURRENT_URL_KEY) ?: DEFAULT_URL
        val restoredOrientation = savedInstanceState?.getInt(CURRENT_ORIENTATION_KEY)
            ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        displayRotationController = DisplayRotationController(restoredOrientation)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            VideoBrowserTheme {
                VideoBrowserApp(
                    orientationController = displayRotationController,
                    initialUrl = currentPageUrl,
                    onUrlChanged = { currentPageUrl = it },
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(CURRENT_URL_KEY, currentPageUrl)
        outState.putInt(CURRENT_ORIENTATION_KEY, displayRotationController.currentOrientation())
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
    Surface(color = ComposeColor(0xFFE5E7EB)) {
        content()
    }
}

@Composable
private fun BrowserActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    containerColor: ComposeColor = ComposeColor(0xFF2A2A2A),
    contentColor: ComposeColor = ComposeColor(0xFFFFFFFF),
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = ComposeColor(0xFF1F1F1F),
            disabledContentColor = ComposeColor(0xFF7A7A7A),
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun VideoBrowserApp(
    orientationController: DisplayRotationController,
    initialUrl: String = DEFAULT_URL,
    onUrlChanged: (String) -> Unit = {},
    onRequestHideSystemUi: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val guard = remember { AdRedirectGuard(4000L) }
    var addressText by rememberSaveable(initialUrl) { mutableStateOf(initialUrl) }
    var loading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var currentUrl by rememberSaveable(initialUrl) { mutableStateOf(initialUrl) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var currentOrientation by remember { mutableStateOf(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) }
    var pageSafeMode by remember { mutableStateOf(false) }
    var adCleanupDisabled by remember { mutableStateOf(false) }

    val normalizeUrl: (String) -> String = { raw ->
        val trimmed = raw.trim()
        when {
            trimmed.isEmpty() -> DEFAULT_URL
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            else -> "https://$trimmed"
        }
    }

    fun openUrl(rawUrl: String) {
        val newUrl = normalizeUrl(rawUrl)
        addressText = newUrl
        currentUrl = newUrl
        onUrlChanged(newUrl)
        webViewRef.value?.loadUrl(newUrl)
        onRequestHideSystemUi()
    }

    fun openHomePage() = openUrl(DEFAULT_URL)

    fun toggleSafeMode() {
        pageSafeMode = !pageSafeMode
        webViewRef.value?.post {
            webViewRef.value?.evaluateJavascript(BrowserGuard.cleanupScript(), null)
        }
        onRequestHideSystemUi()
    }

    fun toggleAdCleanup() {
        adCleanupDisabled = !adCleanupDisabled
        webViewRef.value?.post {
            webViewRef.value?.evaluateJavascript(BrowserGuard.cleanupScript(), null)
        }
        onRequestHideSystemUi()
    }

    fun toggleRotation() {
        currentOrientation = orientationController.nextOrientation()
        activity?.requestedOrientation = currentOrientation
        onRequestHideSystemUi()
    }

    fun updateFromWebView(webView: WebView) {
        if (webView.url != currentUrl) {
            webView.loadUrl(currentUrl)
        }
        webViewRef.value = webView
        canGoBack = webView.canGoBack()
        canGoForward = webView.canGoForward()
        val observedUrl = webView.url ?: currentUrl
        if (observedUrl != currentUrl) {
            currentUrl = observedUrl
            addressText = observedUrl
            onUrlChanged(observedUrl)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor(0xFF151515)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ComposeColor(0xFF1A1F2A))
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BrowserActionButton(
                    icon = Icons.Default.Home,
                    contentDescription = "Home",
                    containerColor = ComposeColor(0xFF2A2A2A),
                ) { openHomePage() }

                BrowserActionButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    enabled = canGoBack,
                    containerColor = ComposeColor(0xFF2A2A2A),
                ) { webViewRef.value?.goBack() }

                BrowserActionButton(
                    icon = Icons.Default.Search,
                    contentDescription = "Open address",
                    containerColor = ComposeColor(0xFF2A2A2A),
                ) { openUrl(addressText) }

                BrowserActionButton(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward",
                    enabled = canGoForward,
                    containerColor = ComposeColor(0xFF2A2A2A),
                ) { webViewRef.value?.goForward() }

                BrowserActionButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = "Reload",
                    containerColor = ComposeColor(0xFF2A2A2A),
                ) { webViewRef.value?.reload() }

                BrowserActionButton(
                    icon = Icons.Default.Shield,
                    contentDescription = if (pageSafeMode) "Exit safe mode" else "Safe mode",
                    containerColor = if (pageSafeMode) ComposeColor(0xFF5A7D2A) else ComposeColor(0xFF2A2A2A),
                ) { toggleSafeMode() }

                BrowserActionButton(
                    icon = Icons.Default.Block,
                    contentDescription = if (adCleanupDisabled) "Enable ad cleanup" else "Disable ad cleanup",
                    containerColor = if (adCleanupDisabled) ComposeColor(0xFF8A5A1D) else ComposeColor(0xFF2A2A2A),
                ) { toggleAdCleanup() }

                Spacer(modifier = Modifier.weight(1f))

                BrowserActionButton(
                    icon = Icons.Default.ScreenRotation,
                    contentDescription = "Switch orientation",
                    containerColor = ComposeColor(0xFF2E5DB7),
                ) { toggleRotation() }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ComposeColor(0xFF2F3541), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = addressText,
                        onValueChange = { addressText = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = ComposeColor.White,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                    ) { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ComposeColor(0xFF2F3541)),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (addressText.isEmpty()) {
                                Text("https://", color = ComposeColor(0xFFCED5E0))
                            }
                            innerTextField()
                        }
                    }

                    BrowserActionButton(
                        icon = Icons.Default.Search,
                        contentDescription = "Go to URL",
                        containerColor = ComposeColor(0xFF3B5BA9),
                    ) { openUrl(addressText) }
                }
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
                            if (BrowserGuard.shouldBlockUrl(url)) return true
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
                            val normalizedUrl = if (!url.isNullOrEmpty()) normalizeUrl(url) else currentUrl
                            if (!url.isNullOrEmpty()) {
                                val elapsed = System.currentTimeMillis() - pageStartedAtMs
                                val shouldRecover = guard.shouldRecoverRedirect(url, elapsed)
                                if (shouldRecover && view != null) {
                                    if (currentUrl.isNotEmpty()) {
                                        view.loadUrl(currentUrl)
                                        return
                                    }
                                }
                                addressText = normalizedUrl
                                currentUrl = normalizedUrl
                                onUrlChanged(normalizedUrl)
                            }
                            val shouldCleanup = BrowserGuard.shouldRunAdCleanup(
                                url = normalizedUrl,
                                pageSafeMode = pageSafeMode,
                                adCleanupDisabled = adCleanupDisabled,
                            )
                            if (shouldCleanup) {
                                view?.post {
                                    view.evaluateJavascript(BrowserGuard.cleanupScript(), null)
                                }
                                view?.postDelayed({
                                    view?.evaluateJavascript(BrowserGuard.cleanupScript(), null)
                                }, 350L)
                            }
                            canGoBack = view?.canGoBack() == true
                            canGoForward = view?.canGoForward() == true
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            activityWindow?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            setKeepScreenOn(true)
                            if (view != null) {
                                view.setOnClickListener {
                                    callback?.onCustomViewHidden()
                                }
                            }
                        }

                        override fun onHideCustomView() {
                            activityWindow?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            setKeepScreenOn(true)
                        }
                    }
                    webViewRef.value = this
                    loadUrl(currentUrl)
                }
            },
            update = { webView ->
                updateFromWebView(webView)
            },
        )
    }
}
