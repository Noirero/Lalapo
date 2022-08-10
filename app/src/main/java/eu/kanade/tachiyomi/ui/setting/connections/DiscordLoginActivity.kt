// AM -->
package eu.kanade.tachiyomi.ui.setting.connections

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.connections.ConnectionsManager
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.util.system.toast
import uy.kohesive.injekt.injectLazy
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FilenameFilter

class DiscordLoginActivity : BaseActivity() {
    private var authToken: String? = null
    private lateinit var webView: WebView
    private val connectionsManager: ConnectionsManager by injectLazy()

    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.discord_login_activity)
        webView = findViewById(R.id.mainWebView)
        initialiseWebView()

        class DiscordWebViewLoginClient : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(webView: WebView, str: String): Boolean {
                webView.stopLoading()
                if (str.endsWith("/app")) {
                    webView.visibility = View.GONE
                    extractToken()
                    login()
                }
                return false
            }
        }

        webView.webViewClient = DiscordWebViewLoginClient()
        login()
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled")
    private fun initialiseWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.setAppCacheEnabled(true)
        webView.settings.databaseEnabled = true
        webView.settings.domStorageEnabled = true
        webView.clearCache(true)
        webView.clearFormData()
        webView.clearHistory()
        webView.clearSslPreferences()
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    private fun login() {
        if (webView.visibility == View.VISIBLE) {
            webView.stopLoading()
            webView.visibility = View.GONE
        } else if (authToken != null) {
            setResult(Activity.RESULT_OK)
            preferences.connectionToken(connectionsManager.discord).set(authToken!!)
            toast(R.string.login_success)
            applicationInfo.dataDir.let { File("$it/app_webview/").deleteRecursively() }
            finish()
        } else {
            webView.visibility = View.VISIBLE
            webView.loadUrl("https://discord.com/login")
        }
    }

    private fun extractToken(): Boolean {
        var readLine: String
        try {
            class DiscordFilenameFilter : FilenameFilter {
                override fun accept(dir: File?, str: String): Boolean {
                    return str.endsWith(".log")
                }
            }
            val listFiles = File(filesDir.parentFile, "app_webview/Default/Local Storage/leveldb").listFiles(DiscordFilenameFilter()) ?: return false

            if (listFiles.isEmpty()) {
                return false
            }
            val bufferedReader = BufferedReader(FileReader(listFiles[0]))
            do {
                readLine = bufferedReader.readLine()
                if (readLine == null) {
                    break
                }
            } while (!readLine.contains("token"))

            val substring = readLine.substring(readLine.indexOf("token") + 5)
            val substring2 = substring.substring(substring.indexOf("\"") + 1)
            authToken = substring2.substring(0, substring2.indexOf("\""))
            return true
        } catch (e: Throwable) {
            return false
        }
    }
}
// AM <--
