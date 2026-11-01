package eu.kanade.presentation.more.settings.widget

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.DesignStyle
import eu.kanade.domain.ui.model.ModernThemePreset
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.presentation.theme.ModernThemeDefinitions
import eu.kanade.presentation.theme.TachiyomiTheme
import tachiyomi.presentation.core.util.collectAsState

/** One preference owner and one selection hierarchy, reused by Settings and Onboarding. */
@Composable
internal fun AppearanceSelectionWidget(uiPreferences: UiPreferences, enableCustomEditor: Boolean = false) {
    val context = LocalContext.current
    val themeMode by uiPreferences.themeMode.collectAsState()
    val designStyle by uiPreferences.designStyle.collectAsState()
    val classicTheme by uiPreferences.appTheme.collectAsState()
    val modernPreset by uiPreferences.modernThemePreset.collectAsState()
    val amoled by uiPreferences.themeDarkAmoled.collectAsState()

    val recreate: () -> Unit = {
        (context as? Activity)?.let(ActivityCompat::recreate)
    }

    Column {
        SelectionLabel("Theme mode")
        AppThemeModePreferenceWidget(
            value = themeMode,
            onItemClick = { selected ->
                if (selected != themeMode) {
                    uiPreferences.themeMode.set(selected)
                    setAppCompatDelegateThemeMode(selected)
                }
            },
        )

        SelectionLabel("Design style")
        DesignStyleSelector(
            selected = designStyle,
            onSelect = { selected ->
                if (selected != designStyle) {
                    uiPreferences.designStyle.set(selected)
                    recreate()
                }
            },
        )

        SelectionLabel(if (designStyle == DesignStyle.CLASSIC) "Classic theme" else "Modern theme")
        if (designStyle == DesignStyle.CLASSIC) {
            AppThemePreferenceWidget(
                value = classicTheme,
                amoled = amoled,
                onItemClick = { selected ->
                    if (selected != classicTheme) {
                        uiPreferences.appTheme.set(selected)
                        recreate()
                    }
                },
            )
        } else {
            ModernThemePreferenceWidget(
                value = modernPreset,
                amoled = amoled,
                onItemClick = { selected ->
                    if (selected != modernPreset && (
                            selected == ModernThemePreset.CUSTOM || ModernThemeDefinitions.find(selected) != null
                            )
                    ) {
                        uiPreferences.modernThemePreset.set(selected)
                        recreate()
                    }
                },
            )
            if (modernPreset == ModernThemePreset.CUSTOM) {
                if (enableCustomEditor) {
                    CustomModernColorEditor(uiPreferences = uiPreferences, onApplied = recreate)
                } else {
                    Text(
                        text = "Custom uses your saved Primary color. Edit colors later in Appearance Settings.",
                        modifier = Modifier.padding(horizontal = PrefsHorizontalPadding, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(
            start = PrefsHorizontalPadding,
            end = PrefsHorizontalPadding,
            top = 12.dp,
            bottom = 4.dp,
        ),
    )
}

@Composable
private fun DesignStyleSelector(
    selected: DesignStyle,
    onSelect: (DesignStyle) -> Unit,
) {
    val styles = DesignStyle.entries
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PrefsHorizontalPadding),
    ) {
        styles.forEachIndexed { index, style ->
            SegmentedButton(
                selected = selected == style,
                onClick = { onSelect(style) },
                shape = SegmentedButtonDefaults.itemShape(index, styles.size),
            ) {
                Text(if (style == DesignStyle.CLASSIC) "Classic" else "Modern")
            }
        }
    }
}

/**
 * Preview parameters never inherit the global DesignStyle or the other style's selection.
 * Production and previews use the same TachiyomiTheme resolver.
 */
internal data class AppearancePreviewSelection(
    val designStyle: DesignStyle,
    val appTheme: AppTheme,
    val modernPreset: ModernThemePreset,
) {
    companion object {
        fun classic(theme: AppTheme) = AppearancePreviewSelection(
            DesignStyle.CLASSIC,
            theme,
            ModernThemePreset.LALAPO,
        )

        fun modern(preset: ModernThemePreset) = AppearancePreviewSelection(
            DesignStyle.MODERN,
            AppTheme.DEFAULT,
            preset,
        )
    }
}

@Composable
private fun ModernThemePreferenceWidget(
    value: ModernThemePreset,
    amoled: Boolean,
    onItemClick: (ModernThemePreset) -> Unit,
) {
    val authored = remember {
        ModernThemePreset.entries.filter {
            it == ModernThemePreset.CUSTOM || ModernThemeDefinitions.find(it) != null
        }
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = PrefsHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(authored, key = { it.name }) { preset ->
            val preview = AppearancePreviewSelection.modern(preset)
            Column(modifier = Modifier.width(114.dp).padding(top = 8.dp)) {
                TachiyomiTheme(
                    appTheme = preview.appTheme,
                    designStyle = preview.designStyle,
                    modernThemePreset = preview.modernPreset,
                    amoled = amoled,
                ) {
                    AppThemePreviewItem(
                        selected = value == preset,
                        onClick = { onItemClick(preset) },
                    )
                }
                Text(
                    text = preset.name.lowercase().replaceFirstChar { it.uppercase() },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
