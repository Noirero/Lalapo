package eu.kanade.presentation.anime.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.ModernAccentDivider
import eu.kanade.presentation.components.ModernSegmentedButton
import eu.kanade.presentation.components.ModernSurface
import tachiyomi.i18n.animiru.AMMR
import tachiyomi.presentation.core.i18n.stringResource

internal enum class AnimeDetailsSection {
    Detail,
    Episodes,
    Info,
}

@Composable
internal fun AnimeDetailsSegmentedNavigation(
    selected: AnimeDetailsSection,
    onSelect: (AnimeDetailsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        AnimeDetailsSection.Detail to AMMR.strings.am_anime_detail_tab_detail,
        AnimeDetailsSection.Episodes to AMMR.strings.am_anime_detail_tab_episode,
        AnimeDetailsSection.Info to AMMR.strings.am_anime_detail_tab_info,
    )

    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth(),
    ) {
        items.forEachIndexed { index, (section, label) ->
            ModernSegmentedButton(
                selected = selected == section,
                onClick = { onSelect(section) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = items.size,
                ),
            ) {
                Text(stringResource(label))
            }
        }
    }
}

@Composable
internal fun AnimeWatchingProgressCard(
    watched: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    if (total <= 0) return

    val progress = (watched.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    ModernSurface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(AMMR.strings.am_anime_detail_progress),
                style = MaterialTheme.typography.titleMedium,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            ModernAccentDivider()
            Text(
                text = stringResource(
                    AMMR.strings.am_anime_detail_progress_format,
                    watched,
                    total,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
