package eu.kanade.presentation.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.DesignStyle
import eu.kanade.domain.ui.model.ModernThemePreset
import eu.kanade.presentation.theme.colorscheme.BaseColorScheme
import eu.kanade.presentation.theme.colorscheme.CatppuccinColorScheme
import eu.kanade.presentation.theme.colorscheme.CloudflareColorScheme
import eu.kanade.presentation.theme.colorscheme.CottoncandyColorScheme
import eu.kanade.presentation.theme.colorscheme.DoomColorScheme
import eu.kanade.presentation.theme.colorscheme.GreenAppleColorScheme
import eu.kanade.presentation.theme.colorscheme.LavenderColorScheme
import eu.kanade.presentation.theme.colorscheme.MatrixColorScheme
import eu.kanade.presentation.theme.colorscheme.MidnightDuskColorScheme
import eu.kanade.presentation.theme.colorscheme.MochaColorScheme
import eu.kanade.presentation.theme.colorscheme.MonetColorScheme
import eu.kanade.presentation.theme.colorscheme.MonochromeColorScheme
import eu.kanade.presentation.theme.colorscheme.NordColorScheme
import eu.kanade.presentation.theme.colorscheme.SapphireColorScheme
import eu.kanade.presentation.theme.colorscheme.StrawberryColorScheme
import eu.kanade.presentation.theme.colorscheme.TachiyomiColorScheme
import eu.kanade.presentation.theme.colorscheme.TakoColorScheme
import eu.kanade.presentation.theme.colorscheme.TealTurqoiseColorScheme
import eu.kanade.presentation.theme.colorscheme.TidalWaveColorScheme
import eu.kanade.presentation.theme.colorscheme.TokyoNightColorScheme
import eu.kanade.presentation.theme.colorscheme.YinYangColorScheme
import eu.kanade.presentation.theme.colorscheme.YotsubaColorScheme
import mihon.app.di.appGraph

@Composable
fun TachiyomiTheme(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    designStyle: DesignStyle? = null,
    modernThemePreset: ModernThemePreset? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val uiPreferences = remember { context.appGraph.uiPreferences }
    BaseTachiyomiTheme(
        appTheme = appTheme ?: uiPreferences.appTheme.get(),
        isAmoled = amoled ?: uiPreferences.themeDarkAmoled.get(),
        designStyle = designStyle ?: uiPreferences.designStyle.get(),
        modernThemePreset = modernThemePreset ?: uiPreferences.modernThemePreset.get(),
        content = content,
    )
}

@Composable
fun TachiyomiPreviewTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    isAmoled: Boolean = false,
    designStyle: DesignStyle = DesignStyle.CLASSIC,
    modernThemePreset: ModernThemePreset = ModernThemePreset.LALAPO,
    content: @Composable () -> Unit,
) = BaseTachiyomiTheme(appTheme, isAmoled, designStyle, modernThemePreset, content)

@Composable
private fun BaseTachiyomiTheme(
    appTheme: AppTheme,
    isAmoled: Boolean,
    designStyle: DesignStyle,
    modernThemePreset: ModernThemePreset,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val customColors = if (designStyle == DesignStyle.MODERN && modernThemePreset == ModernThemePreset.CUSTOM) {
        val preferences = remember(context) { context.appGraph.uiPreferences }
        remember(
            preferences.modernCustomPrimary.get(),
            preferences.modernCustomSecondaryOverride.get(),
            preferences.modernCustomTertiaryOverride.get(),
        ) {
            ModernCustomColors.fromStored(
                preferences.modernCustomPrimary.get(),
                preferences.modernCustomSecondaryOverride.get(),
                preferences.modernCustomTertiaryOverride.get(),
            )
        }
    } else {
        null
    }
    val colorScheme = remember(context, appTheme, isDark, isAmoled, designStyle, modernThemePreset, customColors) {
        resolveDesignStyleColorScheme(
            designStyle = designStyle,
            modernThemePreset = modernThemePreset,
            isDark = isDark,
            isAmoled = isAmoled,
            customColors = customColors,
            classicResolver = {
                getThemeColorScheme(
                    context = context,
                    appTheme = appTheme,
                    isDark = isDark,
                    isAmoled = isAmoled,
                )
            },
        )
    }
    val modernDefinition = remember(designStyle, modernThemePreset, customColors) {
        if (designStyle == DesignStyle.MODERN) {
            if (modernThemePreset == ModernThemePreset.CUSTOM) {
                ModernThemeDefinitions.custom(requireNotNull(customColors))
            } else {
                requireNotNull(ModernThemeDefinitions.find(modernThemePreset)) {
                    "Modern preset $modernThemePreset has no authored production definition"
                }
            }
        } else {
            null
        }
    }
    val modernVisualPalette = remember(colorScheme, modernDefinition, isDark, isAmoled) {
        resolveModernVisualPalette(
            colorScheme = colorScheme,
            enabled = modernDefinition != null,
            definition = modernDefinition,
            isAmoled = isDark && isAmoled,
        )
    }

    CompositionLocalProvider(
        LocalModernVisualPalette provides modernVisualPalette,
        LocalModernArtworkIdentity provides modernDefinition?.artworkIdentity,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

private fun getThemeColorScheme(
    context: Context,
    appTheme: AppTheme,
    isDark: Boolean,
    isAmoled: Boolean,
): ColorScheme {
    val colorScheme = if (appTheme == AppTheme.MONET) {
        MonetColorScheme(context)
    } else {
        colorSchemes.getOrDefault(appTheme, TachiyomiColorScheme)
    }
    return colorScheme.getColorScheme(
        isDark = isDark,
        isAmoled = isAmoled,
        overrideDarkSurfaceContainers = appTheme != AppTheme.MONET,
    )
}

// AY -->
private const val RIPPLE_DRAGGED_ALPHA = .1f
private const val RIPPLE_FOCUSED_ALPHA = .1f
private const val RIPPLE_HOVERED_ALPHA = .1f
private const val RIPPLE_PRESSED_ALPHA = .1f

val playerRippleConfiguration
    @Composable get() = RippleConfiguration(
        color = if (isSystemInDarkTheme()) Color.White else Color.Black,
        rippleAlpha = RippleAlpha(
            draggedAlpha = RIPPLE_DRAGGED_ALPHA,
            focusedAlpha = RIPPLE_FOCUSED_ALPHA,
            hoveredAlpha = RIPPLE_HOVERED_ALPHA,
            pressedAlpha = RIPPLE_PRESSED_ALPHA,
        ),
    )
// <-- AY

private val colorSchemes: Map<AppTheme, BaseColorScheme> = mapOf(
    AppTheme.DEFAULT to TachiyomiColorScheme,
    AppTheme.CATPPUCCIN to CatppuccinColorScheme,
    AppTheme.TOKYONIGHT to TokyoNightColorScheme,
    // AY -->
    AppTheme.CLOUDFLARE to CloudflareColorScheme,
    AppTheme.COTTONCANDY to CottoncandyColorScheme,
    AppTheme.DOOM to DoomColorScheme,
    // <-- AY
    AppTheme.GREEN_APPLE to GreenAppleColorScheme,
    AppTheme.LAVENDER to LavenderColorScheme,
    // AY -->
    AppTheme.MATRIX to MatrixColorScheme,
    // <-- AY
    AppTheme.MIDNIGHT_DUSK to MidnightDuskColorScheme,
    // AY -->
    AppTheme.MOCHA to MochaColorScheme,
    // <-- AY
    AppTheme.MONOCHROME to MonochromeColorScheme,
    AppTheme.NORD to NordColorScheme,
    // AY -->
    AppTheme.SAPPHIRE to SapphireColorScheme,
    // <-- AY
    AppTheme.STRAWBERRY_DAIQUIRI to StrawberryColorScheme,
    AppTheme.TAKO to TakoColorScheme,
    AppTheme.TEALTURQUOISE to TealTurqoiseColorScheme,
    AppTheme.TIDAL_WAVE to TidalWaveColorScheme,
    AppTheme.YINYANG to YinYangColorScheme,
    AppTheme.YOTSUBA to YotsubaColorScheme,
)
