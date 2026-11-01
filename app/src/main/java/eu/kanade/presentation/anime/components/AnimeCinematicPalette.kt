package eu.kanade.presentation.anime.components

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import eu.kanade.presentation.theme.CinematicAccentPalette
import eu.kanade.presentation.theme.LocalModernVisualPalette
import eu.kanade.tachiyomi.data.coil.useBackground
import eu.kanade.tachiyomi.util.system.getBitmapOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.domain.anime.model.Anime
import kotlin.math.abs

private const val PALETTE_BITMAP_SIZE = 96
private const val MINIMUM_ARTWORK_SATURATION = 0.18f
private const val MINIMUM_TEXT_CONTRAST = 4.5f
private const val PALETTE_CACHE_SIZE = 48

internal data class AnimeArtworkPaletteSeeds(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
)

private data class CachedArtworkPalette(
    val seeds: AnimeArtworkPaletteSeeds?,
)

private val artworkPaletteCache = LruCache<String, CachedArtworkPalette>(PALETTE_CACHE_SIZE)

@Composable
internal fun rememberAnimeCinematicAccentPalette(
    anime: Anime,
): CinematicAccentPalette? {
    val modernEnabled = LocalModernVisualPalette.current.isModern
    if (!modernEnabled) return null

    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val cacheKey = remember(
        anime.id,
        anime.backgroundUrl,
        anime.backgroundLastModified,
        anime.thumbnailUrl,
        anime.coverLastModified,
    ) {
        animeArtworkPaletteKey(anime)
    }
    val cached = remember(cacheKey) { artworkPaletteCache.get(cacheKey) }
    val seeds by produceState<AnimeArtworkPaletteSeeds?>(
        initialValue = cached?.seeds,
        key1 = cacheKey,
    ) {
        if (cached != null) {
            value = cached.seeds
            return@produceState
        }

        val extracted = withContext(Dispatchers.IO) {
            extractArtworkPaletteSeeds(
                context = context,
                anime = anime,
            )
        }
        artworkPaletteCache.put(cacheKey, CachedArtworkPalette(extracted))
        value = extracted
    }

    return remember(seeds, colorScheme) {
        seeds?.let { deriveCinematicAccentPalette(it, colorScheme) }
    }
}

internal fun animeArtworkPaletteKey(anime: Anime): String {
    return animeArtworkPaletteKey(
        animeId = anime.id,
        backgroundUrl = anime.backgroundUrl,
        backgroundLastModified = anime.backgroundLastModified,
        thumbnailUrl = anime.thumbnailUrl,
        coverLastModified = anime.coverLastModified,
    )
}

internal fun animeArtworkPaletteKey(
    animeId: Long,
    backgroundUrl: String?,
    backgroundLastModified: Long,
    thumbnailUrl: String?,
    coverLastModified: Long,
): String {
    return buildString {
        append(animeId)
        append(';')
        append(backgroundUrl.orEmpty())
        append(';')
        append(backgroundLastModified)
        append(';')
        append(thumbnailUrl.orEmpty())
        append(';')
        append(coverLastModified)
    }
}

private suspend fun extractArtworkPaletteSeeds(
    context: Context,
    anime: Anime,
): AnimeArtworkPaletteSeeds? {
    val bitmap = loadPaletteBitmap(context, anime, useBackground = true)
        ?: loadPaletteBitmap(context, anime, useBackground = false)
        ?: return null

    return try {
        val palette = Palette.from(bitmap)
            .maximumColorCount(12)
            .generate()

        val usefulSwatches = palette.swatches
            .filter { it.hsl[1] >= MINIMUM_ARTWORK_SATURATION }
            .sortedByDescending { swatch ->
                (swatch.population.coerceAtLeast(1) * (0.35f + swatch.hsl[1])).toDouble()
            }

        val primary = palette.vibrantSwatch
            ?.takeIf { it.hsl[1] >= MINIMUM_ARTWORK_SATURATION }
            ?: usefulSwatches.firstOrNull()
            ?: return null

        val secondary = usefulSwatches.firstOrNull {
            hueDistance(primary.hsl[0], it.hsl[0]) >= 18f
        } ?: palette.mutedSwatch ?: primary

        val tertiary = usefulSwatches.firstOrNull {
            it != primary &&
                it != secondary &&
                hueDistance(primary.hsl[0], it.hsl[0]) >= 28f
        } ?: palette.darkVibrantSwatch ?: palette.lightVibrantSwatch ?: secondary

        AnimeArtworkPaletteSeeds(
            primary = Color(primary.rgb),
            secondary = Color(secondary.rgb),
            tertiary = Color(tertiary.rgb),
        )
    } finally {
        bitmap.recycle()
    }
}

private suspend fun loadPaletteBitmap(
    context: Context,
    anime: Anime,
    useBackground: Boolean,
): Bitmap? {
    val request = ImageRequest.Builder(context)
        .data(anime)
        .useBackground(useBackground)
        .size(PALETTE_BITMAP_SIZE, PALETTE_BITMAP_SIZE)
        .build()

    val drawable = runCatching {
        context.imageLoader.execute(request).image?.asDrawable(context.resources)
    }.getOrNull() ?: return null

    val source = drawable.getBitmapOrNull() ?: return null
    val scaled = if (source.width == PALETTE_BITMAP_SIZE && source.height == PALETTE_BITMAP_SIZE) {
        source
    } else {
        Bitmap.createScaledBitmap(source, PALETTE_BITMAP_SIZE, PALETTE_BITMAP_SIZE, true)
    }

    return runCatching {
        scaled.copy(Bitmap.Config.ARGB_8888, false)
    }.getOrNull().also {
        if (scaled !== source) scaled.recycle()
    }
}

internal fun deriveCinematicAccentPalette(
    seeds: AnimeArtworkPaletteSeeds,
    colorScheme: ColorScheme,
): CinematicAccentPalette {
    val rawStart = lerp(seeds.primary, colorScheme.primary, 0.12f)
    val rawMiddle = lerp(seeds.secondary, colorScheme.secondary, 0.20f)
    val rawEnd = lerp(seeds.tertiary, colorScheme.tertiary, 0.22f)
    val content = bestReadableContentColor(
        backgrounds = listOf(rawStart, rawMiddle, rawEnd, colorScheme.primary),
        preferred = colorScheme.onPrimary,
    )

    val accentStart = ensureContrast(rawStart, content, colorScheme.primary)
    val accentMiddle = ensureContrast(rawMiddle, content, colorScheme.primary)
    val accentEnd = ensureContrast(rawEnd, content, colorScheme.primary)
    val activeStart = ensureContrast(
        lerp(accentStart, accentMiddle, 0.22f),
        content,
        colorScheme.primary,
    )
    val activeEnd = ensureContrast(
        lerp(accentEnd, accentStart, 0.18f),
        content,
        colorScheme.primary,
    )

    return CinematicAccentPalette(
        accentGradientStart = accentStart,
        accentGradientMiddle = accentMiddle,
        accentGradientEnd = accentEnd,
        activeGradientStart = activeStart,
        activeGradientEnd = activeEnd,
        borderHighlight = lerp(seeds.secondary, colorScheme.outline, 0.46f).copy(alpha = 0.42f),
        glow = lerp(seeds.primary, colorScheme.primary, 0.24f).copy(alpha = 0.16f),
        accentGradientContentColor = content,
    )
}

private fun bestReadableContentColor(
    backgrounds: List<Color>,
    preferred: Color,
): Color {
    val candidates = listOf(
        preferred,
        Color.White,
        Color(0xFF0A0A10),
    )
    return candidates.maxBy { candidate ->
        backgrounds.minOf { background -> contrastRatio(candidate, background) }
    }
}

private fun ensureContrast(
    background: Color,
    foreground: Color,
    fallback: Color,
): Color {
    if (contrastRatio(foreground, background) >= MINIMUM_TEXT_CONTRAST) return background

    var candidate = background
    repeat(10) {
        candidate = lerp(candidate, fallback, 0.18f)
        if (contrastRatio(foreground, candidate) >= MINIMUM_TEXT_CONTRAST) return candidate
    }

    val towardBlack = background.luminance() > foreground.luminance()
    repeat(10) {
        candidate = lerp(candidate, if (towardBlack) Color.White else Color.Black, 0.12f)
        if (contrastRatio(foreground, candidate) >= MINIMUM_TEXT_CONTRAST) return candidate
    }

    return if (contrastRatio(foreground, fallback) >= MINIMUM_TEXT_CONTRAST) {
        fallback
    } else if (foreground.luminance() >= 0.5f) {
        Color.Black
    } else {
        Color.White
    }
}

internal fun cinematicContrastRatio(
    foreground: Color,
    background: Color,
): Float {
    return contrastRatio(foreground, background)
}

private fun contrastRatio(
    foreground: Color,
    background: Color,
): Float {
    val lighter = maxOf(foreground.luminance(), background.luminance())
    val darker = minOf(foreground.luminance(), background.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun hueDistance(
    first: Float,
    second: Float,
): Float {
    val raw = abs(first - second)
    return minOf(raw, 360f - raw)
}
