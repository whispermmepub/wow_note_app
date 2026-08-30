package com.whispermmepub.wownote.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.whispermmepub.wownote.ui.components.FloatingCircleButton
import com.whispermmepub.wownote.ui.components.FloatingPressable
import com.whispermmepub.wownote.ui.components.FloatingSurface
import org.json.JSONArray

@Composable
fun BrowserScreen(
    onCapture: (title: String, text: String, url: String) -> Unit
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf("https://www.google.com") }
    var address by remember { mutableStateOf(currentUrl) }
    var title by remember { mutableStateOf("Browser") }
    var status by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("Browser", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        FloatingSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            elevation = 12.dp,
            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = address,
                    onValueChange = { address = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                FloatingPressable(
                    onClick = {
                        val normalized = normalizeUrl(address)
                        address = normalized
                        webView?.loadUrl(normalized)
                    },
                    modifier = Modifier.height(36.dp),
                    shapeRadius = 14.dp,
                    elevation = 5.dp,
                    backgroundColor = Color(0xFF007AFF)
                ) {
                    Text("Go", color = Color.White, modifier = Modifier.align(Alignment.Center).padding(horizontal = 13.dp), fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        FloatingSurface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            cornerRadius = 24.dp,
            elevation = 13.dp,
            backgroundColor = Color.White,
            contentPadding = PaddingValues(0.dp)
        ) {
            BrowserWebView(
                initialUrl = currentUrl,
                onCreated = { webView = it },
                onPageChanged = { newTitle, newUrl ->
                    title = newTitle.ifBlank { "Browser" }
                    currentUrl = newUrl
                    address = newUrl
                }
            )
        }
        Spacer(Modifier.height(10.dp))
        FloatingSurface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 86.dp),
            cornerRadius = 23.dp,
            elevation = 16.dp,
            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                BrowserIcon(Icons.Rounded.ArrowBack, "Back") { webView?.takeIf { it.canGoBack() }?.goBack() }
                BrowserIcon(Icons.Rounded.ArrowForward, "Forward") { webView?.takeIf { it.canGoForward() }?.goForward() }
                BrowserIcon(Icons.Rounded.Refresh, "Reload") { webView?.reload() }
                BrowserIcon(Icons.Rounded.ContentCopy, "Save selection") {
                    val wv = webView ?: return@BrowserIcon
                    wv.evaluateJavascript("(function(){return window.getSelection ? window.getSelection().toString() : '';})()") { raw ->
                        val text = decodeJavascriptString(raw).trim()
                        if (text.isBlank()) status = "စာသားကို select လုပ်ပြီး ထပ်နှိပ်ပါ"
                        else {
                            onCapture(title, text, currentUrl)
                            status = "Selected text ကို note သိမ်းပြီးပြီ"
                        }
                    }
                }
                BrowserIcon(Icons.Rounded.Description, "Save page") {
                    val wv = webView ?: return@BrowserIcon
                    wv.evaluateJavascript("(function(){return document.body ? document.body.innerText.slice(0,20000) : '';})()") { raw ->
                        val text = decodeJavascriptString(raw).trim()
                        onCapture(title, text, currentUrl)
                        status = "Page ကို note သိမ်းပြီးပြီ"
                    }
                }
                BrowserIcon(Icons.Rounded.OpenInBrowser, "External") {
                    runCatching {
                        webView?.context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)))
                    }
                }
            }
        }
    }

    status?.let {
        FloatingSurface(
            modifier = Modifier.padding(top = 84.dp),
            cornerRadius = 18.dp,
            elevation = 16.dp,
            backgroundColor = Color(0xEE1C1C1E),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Text(it, color = Color.White, fontSize = 13.sp)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BrowserWebView(
    initialUrl: String,
    onCreated: (WebView) -> Unit,
    onPageChanged: (String, String) -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                webChromeClient = object : WebChromeClient() {
                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        val url = view?.url.orEmpty()
                        onPageChanged(title.orEmpty(), url)
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val uri = request.url
                        return if (uri.scheme == "http" || uri.scheme == "https") {
                            false
                        } else {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                            true
                        }
                    }
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        onPageChanged(view.title.orEmpty(), url)
                    }
                }
                loadUrl(initialUrl)
                onCreated(this)
            }
        }
    )
}

@Composable
private fun BrowserIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    FloatingCircleButton(
        onClick = onClick,
        modifier = Modifier.size(42.dp),
        elevation = 5.dp,
        backgroundColor = Color(0xFFF2F2F7)
    ) {
        Icon(icon, label, tint = Color(0xFF2C2C2E), modifier = Modifier.align(Alignment.Center).size(19.dp))
    }
}

private fun normalizeUrl(value: String): String {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return "https://www.google.com"
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    return if (trimmed.contains('.') && !trimmed.contains(' ')) "https://$trimmed"
    else "https://www.google.com/search?q=" + Uri.encode(trimmed)
}

private fun decodeJavascriptString(raw: String): String = runCatching {
    JSONArray("[$raw]").getString(0)
}.getOrDefault(raw.trim('"'))
