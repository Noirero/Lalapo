package eu.kanade.presentation.more.settings.screen

import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.network.PREF_DOH_360
import eu.kanade.tachiyomi.network.PREF_DOH_ADGUARD
import eu.kanade.tachiyomi.network.PREF_DOH_ALIDNS
import eu.kanade.tachiyomi.network.PREF_DOH_CLOUDFLARE
import eu.kanade.tachiyomi.network.PREF_DOH_CONTROLD
import eu.kanade.tachiyomi.network.PREF_DOH_DNSPOD
import eu.kanade.tachiyomi.network.PREF_DOH_GOOGLE
import eu.kanade.tachiyomi.network.PREF_DOH_LIBREDNS
import eu.kanade.tachiyomi.network.PREF_DOH_MULLVAD
import eu.kanade.tachiyomi.network.PREF_DOH_NJALLA
import eu.kanade.tachiyomi.network.PREF_DOH_QUAD101
import eu.kanade.tachiyomi.network.PREF_DOH_QUAD9
import eu.kanade.tachiyomi.network.PREF_DOH_SHECAN
import eu.kanade.tachiyomi.util.system.setDefaultSettings
import eu.kanade.tachiyomi.util.system.toast
import logcat.LogPriority
import mihon.app.di.appGraph
import okhttp3.Headers
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.i18n.animiru.AMMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import java.io.File

object SettingsNetworkScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = AMMR.strings.am_settings_network

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val networkPreferences = remember { context.appGraph.networkPreferences }
        val networkHelper = remember { context.appGraph.networkHelper }

        val userAgentPref = networkPreferences.defaultUserAgent
        val userAgent by userAgentPref.collectAsState()

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(AMMR.strings.am_settings_network),
                preferenceItems = listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_clear_cookies),
                        onClick = {
                            networkHelper.cookieJar.removeAll()
                            context.toast(MR.strings.cookies_cleared)
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_clear_webview_data),
                        onClick = {
                            try {
                                WebView(context).run {
                                    setDefaultSettings()
                                    clearCache(true)
                                    clearFormData()
                                    clearHistory()
                                    clearSslPreferences()
                                }
                                WebStorage.getInstance().deleteAllData()
                                context.applicationInfo?.dataDir?.let {
                                    File("$it/app_webview/").deleteRecursively()
                                }
                                context.toast(MR.strings.webview_data_deleted)
                            } catch (e: Throwable) {
                                logcat(LogPriority.ERROR, e)
                                context.toast(MR.strings.cache_delete_error)
                            }
                        },
                    ),
                    Preference.PreferenceItem.ListPreference(
                        preference = networkPreferences.dohProvider,
                        entries = mapOf(
                            -1 to stringResource(MR.strings.disabled),
                            PREF_DOH_CLOUDFLARE to "Cloudflare",
                            PREF_DOH_GOOGLE to "Google",
                            PREF_DOH_ADGUARD to "AdGuard",
                            PREF_DOH_QUAD9 to "Quad9",
                            PREF_DOH_ALIDNS to "AliDNS",
                            PREF_DOH_DNSPOD to "DNSPod",
                            PREF_DOH_360 to "360",
                            PREF_DOH_QUAD101 to "Quad 101",
                            PREF_DOH_MULLVAD to "Mullvad",
                            PREF_DOH_CONTROLD to "Control D",
                            PREF_DOH_NJALLA to "Njalla",
                            PREF_DOH_SHECAN to "Shecan",
                            PREF_DOH_LIBREDNS to "LibreDNS",
                        ),
                        title = stringResource(MR.strings.pref_dns_over_https),
                        onValueChanged = {
                            context.toast(MR.strings.requires_app_restart)
                            true
                        },
                    ),
                    Preference.PreferenceItem.EditTextPreference(
                        preference = userAgentPref,
                        title = stringResource(MR.strings.pref_user_agent_string),
                        onValueChanged = {
                            try {
                                Headers.Builder().add("User-Agent", it)
                                context.toast(MR.strings.requires_app_restart)
                            } catch (_: IllegalArgumentException) {
                                context.toast(MR.strings.error_user_agent_string_invalid)
                                return@EditTextPreference false
                            }
                            true
                        },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.pref_reset_user_agent_string),
                        enabled = remember(userAgent) { userAgent != userAgentPref.defaultValue() },
                        onClick = {
                            userAgentPref.delete()
                            context.toast(MR.strings.requires_app_restart)
                        },
                    ),
                ),
            ),
        )
    }
}
