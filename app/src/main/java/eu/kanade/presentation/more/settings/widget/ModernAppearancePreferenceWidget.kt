package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.presentation.components.ModernMetadataChip
import eu.kanade.presentation.components.ModernPrimaryButton
import eu.kanade.presentation.components.ModernSegmentedButton
import eu.kanade.presentation.components.ModernSurface
import eu.kanade.presentation.theme.TachiyomiTheme
import tachiyomi.i18n.animiru.AMMR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ModernAppearancePreferenceWidget(
    checked: Boolean,
    appTheme: AppTheme,
    amoled: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
) {
    BasePreferenceWidget(
        title = stringResource(AMMR.strings.am_modern_appearance_preview),
        subcomponent = {
            TachiyomiTheme(
                appTheme = appTheme,
                amoled = amoled,
                modernAppearance = true,
            ) {
                ModernAppearancePreview()
            }
        },
    )

    SwitchPreferenceWidget(
        title = stringResource(AMMR.strings.am_modern_appearance),
        subtitle = stringResource(AMMR.strings.am_modern_appearance_summary),
        checked = checked,
        onCheckedChanged = onCheckedChanged,
    )
}

@Composable
private fun ModernAppearancePreview() {
    val segmentLabels = listOf(
        AMMR.strings.am_modern_preview_detail,
        AMMR.strings.am_modern_preview_episodes,
        AMMR.strings.am_modern_preview_info,
    )

    ModernSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PrefsHorizontalPadding),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(AMMR.strings.am_modern_preview_title),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(AMMR.strings.am_modern_preview_metadata),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            )

            ModernPrimaryButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(AMMR.strings.am_modern_preview_action))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModernMetadataChip(
                    onClick = {},
                    label = { Text(stringResource(AMMR.strings.am_modern_preview_chip)) },
                    selected = true,
                )
            }

            MultiChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                segmentLabels.forEachIndexed { index, label ->
                    ModernSegmentedButton(
                        checked = index == 0,
                        onCheckedChange = {},
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = segmentLabels.size,
                        ),
                    ) {
                        Text(stringResource(label))
                    }
                }
            }
        }
    }
}
