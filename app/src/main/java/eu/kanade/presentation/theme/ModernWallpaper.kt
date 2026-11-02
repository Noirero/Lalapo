package eu.kanade.presentation.theme

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import eu.kanade.tachiyomi.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@DrawableRes
internal fun modernWallpaperResource(
    isModern: Boolean,
    identity: ModernArtworkIdentity?,
): Int? = if (!isModern || identity == null) {
    null
} else {
    when (identity) {
        ModernArtworkIdentity.LALAPO_AURORA -> R.drawable.modern_wallpaper_lalapo
        ModernArtworkIdentity.SAKURA_BLOSSOM -> R.drawable.modern_wallpaper_sakura
        ModernArtworkIdentity.VIOLET_MOON_AURORA -> R.drawable.modern_wallpaper_violet
        ModernArtworkIdentity.CYAN_WATER_SKY -> R.drawable.modern_wallpaper_cyan
        ModernArtworkIdentity.EMERALD_FOLIAGE -> R.drawable.modern_wallpaper_emerald
        ModernArtworkIdentity.AMBER_MAPLE_SUNSET -> R.drawable.modern_wallpaper_amber
        ModernArtworkIdentity.CUSTOM_WAVES -> null
    }
}

/**
 * Shared Miyorare-style backdrop for authored Modern presets.
 *
 * The full portrait is sampled down before a bounded two-pass CPU box blur. Only two tiny blurred
 * bitmaps are retained, so scrolling never performs live blur work and switching through all six
 * presets cannot pin six full-resolution wallpapers in a process-lifetime cache.
 */
@Composable
internal fun ModernGlobalBackground(modifier: Modifier = Modifier) {
    val palette = LocalModernVisualPalette.current
    val resourceId = modernWallpaperResource(
        isModern = palette.isModern,
        identity = LocalModernArtworkIdentity.current,
    ) ?: return
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() >= 0.5f
    val isAmoled = scheme.background.red == 0f &&
        scheme.background.green == 0f && scheme.background.blue == 0f
    val semanticBackground = scheme.background.copy(alpha = 1f)
    val baseColor = if (isLight) {
        lerp(semanticBackground, Color.White, 0.30f)
    } else {
        semanticBackground
    }
    val blurred by produceState<Bitmap?>(initialValue = null, key1 = resourceId) {
        value = withContext(Dispatchers.Default) {
            ModernBlurredWallpaperCache.getOrCreate(context.resources, resourceId)
        }
    }
    val washColor = if (isLight) Color.White else baseColor
    val wash = if (isAmoled) {
        listOf(
            Color.Black.copy(alpha = 0.34f),
            Color.Black.copy(alpha = 0.58f),
            Color.Black.copy(alpha = 0.86f),
        )
    } else if (isLight) {
        listOf(
            washColor.copy(alpha = 0.32f),
            washColor.copy(alpha = 0.22f),
            washColor.copy(alpha = 0.30f),
        )
    } else {
        listOf(
            washColor.copy(alpha = 0.46f),
            washColor.copy(alpha = 0.38f),
            washColor.copy(alpha = 0.50f),
        )
    }

    Box(modifier = modifier.fillMaxSize().background(baseColor)) {
        blurred?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier.matchParentSize(),
                alpha = when {
                    isAmoled -> 0.58f
                    isLight -> 0.62f
                    else -> 1f
                },
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(wash)),
        )
    }
}

/** Sharp authored portrait used only while Pustaka is the active Home destination. */
@Composable
internal fun ModernSharpWallpaper(modifier: Modifier = Modifier) {
    val palette = LocalModernVisualPalette.current
    val resourceId = modernWallpaperResource(
        isModern = palette.isModern,
        identity = LocalModernArtworkIdentity.current,
    ) ?: return
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() >= 0.5f
    val isAmoled = scheme.background.red == 0f &&
        scheme.background.green == 0f && scheme.background.blue == 0f
    val washColor = if (isAmoled) Color.Black else scheme.background.copy(alpha = 1f)
    val wash = when {
        isAmoled -> listOf(
            Color.Transparent,
            washColor.copy(alpha = 0.24f),
            washColor.copy(alpha = 0.70f),
        )
        isLight -> listOf(
            washColor.copy(alpha = 0.04f),
            washColor.copy(alpha = 0.11f),
            washColor.copy(alpha = 0.22f),
        )
        else -> listOf(
            washColor.copy(alpha = 0.08f),
            washColor.copy(alpha = 0.18f),
            washColor.copy(alpha = 0.34f),
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(resourceId),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter,
            modifier = Modifier.matchParentSize(),
            alpha = if (isAmoled) 0.82f else 1f,
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(wash)),
        )
    }
}

private object ModernBlurredWallpaperCache {
    private const val BLUR_WIDTH = 135
    private const val BLUR_HEIGHT = 301
    private const val BLUR_RADIUS = 7
    private const val BLUR_PASSES = 2
    private val cache = LruCache<Int, Bitmap>(2)

    fun getOrCreate(resources: Resources, @DrawableRes resourceId: Int): Bitmap? {
        synchronized(cache) {
            cache.get(resourceId)?.let { return it }
        }
        val decoded = decodeSampled(resources, resourceId) ?: return null
        val scaled = Bitmap.createScaledBitmap(decoded, BLUR_WIDTH, BLUR_HEIGHT, true)
        if (scaled !== decoded) decoded.recycle()
        val blurred = boxBlur(scaled, BLUR_RADIUS, BLUR_PASSES)
        if (blurred !== scaled) scaled.recycle()
        synchronized(cache) {
            cache.put(resourceId, blurred)
        }
        return blurred
    }

    private fun decodeSampled(resources: Resources, @DrawableRes resourceId: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(resources, resourceId, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        val targetWidth = BLUR_WIDTH * 2
        val targetHeight = BLUR_HEIGHT * 2
        while (
            bounds.outWidth / (sample * 2) >= targetWidth &&
            bounds.outHeight / (sample * 2) >= targetHeight
        ) {
            sample *= 2
        }
        return BitmapFactory.decodeResource(
            resources,
            resourceId,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        )
    }

    private fun boxBlur(source: Bitmap, radius: Int, passes: Int): Bitmap {
        val width = source.width
        val height = source.height
        var pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        repeat(passes) {
            val horizontal = IntArray(pixels.size)
            for (y in 0 until height) {
                val row = y * width
                for (x in 0 until width) {
                    var a = 0
                    var r = 0
                    var g = 0
                    var b = 0
                    var count = 0
                    for (offset in -radius..radius) {
                        val px = pixels[row + (x + offset).coerceIn(0, width - 1)]
                        a += px ushr 24 and 0xFF
                        r += px ushr 16 and 0xFF
                        g += px ushr 8 and 0xFF
                        b += px and 0xFF
                        count++
                    }
                    horizontal[row + x] =
                        (a / count shl 24) or
                        (r / count shl 16) or
                        (g / count shl 8) or
                        (b / count)
                }
            }

            val vertical = IntArray(pixels.size)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    var a = 0
                    var r = 0
                    var g = 0
                    var b = 0
                    var count = 0
                    for (offset in -radius..radius) {
                        val px = horizontal[(y + offset).coerceIn(0, height - 1) * width + x]
                        a += px ushr 24 and 0xFF
                        r += px ushr 16 and 0xFF
                        g += px ushr 8 and 0xFF
                        b += px and 0xFF
                        count++
                    }
                    vertical[y * width + x] =
                        (a / count shl 24) or
                        (r / count shl 16) or
                        (g / count shl 8) or
                        (b / count)
                }
            }
            pixels = vertical
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}
