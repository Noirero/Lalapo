package eu.kanade.tachiyomi.connections.discord;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import eu.kanade.tachiyomi.R;
import eu.kanade.tachiyomi.data.preference.PreferencesHelper;

public class DiscordLoginActivity extends Activity {
    private String authToken = null;
    private WebView webView;
    private PreferencesHelper preferences;

    /* access modifiers changed from: protected */
    @SuppressLint("SetJavaScriptEnabled")
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.discord_login_activity);
        WebView webView2 = findViewById(R.id.mainWebView);
        this.webView = webView2;
        webView2.getSettings().setJavaScriptEnabled(true);
        this.webView.getSettings().setAppCacheEnabled(true);
        this.webView.getSettings().setDatabaseEnabled(true);
        this.webView.getSettings().setDomStorageEnabled(true);
        this.webView.clearCache(true);
        this.webView.clearFormData();
        this.webView.clearHistory();
        this.webView.clearSslPreferences();
        WebStorage.getInstance().deleteAllData();
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        this.webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView webView, String str) {
                Log.d("Web", "Attempt to enter " + str);
                DiscordLoginActivity.this.webView.stopLoading();
                if (!str.endsWith("/app")) {
                    return false;
                }
                DiscordLoginActivity.this.webView.setVisibility(View.GONE);
                DiscordLoginActivity.this.extractToken();
                DiscordLoginActivity.this.login();
                return false;
            }
        });
        login();
    }

    public void login() {
        if (this.webView.getVisibility() == View.VISIBLE) {
            this.webView.stopLoading();
            this.webView.setVisibility(View.GONE);
        } else if (this.authToken != null) {
            Log.i("tokenads",this.authToken);
            if(this.authToken != null) preferences.discordToken().set(this.authToken);
        } else {
            this.webView.setVisibility(View.VISIBLE);
            this.webView.loadUrl("https://discord.com/login");
        }
    }

    public boolean extractToken() {
        String readLine;
        try {
            File[] listFiles = new File(getFilesDir().getParentFile(), "app_webview/Default/Local Storage/leveldb").listFiles(new FilenameFilter() {
                public boolean accept(File file, String str) {
                    return str.endsWith(".log");
                }
            });
            if (listFiles.length == 0) {
                return false;
            }
            BufferedReader bufferedReader = new BufferedReader(new FileReader(listFiles[0]));
            do {
                readLine = bufferedReader.readLine();
                if (readLine == null) {
                    break;
                }
            } while (!readLine.contains("token"));
            assert readLine != null;
            String substring = readLine.substring(readLine.indexOf("token") + 5);
            String substring2 = substring.substring(substring.indexOf("\"") + 1);
            this.authToken = substring2.substring(0, substring2.indexOf("\""));
            return true;
        } catch (Throwable th) {
            return false;
        }
    }
}
