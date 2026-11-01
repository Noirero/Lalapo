package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.theme.CustomHsv
import eu.kanade.presentation.theme.DEFAULT_CUSTOM_PRIMARY
import eu.kanade.presentation.theme.ModernCustomColors
import eu.kanade.presentation.theme.ModernCustomDraft
import eu.kanade.presentation.theme.customColorHex
import eu.kanade.presentation.theme.customFromHsv
import eu.kanade.presentation.theme.customHexColor
import eu.kanade.presentation.theme.customToHsv
import tachiyomi.core.common.preference.Preference
import tachiyomi.presentation.core.util.collectAsState

/**
 * Explicit transaction: sliders and invalid HEX are local draft only.
 * Only a fully validated Apply writes preferences, once per field.
 */
internal fun applyCustomColorDraft(
    draft: ModernCustomDraft,
    primary: Preference<String>,
    secondary: Preference<String>,
    tertiary: Preference<String>,
): Boolean {
    val normalized = draft.normalized() ?: return false
    primary.set(normalized.primary)
    secondary.set(normalized.secondaryOverride.orEmpty())
    tertiary.set(normalized.tertiaryOverride.orEmpty())
    return true
}

@Composable
internal fun CustomModernColorEditor(
    uiPreferences: UiPreferences,
    onApplied: () -> Unit,
) {
    val primary by uiPreferences.modernCustomPrimary.collectAsState()
    val secondary by uiPreferences.modernCustomSecondaryOverride.collectAsState()
    val tertiary by uiPreferences.modernCustomTertiaryOverride.collectAsState()
    var editing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = PrefsHorizontalPadding, vertical = 8.dp)) {
        val resolved = remember(primary, secondary, tertiary) {
            ModernCustomColors.fromStored(primary, secondary, tertiary)
        }
        Text(
            text = "Primary: ${customColorHex(resolved.primary)} · Secondary/Tertiary: AUTO or override",
            style = MaterialTheme.typography.bodySmall,
        )
        Button(onClick = { editing = true }) {
            Text("Customize colors")
        }
    }

    if (editing) {
        val initial = ModernCustomDraft(
            primary = primary,
            secondaryOverride = secondary.takeIf { it.isNotBlank() },
            tertiaryOverride = tertiary.takeIf { it.isNotBlank() },
        )
        CustomModernColorDialog(
            initial = initial,
            onDismiss = { editing = false },
            onApply = { draft ->
                if (applyCustomColorDraft(
                        draft,
                        uiPreferences.modernCustomPrimary,
                        uiPreferences.modernCustomSecondaryOverride,
                        uiPreferences.modernCustomTertiaryOverride,
                    )
                ) {
                    editing = false
                    onApplied()
                }
            },
        )
    }
}

private enum class ColorRole { PRIMARY, SECONDARY, TERTIARY }

@Composable
private fun CustomModernColorDialog(
    initial: ModernCustomDraft,
    onDismiss: () -> Unit,
    onApply: (ModernCustomDraft) -> Unit,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    var advanced by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf(ColorRole.PRIMARY) }
    val derived = draft.colors() ?: ModernCustomColors.fromStored(DEFAULT_CUSTOM_PRIMARY, "", "")
    val editText = when (role) {
        ColorRole.PRIMARY -> draft.primary
        ColorRole.SECONDARY -> draft.secondaryOverride ?: customColorHex(derived.secondary)
        ColorRole.TERTIARY -> draft.tertiaryOverride ?: customColorHex(derived.tertiary)
    }
    val onColorChange: (String) -> Unit = { value ->
        draft = when (role) {
            ColorRole.PRIMARY -> draft.copy(primary = value)
            ColorRole.SECONDARY -> draft.copy(secondaryOverride = value)
            ColorRole.TERTIARY -> draft.copy(tertiaryOverride = value)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Modern colors") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Choose Primary; Secondary and Tertiary are generated automatically.")
                TextButton(onClick = { role = ColorRole.PRIMARY }) {
                    Text("Primary: ${draft.primary}")
                }
                TextButton(onClick = { advanced = !advanced }) {
                    Text(if (advanced) "Hide advanced colors" else "Advanced · Secondary / Tertiary")
                }
                if (advanced) {
                    ColorOverrideRow(
                        title = "Secondary",
                        value = draft.secondaryOverride,
                        automatic = customColorHex(derived.secondary),
                        onEdit = {
                            draft =
                                draft.copy(
                                    secondaryOverride =
                                        draft.secondaryOverride ?: customColorHex(derived.secondary),
                                )
                            role = ColorRole.SECONDARY
                        },
                        onAuto = {
                            draft = draft.copy(secondaryOverride = null)
                            if (role == ColorRole.SECONDARY) role = ColorRole.PRIMARY
                        },
                    )
                    ColorOverrideRow(
                        title = "Tertiary",
                        value = draft.tertiaryOverride,
                        automatic = customColorHex(derived.tertiary),
                        onEdit = {
                            draft =
                                draft.copy(
                                    tertiaryOverride = draft.tertiaryOverride ?: customColorHex(derived.tertiary),
                                )
                            role = ColorRole.TERTIARY
                        },
                        onAuto = {
                            draft = draft.copy(tertiaryOverride = null)
                            if (role == ColorRole.TERTIARY) role = ColorRole.PRIMARY
                        },
                    )
                }
                Text("Editing: ${role.name.lowercase().replaceFirstChar { it.uppercase() }}")
                CustomHexHsvField(
                    value = editText,
                    onChange = onColorChange,
                    fallback = when (role) {
                        ColorRole.PRIMARY -> derived.primary
                        ColorRole.SECONDARY -> derived.secondary
                        ColorRole.TERTIARY -> derived.tertiary
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorSwatch("Primary", customHexColor(draft.primary) ?: derived.primary)
                    ColorSwatch("Secondary", derived.secondary)
                    ColorSwatch("Tertiary", derived.tertiary)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = draft.isValid,
                onClick = { onApply(draft) },
            ) {
                Text("Apply")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ColorOverrideRow(
    title: String,
    value: String?,
    automatic: String,
    onEdit: () -> Unit,
    onAuto: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onEdit) {
            Text("$title: ${value ?: "AUTO ($automatic)"}")
        }
        if (value != null) {
            TextButton(onClick = onAuto) { Text("AUTO") }
        }
    }
}

@Composable
private fun CustomHexHsvField(
    value: String,
    onChange: (String) -> Unit,
    fallback: Color,
) {
    val parsed = customHexColor(value)
    val hsv = customToHsv(parsed ?: fallback)
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("HEX (#RRGGBB)") },
        singleLine = true,
        isError = parsed == null,
        supportingText = if (parsed == null) {
            { Text("Enter 6 hexadecimal digits; alpha is not supported") }
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth(),
    )
    fun modify(h: Float = hsv.hue, s: Float = hsv.saturation, v: Float = hsv.value) {
        onChange(customColorHex(customFromHsv(CustomHsv(h, s, v))))
    }
    Text("Hue")
    Slider(value = hsv.hue, valueRange = 0f..359.9f, onValueChange = { modify(h = it) })
    Text("Saturation")
    Slider(value = hsv.saturation, onValueChange = { modify(s = it) })
    Text("Value")
    Slider(value = hsv.value, onValueChange = { modify(v = it) })
}

@Composable
private fun ColorSwatch(title: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(color, RoundedCornerShape(12.dp)),
        )
        Text(title, style = MaterialTheme.typography.labelSmall)
    }
}
