package eu.kanade.presentation.more

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                MoreCard(modifier = Modifier.padding(top = 14.dp)) {
                    MoreRow(
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
                        onCheckedChange = onIncognitoModeChange,
                    )
                    MoreRowDivider()
                    MoreRow(
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
                        onCheckedChange = onDownloadedOnlyChange,
                    )
                }
            }

            item {
                MoreCard(sectionTitle = stringResource(AMMR.strings.am_more_tools_section)) {
                    val downloadQueueState = downloadQueueStateProvider()
                    MoreRow(
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
                        onClick = onClickDownloadQueue,
                    )
                    MoreRowDivider()
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

                    MoreRow(
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
                        onClick = onClickSyncAndBackup,
                    )

                    if (extensionHealth.hasIssue) {
                        MoreRowDivider()
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

                        MoreRow(
                            title = stringResource(AMMR.strings.am_source_extension_health),
                            subtitle = subtitle,
                            icon = Icons.Outlined.WarningAmber,
                            iconTint = MaterialTheme.colorScheme.error,
                            onClick = onClickExtensionHealth,
                        )
                    }

                    if (storageHealth.isLow) {
                        MoreRowDivider()
                        val available = storageHealth.availableBytes
                            .takeIf { it >= 0L }
                            ?.let { Formatter.formatFileSize(context, it) }
                        MoreRow(
                            title = stringResource(AMMR.strings.am_storage_health_low),
                            subtitle = available?.let {
                                stringResource(AMMR.strings.am_storage_health_available, it)
                            },
                            icon = Icons.Outlined.Storage,
                            iconTint = MaterialTheme.colorScheme.error,
                            onClick = onClickStorageHealth,
                        )
                    }

                    MoreRowDivider()
                    MoreRow(
                        title = stringResource(MR.strings.label_stats),
                        icon = Icons.Outlined.QueryStats,
                        onClick = onClickStats,
                    )
                }
            }

            item {
                MoreCard(
                    modifier = Modifier.padding(bottom = 24.dp),
                    sectionTitle = stringResource(AMMR.strings.am_more_app_section),
                ) {
                    MoreRow(
                        title = stringResource(MR.strings.label_settings),
                        icon = Icons.Outlined.Settings,
                        onClick = onClickSettings,
                    )
                    MoreRowDivider()
                    MoreRow(
                        title = stringResource(MR.strings.pref_category_about),
                        icon = Icons.Outlined.Info,
                        onClick = onClickAbout,
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreCard(
    modifier: Modifier = Modifier,
    sectionTitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        if (sectionTitle != null) {
            Text(
                text = sectionTitle,
                modifier = Modifier
                    .padding(start = 12.dp, top = 14.dp, bottom = 8.dp)
                    .semantics { heading() },
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.8.sp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(colors.surfaceContainerHigh, colors.surfaceContainer, colors.surfaceContainerHigh),
                    ),
                )
                .border(1.dp, colors.outlineVariant, shape)
                .padding(vertical = 2.dp),
            content = content,
        )
    }
}

@Composable
private fun MoreRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 18.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun MoreRow(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    checked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    widget: @Composable (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = when {
        onCheckedChange != null -> Modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        )
        onClick != null -> Modifier.clickable(role = Role.Button, onClick = onClick)
        else -> Modifier
    }
    val isToggle = onCheckedChange != null
    val hasSubtitle = !subtitle.isNullOrBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (isToggle) 58.dp else 48.dp)
            .then(interaction)
            .padding(horizontal = 18.dp, vertical = if (isToggle) 8.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconShape = RoundedCornerShape(11.dp)
        Box(
            modifier = Modifier
                .size(width = 38.dp, height = if (isToggle) 38.dp else 32.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(iconTint.copy(alpha = 0.20f), iconTint.copy(alpha = 0.08f)),
                    ),
                    iconShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(if (isToggle) 24.dp else 22.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(width = 1.dp, height = if (hasSubtitle) 22.dp else 15.dp)
                .background(colors.outlineVariant),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            if (!subtitle.isNullOrBlank()) {
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(8.dp))
        if (onCheckedChange != null) {
            Switch(checked = checked, onCheckedChange = null)
        } else {
            widget?.invoke()
            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { scaleX = if (isRtl) -1f else 1f },
            )
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
