package eu.kanade.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Pustaka chrome authored for the six owner reference images.
 *
 * This is deliberately separate from Material semantics and the wallpaper. Only the Library
 * and its shared bottom navigation opt in; Classic and user-defined Custom do not.
 */
@Immutable
internal data class ModernLibraryChrome(
    val surfaceStart: Color,
    val surfaceEnd: Color,
    val border: Color,
    val borderGlow: Color,
    val selectedStart: Color,
    val selectedEnd: Color,
    val selectedBorder: Color,
    val content: Color,
    val contentMuted: Color,
    val selectedContent: Color,
    val titleColor: Color,
    val headerSubtitleColor: Color,
    val ornament: String,
    val serifTitle: Boolean,
    val controlShape: Shape,
    val segmentedShape: Shape,
    val tabShape: Shape,
    val actionShape: Shape,
    val navShape: Shape,
    val navSelectedShape: Shape,
    val cardShape: Shape,
    val cardAspectRatio: Float,
    val setSelectorWeight: Float,
)

private val roundControl = RoundedCornerShape(24.dp)
private val roundSegment = RoundedCornerShape(28.dp)
private val roundTab = RoundedCornerShape(24.dp)
private val roundNav = RoundedCornerShape(32.dp)
private val softAction = CutCornerShape(topStart = 6.dp, topEnd = 12.dp, bottomEnd = 6.dp, bottomStart = 12.dp)
private val fluidShape = RoundedCornerShape(topStart = 28.dp, topEnd = 18.dp, bottomEnd = 28.dp, bottomStart = 18.dp)
private val cyberShape = CutCornerShape(topStart = 10.dp, topEnd = 4.dp, bottomEnd = 10.dp, bottomStart = 4.dp)
private val angularAction = CutCornerShape(topStart = 12.dp, topEnd = 3.dp, bottomEnd = 12.dp, bottomStart = 3.dp)
private val ornamentalAction = CutCornerShape(topStart = 7.dp, topEnd = 14.dp, bottomEnd = 7.dp, bottomStart = 14.dp)

private object Presets {
    val lalapo = ModernLibraryChrome(
        surfaceStart = Color(0xB208173C),
        surfaceEnd = Color(0xB4082F62),
        border = Color(0xFF38BDFF),
        borderGlow = Color(0x9932B5FF),
        selectedStart = Color(0xFF086CF4),
        selectedEnd = Color(0xFF03D9F5),
        selectedBorder = Color(0xFF83F4FF),
        content = Color.White,
        contentMuted = Color(0xFFE4EDFF),
        selectedContent = Color.White,
        titleColor = Color.White,
        ornament = "//",
        headerSubtitleColor = Color(0xFFE4EDFF),
        serifTitle = true,
        controlShape = roundControl,
        segmentedShape = roundSegment,
        tabShape = roundTab,
        actionShape = RoundedCornerShape(24.dp),
        navShape = roundNav,
        navSelectedShape = RoundedCornerShape(28.dp),
        cardShape = RoundedCornerShape(17.dp),
        cardAspectRatio = ModernLibraryGeometry.COVER_ASPECT_RATIO,
        setSelectorWeight = 0.38f,
    )

    val sakura = ModernLibraryChrome(
        surfaceStart = Color(0xE4FBE3F0),
        surfaceEnd = Color(0xE7FFD5E7),
        border = Color(0xFFCE5D96),
        borderGlow = Color(0x77FD90C3),
        selectedStart = Color(0xFF991451),
        selectedEnd = Color(0xFFD5397F),
        selectedBorder = Color(0xFFFFB4D7),
        content = Color(0xFF51152E),
        contentMuted = Color(0xFF6E2846),
        selectedContent = Color.White,
        titleColor = Color(0xFF9C2257),
        ornament = "✿",
        headerSubtitleColor = Color(0xFF51152E),
        serifTitle = true,
        controlShape = RoundedCornerShape(28.dp),
        segmentedShape = RoundedCornerShape(30.dp),
        tabShape = RoundedCornerShape(23.dp),
        actionShape = softAction,
        navShape = RoundedCornerShape(34.dp),
        navSelectedShape = RoundedCornerShape(29.dp),
        cardShape = RoundedCornerShape(20.dp),
        cardAspectRatio = ModernLibraryGeometry.COVER_ASPECT_RATIO,
        setSelectorWeight = 0.34f,
    )

    val violet = ModernLibraryChrome(
        surfaceStart = Color(0xB20E092F),
        surfaceEnd = Color(0xB4310C59),
        border = Color(0xFFE25BFF),
        borderGlow = Color(0x99AC2AFF),
        selectedStart = Color(0xFF9B0AF5),
        selectedEnd = Color(0xFFFA16E5),
        selectedBorder = Color(0xFFFF81F4),
        content = Color.White,
        contentMuted = Color(0xFFEAE0FF),
        selectedContent = Color.White,
        titleColor = Color.White,
        ornament = "///",
        headerSubtitleColor = Color(0xFFEAE0FF),
        serifTitle = false,
        controlShape = RoundedCornerShape(20.dp),
        segmentedShape = cyberShape,
        tabShape = RoundedCornerShape(17.dp),
        actionShape = angularAction,
        navShape = RoundedCornerShape(26.dp),
        navSelectedShape = RoundedCornerShape(28.dp),
        cardShape = RoundedCornerShape(15.dp),
        cardAspectRatio = ModernLibraryGeometry.COVER_ASPECT_RATIO,
        setSelectorWeight = 0.39f,
    )

    val cyan = ModernLibraryChrome(
        surfaceStart = Color(0xAF05283B),
        surfaceEnd = Color(0xC0095973),
        border = Color(0xFF3CE9FD),
        borderGlow = Color(0x9932E9FF),
        selectedStart = Color(0xFF05BFD8),
        selectedEnd = Color(0xFF6BFFF9),
        selectedBorder = Color(0xFFADFFFF),
        content = Color.White,
        contentMuted = Color(0xFFE1FDFF),
        selectedContent = Color(0xFF002E43),
        titleColor = Color.White,
        ornament = "〰",
        headerSubtitleColor = Color(0xFFE1FDFF),
        serifTitle = true,
        controlShape = RoundedCornerShape(27.dp),
        segmentedShape = RoundedCornerShape(32.dp),
        tabShape = RoundedCornerShape(27.dp),
        actionShape = fluidShape,
        navShape = RoundedCornerShape(38.dp),
        navSelectedShape = RoundedCornerShape(32.dp),
        cardShape = RoundedCornerShape(19.dp),
        cardAspectRatio = ModernLibraryGeometry.COVER_ASPECT_RATIO,
        setSelectorWeight = 0.39f,
    )

    val emerald = ModernLibraryChrome(
        surfaceStart = Color(0xAD052C1C),
        surfaceEnd = Color(0xC00C4931),
        border = Color(0xFFABF567),
        borderGlow = Color(0x8885E748),
        selectedStart = Color(0xFF80D62F),
        selectedEnd = Color(0xFFD2FF81),
        selectedBorder = Color(0xFFE6FFA7),
        content = Color.White,
        contentMuted = Color(0xFFF0FFE4),
        selectedContent = Color(0xFF183B15),
        titleColor = Color.White,
        ornament = "❧",
        headerSubtitleColor = Color(0xFFF0FFE4),
        serifTitle = true,
        controlShape = RoundedCornerShape(22.dp),
        segmentedShape = fluidShape,
        tabShape = RoundedCornerShape(topStart = 25.dp, bottomEnd = 25.dp, topEnd = 14.dp, bottomStart = 14.dp),
        actionShape = fluidShape,
        navShape = fluidShape,
        navSelectedShape = fluidShape,
        cardShape = RoundedCornerShape(19.dp),
        cardAspectRatio = ModernLibraryGeometry.COVER_ASPECT_RATIO,
        setSelectorWeight = 0.37f,
    )

    val amber = ModernLibraryChrome(
        surfaceStart = Color(0xBE1D1108),
        surfaceEnd = Color(0xD03A230C),
        border = Color(0xFFFFC34D),
        borderGlow = Color(0x99FFAE22),
        selectedStart = Color(0xFFFFAA18),
        selectedEnd = Color(0xFFFFE46B),
        selectedBorder = Color(0xFFFFED9E),
        content = Color.White,
        contentMuted = Color(0xFFFFE9C9),
        selectedContent = Color(0xFF2C1B04),
        titleColor = Color(0xFFFFD383),
        ornament = "//",
        headerSubtitleColor = Color(0xFFFFE9C9),
        serifTitle = true,
        controlShape = RoundedCornerShape(29.dp),
        segmentedShape = RoundedCornerShape(31.dp),
        tabShape = RoundedCornerShape(28.dp),
        actionShape = ornamentalAction,
        navShape = RoundedCornerShape(38.dp),
        navSelectedShape = RoundedCornerShape(31.dp),
        cardShape = RoundedCornerShape(19.dp),
        cardAspectRatio = ModernLibraryGeometry.COVER_ASPECT_RATIO,
        setSelectorWeight = 0.40f,
    )
}

/**
 * The authored Sakura, Cyan and Emerald wallpaper headers are darker than their semantic
 * Material surfaces. Use light status-bar glyphs only while their Library artwork is visible.
 */
internal fun modernLibraryNeedsLightStatusBarIcons(
    palette: ModernVisualPalette,
    identity: ModernArtworkIdentity?,
): Boolean = palette.isModern && when (identity) {
    ModernArtworkIdentity.SAKURA_BLOSSOM,
    ModernArtworkIdentity.CYAN_WATER_SKY,
    ModernArtworkIdentity.EMERALD_FOLIAGE,
    -> true
    else -> false
}

internal fun resolveModernLibraryChrome(
    palette: ModernVisualPalette,
    identity: ModernArtworkIdentity?,
): ModernLibraryChrome? {
    if (!palette.isModern) return null
    return when (identity) {
        ModernArtworkIdentity.LALAPO_AURORA -> Presets.lalapo
        ModernArtworkIdentity.SAKURA_BLOSSOM -> Presets.sakura
        ModernArtworkIdentity.VIOLET_MOON_AURORA -> Presets.violet
        ModernArtworkIdentity.CYAN_WATER_SKY -> Presets.cyan
        ModernArtworkIdentity.EMERALD_FOLIAGE -> Presets.emerald
        ModernArtworkIdentity.AMBER_MAPLE_SUNSET -> Presets.amber
        ModernArtworkIdentity.CUSTOM_WAVES, null -> null
    }
}

/**
 * Reserve readable space for the Set label on narrow viewports without widening it
 * unnecessarily on 9:16 references. Category tabs retain the remaining scrollable width.
 */
internal fun modernLibrarySetSelectorWidth(availableWidth: Dp, referenceFraction: Float): Dp =
    maxOf(availableWidth * referenceFraction, minOf(184.dp, availableWidth * 0.52f))

/** Inexpensive gradients and strokes; no per-item blur, shadow layers, or animation. */
internal fun Modifier.modernLibraryChromeSurface(
    chrome: ModernLibraryChrome,
    shape: Shape,
    selected: Boolean = false,
    emphasized: Boolean = false,
): Modifier = this
    .background(
        brush = Brush.horizontalGradient(
            if (selected) {
                listOf(chrome.selectedStart, chrome.selectedEnd)
            } else {
                listOf(chrome.surfaceStart, chrome.surfaceEnd)
            },
        ),
        shape = shape,
    )
    .border(
        width = 2.dp,
        color = chrome.borderGlow.copy(alpha = if (selected || emphasized) 0.75f else 0.40f),
        shape = shape,
    )
    .border(
        width = 1.dp,
        color = if (selected || emphasized) chrome.selectedBorder else chrome.border,
        shape = shape,
    )

/**
 * Draw a highlight *on the actual button outline*, including cut corners.
 * The old free-standing corner strokes were visibly detached from the reference silhouette.
 */
internal fun Modifier.modernLibraryActionSurface(
    chrome: ModernLibraryChrome,
    selected: Boolean,
): Modifier = this
    .modernLibraryChromeSurface(chrome, chrome.actionShape, selected = selected)
    .drawWithCache {
        val outline = chrome.actionShape.createOutline(size, layoutDirection, this)
        val path = when (outline) {
            is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
            is Outline.Generic -> outline.path
        }
        val highlight = Brush.linearGradient(
            colors = listOf(chrome.selectedBorder, chrome.borderGlow, chrome.selectedBorder),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        )
        onDrawWithContent {
            drawContent()
            drawPath(path, highlight, style = Stroke(width = 1.5.dp.toPx()))
        }
    }

internal fun Modifier.modernLibraryChromeCover(
    chrome: ModernLibraryChrome,
    shape: Shape,
): Modifier = this
    .clip(shape)
    .border(width = 2.dp, color = chrome.borderGlow.copy(alpha = 0.48f), shape = shape)
    .border(width = 1.dp, color = chrome.border, shape = shape)
