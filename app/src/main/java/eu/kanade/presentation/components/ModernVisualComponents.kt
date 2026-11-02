package eu.kanade.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
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
import eu.kanade.presentation.theme.LocalCinematicAccentPalette
import eu.kanade.presentation.theme.LocalModernArtworkIdentity
import eu.kanade.presentation.theme.LocalModernVisualPalette
import eu.kanade.presentation.theme.ModernArtworkIdentity
import eu.kanade.presentation.theme.resolveAccentPalette
import eu.kanade.presentation.theme.resolveGlassPalette

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

    if (LocalModernArtworkIdentity.current == ModernArtworkIdentity.CUSTOM_WAVES) {
        val accent = palette.resolveAccentPalette(LocalCinematicAccentPalette.current)
        val brush = if (selected) {
            Brush.horizontalGradient(listOf(accent.activeGradientStart, accent.activeGradientEnd))
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
                        color = if (selected) accent.glow else accent.borderHighlight,
                        shape = shape,
                    )
                } else {
                    Modifier
                },
            )
    }

    val glass = palette.resolveGlassPalette(MaterialTheme.colorScheme)
    val brush = if (selected) {
        Brush.horizontalGradient(
            listOf(
                glass.selectedSurface,
                glass.innerHighlight.copy(alpha = glass.innerHighlight.alpha * 0.58f),
                glass.selectedSurface,
            ),
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                glass.surface,
                glass.surfaceStrong,
                glass.surface,
            ),
        )
    }

    return this
        .background(brush = brush, shape = shape)
        .then(
            if (drawBorder) {
                Modifier
                    .border(
                        width = 2.dp,
                        color = if (selected) {
                            glass.selectedGlow.copy(alpha = glass.selectedGlow.alpha * 0.30f)
                        } else {
                            glass.glow.copy(
                                alpha =
                                glass.glow.alpha * 0.22f,
                            )
                        },
                        shape = shape,
                    )
                    .border(
                        width = 1.dp,
                        color = if (selected) glass.selectedBorder else glass.borderStrong,
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
    emphasized: Boolean = false,
): Modifier {
    val palette = LocalModernVisualPalette.current
    if (!palette.isModern) return this

    if (LocalModernArtworkIdentity.current == ModernArtworkIdentity.CUSTOM_WAVES) {
        val accent = palette.resolveAccentPalette(LocalCinematicAccentPalette.current)
        return border(
            width = 1.dp,
            color = if (emphasized) accent.glow else accent.borderHighlight,
            shape = shape,
        )
    }

    val glass = palette.resolveGlassPalette(MaterialTheme.colorScheme)
    return border(
        width = 1.dp,
        color = if (emphasized) glass.selectedBorder else glass.borderStrong,
        shape = shape,
    )
}

@Composable
internal fun Modifier.modernCoverTreatment(
    shape: Shape,
): Modifier {
    val palette = LocalModernVisualPalette.current
    if (!palette.isModern) return this

    if (LocalModernArtworkIdentity.current == ModernArtworkIdentity.CUSTOM_WAVES) {
        val accent = palette.resolveAccentPalette(LocalCinematicAccentPalette.current)
        return clip(shape).border(width = 1.dp, color = accent.borderHighlight, shape = shape)
    }

    val glass = palette.resolveGlassPalette(MaterialTheme.colorScheme)
    return clip(shape)
        .border(
            width = 2.dp,
            color = glass.cardGlow.copy(alpha = glass.cardGlow.alpha * 0.42f),
            shape = shape,
        )
        .border(
            width = 1.dp,
            color = glass.borderStrong,
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
    val accent = palette.resolveAccentPalette(LocalCinematicAccentPalette.current)
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
            accent.accentGradientContentColor
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

    val accent = palette.resolveAccentPalette(LocalCinematicAccentPalette.current)
    Button(
        onClick = onClick,
        modifier = modifier
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        accent.accentGradientStart,
                        accent.accentGradientMiddle,
                        accent.accentGradientEnd,
                    ),
                ),
                shape = shape,
            )
            .modernBorderHighlight(
                shape = shape,
                emphasized = true,
            ),
        enabled = true,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = accent.accentGradientContentColor,
        ),
        content = content,
    )
}

@Composable
internal fun ModernTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    content: @Composable RowScope.() -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.then(
            if (enabled) {
                Modifier.modernBorderHighlight(shape = shape)
            } else {
                Modifier
            },
        ),
        enabled = enabled,
        shape = shape,
        content = content,
    )
}

@Composable
internal fun ModernMetadataChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
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
                    emphasized = emphasized,
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
    val palette = LocalModernVisualPalette.current
    val accent = palette.resolveAccentPalette(LocalCinematicAccentPalette.current)
    val useModernSelectedTreatment = palette.isModern && enabled && selected

    SegmentedButton(
        selected = selected,
        onClick = onClick,
        shape = shape,
        modifier = modifier.then(
            if (useModernSelectedTreatment) {
                Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                accent.activeGradientStart,
                                accent.activeGradientEnd,
                            ),
                        ),
                        shape = shape,
                    )
                    .modernBorderHighlight(
                        shape = shape,
                        emphasized = true,
                    )
            } else {
                Modifier
            },
        ),
        enabled = enabled,
        colors = if (useModernSelectedTreatment) {
            SegmentedButtonDefaults.colors(
                activeContainerColor = Color.Transparent,
                activeContentColor = accent.accentGradientContentColor,
                activeBorderColor = Color.Transparent,
            )
        } else {
            SegmentedButtonDefaults.colors()
        },
        label = label,
    )
}

@Composable
internal fun ModernAccentDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
) {
    val palette = LocalModernVisualPalette.current
    if (!palette.isModern) {
        HorizontalDivider(
            modifier = modifier,
            thickness = thickness,
        )
        return
    }

    val accent = palette.resolveAccentPalette(LocalCinematicAccentPalette.current)
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = accent.borderHighlight,
    )
}

@Composable
internal fun ModernHeroBackground(
    backgroundContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = LocalModernVisualPalette.current
    val cinematicAccent = LocalCinematicAccentPalette.current

    Box(modifier = modifier) {
        backgroundContent()

        if (palette.isModern) {
            val gradientStart = cinematicAccent?.accentGradientStart ?: palette.backgroundGradientStart
            val gradientMiddle = cinematicAccent?.accentGradientMiddle ?: palette.backgroundGradientMiddle
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                gradientStart.copy(alpha = 0.18f),
                                gradientMiddle.copy(alpha = 0.30f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                            ),
                        ),
                    ),
            )
        }

        content()
    }
}
