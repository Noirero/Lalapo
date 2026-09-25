package eu.kanade.presentation.more

import android.text.format.Formatter
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import tachiyomi.i18n.MR
import tachiyomi.i18n.animiru.AMMR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

enum class SyncProvider {
    None,
    GoogleDrive,
    SyncYomi,
    Multiple,
}

data class SyncHealthState(
    val enabled: Boolean = false,
    val provider: SyncProvider = SyncProvider.None,
    val lastSyncTimestamp: Long = 0L,
    val lastError: String? = null,
    val isSyncing: Boolean = false,
)

data class ExtensionHealthState(
    val updates: Int = 0,
    val untrusted: Int = 0,
) {
    val hasIssue: Boolean
        get() = updates > 0 || untrusted > 0
}

data class StorageHealthState(
    val isLow: Boolean = false,
    val availableBytes: Long = -1L,
)

@Composable
fun MoreScreen(
    downloadQueueStateProvider: () -> DownloadQueueState,
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    syncHealth: SyncHealthState,
    extensionHealth: ExtensionHealthState,
    storageHealth: StorageHealthState,
    onClickDownloadQueue: () -> Unit,
    onClickSyncAndBackup: () -> Unit,
    onSyncNow: () -> Unit,
    onClickExtensionHealth: () -> Unit,
    onClickStorageHealth: () -> Unit,
    onClickStats: () -> Unit,
    onClickSettings: () -> Unit,
    onClickAbout: () -> Unit,
) {
    val context = LocalContext.current

    Scaffold { contentPadding ->
        ScrollbarLazyColumn(
            modifier = Modifier.padding(contentPadding),
        ) {
            item { LogoHeader() }

            item {
                SwitchPreferenceWidget(
                    title = stringResource(AMMR.strings.am_label_private_session),
                    subtitle = stringResource(
                        if (incognitoMode) {
                            AMMR.strings.am_private_session_active_summary
                        } else {
                            AMMR.strings.am_pref_incognito_mode_summary
                        },
                    ),
                    icon = ImageVector.vectorResource(R.drawable.ic_glasses_24dp),
                    checked = incognitoMode,
                    onCheckedChanged = onIncognitoModeChange,
                )
            }
            item {
                SwitchPreferenceWidget(
                    title = stringResource(AMMR.strings.am_label_offline_mode),
                    subtitle = stringResource(
                        if (downloadedOnly) {
                            AMMR.strings.am_offline_mode_active_summary
                        } else {
                            MR.strings.downloaded_only_summary
                        },
                    ),
                    icon = Icons.Outlined.CloudOff,
                    checked = downloadedOnly,
                    onCheckedChanged = onDownloadedOnlyChange,
                )
            }

            item { HorizontalDivider() }

            item {
                val downloadQueueState = downloadQueueStateProvider()
                TextPreferenceWidget(
                    title = stringResource(MR.strings.label_download_queue),
                    subtitle = when (downloadQueueState) {
                        DownloadQueueState.Stopped -> null
                        is DownloadQueueState.Paused -> {
                            val pending = downloadQueueState.pending
                            if (pending == 0) {
                                stringResource(MR.strings.paused)
                            } else {
                                "${stringResource(MR.strings.paused)} • ${
                                    pluralStringResource(
                                        MR.plurals.download_queue_summary,
                                        count = pending,
                                        pending,
                                    )
                                }"
                            }
                        }
                        is DownloadQueueState.Downloading -> {
                            val pending = downloadQueueState.pending
                            pluralStringResource(MR.plurals.download_queue_summary, count = pending, pending)
                        }
                    },
                    icon = Icons.Outlined.GetApp,
                    onPreferenceClick = onClickDownloadQueue,
                )
            }

            item {
                val provider = syncProviderLabel(syncHealth.provider)
                val subtitle = when {
                    !syncHealth.enabled -> stringResource(AMMR.strings.am_sync_not_configured)
                    syncHealth.isSyncing -> stringResource(AMMR.strings.am_sync_in_progress, provider)
                    !syncHealth.lastError.isNullOrBlank() -> stringResource(
                        AMMR.strings.am_sync_health_issue,
                        provider,
                    )
                    syncHealth.lastSyncTimestamp <= 0L -> stringResource(
                        AMMR.strings.am_sync_never_completed,
                        provider,
                    )
                    else -> stringResource(
                        AMMR.strings.am_sync_last_completed,
                        provider,
                        relativeTimeSpanString(syncHealth.lastSyncTimestamp),
                    )
                }

                TextPreferenceWidget(
                    title = stringResource(AMMR.strings.am_label_sync_backup),
                    subtitle = subtitle,
                    icon = Icons.Outlined.Sync,
                    iconTint = if (!syncHealth.lastError.isNullOrBlank()) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    widget = if (syncHealth.enabled) {
                        {
                            IconButton(
                                onClick = onSyncNow,
                                enabled = !syncHealth.isSyncing,
                            ) {
                                if (syncHealth.isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.padding(8.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Sync,
                                        contentDescription = stringResource(AMMR.strings.am_sync_now),
                                    )
                                }
                            }
                        }
                    } else {
                        null
                    },
                    onPreferenceClick = onClickSyncAndBackup,
                )
            }

            if (extensionHealth.hasIssue) {
                item {
                    val subtitle = buildList {
                        if (extensionHealth.updates > 0) {
                            add(
                                stringResource(
                                    AMMR.strings.am_extension_health_updates,
                                    extensionHealth.updates,
                                ),
                            )
                        }
                        if (extensionHealth.untrusted > 0) {
                            add(
                                stringResource(
                                    AMMR.strings.am_extension_health_untrusted,
                                    extensionHealth.untrusted,
                                ),
                            )
                        }
                    }.joinToString(" • ")

                    TextPreferenceWidget(
                        title = stringResource(AMMR.strings.am_source_extension_health),
                        subtitle = subtitle,
                        icon = Icons.Outlined.WarningAmber,
                        iconTint = MaterialTheme.colorScheme.error,
                        onPreferenceClick = onClickExtensionHealth,
                    )
                }
            }

            if (storageHealth.isLow) {
                item {
                    val available = storageHealth.availableBytes
                        .takeIf { it >= 0L }
                        ?.let { Formatter.formatFileSize(context, it) }
                    TextPreferenceWidget(
                        title = stringResource(AMMR.strings.am_storage_health_low),
                        subtitle = available?.let {
                            stringResource(AMMR.strings.am_storage_health_available, it)
                        },
                        icon = Icons.Outlined.Storage,
                        iconTint = MaterialTheme.colorScheme.error,
                        onPreferenceClick = onClickStorageHealth,
                    )
                }
            }

            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.label_stats),
                    icon = Icons.Outlined.QueryStats,
                    onPreferenceClick = onClickStats,
                )
            }

            item { HorizontalDivider() }

            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.label_settings),
                    icon = Icons.Outlined.Settings,
                    onPreferenceClick = onClickSettings,
                )
            }
            item {
                TextPreferenceWidget(
                    title = stringResource(MR.strings.pref_category_about),
                    icon = Icons.Outlined.Info,
                    onPreferenceClick = onClickAbout,
                )
            }
        }
    }
}

@Composable
private fun syncProviderLabel(provider: SyncProvider): String {
    return stringResource(
        when (provider) {
            SyncProvider.None -> AMMR.strings.am_sync_provider_none
            SyncProvider.GoogleDrive -> AMMR.strings.am_sync_provider_google_drive
            SyncProvider.SyncYomi -> AMMR.strings.am_sync_provider_syncyomi
            SyncProvider.Multiple -> AMMR.strings.am_sync_provider_multiple
        },
    )
}
