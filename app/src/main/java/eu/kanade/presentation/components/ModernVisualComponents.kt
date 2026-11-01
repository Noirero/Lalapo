package eu.kanade.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.LocalModernVisualPalette

/**
 * Material-first presentation primitives for the optional Modern appearance.
 *
 * Material components remain responsible for interaction semantics, indication, focus, enabled
 * state, touch targets, and ordinary semantic colors. These helpers only add decorative roles.
 */
@Composable
internal fun Modifier.modernSurfaceTreatment(
    shape: Shape,
    selected: Boolean = false,
    drawBorder: Boolean = true,
): Modifier {
    val palette = LocalModernVisualPalette.current
    if (!palette.isModern) return this

    val brush = if (selected) {
        Brush.horizontalGradient(
            listOf(
                palette.accentGradientStart,
                palette.accentGradientMiddle,
                palette.accentGradientEnd,
            ),
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                palette.surfaceGradientStart.copy(alpha = palette.glassSurfaceAlpha),
                palette.surfaceGradientMiddle.copy(alpha = palette.glassSurfaceAlpha),
                palette.surfaceGradientEnd.copy(alpha = palette.glassSurfaceAlpha),
            ),
        )
    }

    return this
        .background(brush = brush, shape = shape)
        .then(
            if (drawBorder) {
                Modifier.border(
                    width = 1.dp,
                    color = if (selected) palette.glow else palette.borderHighlight,
                    shape = shape,
                )
            } else {
                Modifier
            },
        )
}

@Composable
internal fun Modifier.modernBorderHighlight(
    shape: Shape,
    selected: Boolean = false,
): Modifier {
    val palette = LocalModernVisualPalette.current
    if (!palette.isModern) return this

    return border(
        width = 1.dp,
        color = if (selected) palette.glow else palette.borderHighlight,
        shape = shape,
    )
}

@Composable
internal fun Modifier.modernCoverTreatment(
    shape: Shape,
): Modifier {
    val palette = LocalModernVisualPalette.current
    if (!palette.isModern) return this

    return clip(shape)
        .border(
            width = 1.dp,
            color = palette.borderHighlight,
            shape = shape,
        )
}

@Composable
internal fun ModernSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    selected: Boolean = false,
    tonalElevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = LocalModernVisualPalette.current
    val fallbackColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    Surface(
        modifier = modifier.then(
            if (palette.isModern) {
                Modifier.modernSurfaceTreatment(
                    shape = shape,
                    selected = selected,
                )
            } else {
                Modifier
            },
        ),
        shape = shape,
        color = if (palette.isModern) Color.Transparent else fallbackColor,
        contentColor = if (selected && palette.isModern) {
            palette.accentGradientContentColor
        } else if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        tonalElevation = tonalElevation,
    ) {
        Box(content = content)
    }
}

@Composable
internal fun ModernPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    content: @Composable RowScope.() -> Unit,
) {
    val palette = LocalModernVisualPalette.current
    if (!palette.isModern || !enabled) {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            content = content,
        )
        return
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        palette.accentGradientStart,
                        palette.accentGradientMiddle,
                        palette.accentGradientEnd,
                    ),
                ),
                shape = shape,
            )
            .modernBorderHighlight(shape = shape, selected = true),
        enabled = true,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = palette.accentGradientContentColor,
        ),
        content = content,
    )
}

@Composable
internal fun ModernMetadataChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.extraLarge,
) {
    SuggestionChip(
        onClick = onClick,
        label = label,
        modifier = modifier.then(
            if (enabled) {
                Modifier.modernBorderHighlight(
                    shape = shape,
                    selected = selected,
                )
            } else {
                Modifier
            },
        ),
        enabled = enabled,
        shape = shape,
    )
}

@Composable
internal fun SingleChoiceSegmentedButtonRowScope.ModernSegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable () -> Unit,
) {
    SegmentedButton(
        selected = selected,
        onClick = onClick,
        shape = shape,
        modifier = modifier.then(
            if (enabled && selected) {
                Modifier.modernBorderHighlight(
                    shape = shape,
                    selected = true,
                )
            } else {
                Modifier
            },
        ),
        enabled = enabled,
        label = label,
    )
}

@Composable
internal fun ModernHeroBackground(
    backgroundContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = LocalModernVisualPalette.current

    Box(modifier = modifier) {
        backgroundContent()

        if (palette.isModern) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                palette.backgroundGradientStart.copy(alpha = 0.18f),
                                palette.backgroundGradientMiddle.copy(alpha = 0.30f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                            ),
                        ),
                    ),
            )
        }

        content()
    }
}
