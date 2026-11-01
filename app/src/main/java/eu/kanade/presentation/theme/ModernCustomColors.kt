package eu.kanade.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal const val DEFAULT_CUSTOM_PRIMARY = "#485CE0"
private val rgbHexPattern = Regex("^#?[0-9a-fA-F]{6}$")

internal fun normalizeCustomHex(input: String): String? {
    val candidate = input.trim()
    if (!rgbHexPattern.matches(candidate)) return null
    return "#" + candidate.removePrefix("#").uppercase(Locale.ROOT)
}

internal fun customHexColor(input: String): Color? =
    normalizeCustomHex(input)?.substring(1)?.toLong(16)?.let { Color((0xFF000000L or it).toInt()) }

internal fun customColorHex(color: Color): String {
    val r = (color.red * 255f).roundToInt().coerceIn(0, 255)
    val g = (color.green * 255f).roundToInt().coerceIn(0, 255)
    val b = (color.blue * 255f).roundToInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(Locale.ROOT, r, g, b)
}

internal data class CustomHsv(val hue: Float, val saturation: Float, val value: Float)

internal fun customToHsv(color: Color): CustomHsv {
    val r = color.red
    val g = color.green
    val b = color.blue
    val high = max(r, max(g, b))
    val low = min(r, min(g, b))
    val delta = high - low
    val hue = if (delta < 0.00001f) {
        0f
    } else {
        val sector = when (high) {
            r -> ((g - b) / delta) % 6f
            g -> ((b - r) / delta) + 2f
            else -> ((r - g) / delta) + 4f
        }
        ((sector * 60f) + 360f) % 360f
    }
    return CustomHsv(hue, if (high == 0f) 0f else delta / high, high)
}

internal fun customFromHsv(hsv: CustomHsv): Color {
    val h = ((hsv.hue % 360f) + 360f) % 360f
    val s = hsv.saturation.coerceIn(0f, 1f)
    val v = hsv.value.coerceIn(0f, 1f)
    val chroma = v * s
    val sector = h / 60f
    val x = chroma * (1f - abs((sector % 2f) - 1f))
    val (r, g, b) = when (sector.toInt()) {
        0 -> Triple(chroma, x, 0f)
        1 -> Triple(x, chroma, 0f)
        2 -> Triple(0f, chroma, x)
        3 -> Triple(0f, x, chroma)
        4 -> Triple(x, 0f, chroma)
        else -> Triple(chroma, 0f, x)
    }
    val m = v - chroma
    return Color(r + m, g + m, b + m)
}

@Immutable
internal data class ModernCustomColors(
    val primary: Color,
    val secondaryOverride: Color? = null,
    val tertiaryOverride: Color? = null,
) {
    private val primaryHsv get() = customToHsv(primary)
    private val anchorHue get() = if (primaryHsv.saturation < 0.14f) 210f else primaryHsv.hue

    val secondary: Color get() = secondaryOverride ?: customFromHsv(
        CustomHsv(
            hue = (anchorHue + 45f) % 360f,
            saturation = (primaryHsv.saturation * 0.85f).coerceIn(0.48f, 0.72f),
            value = (primaryHsv.value * 1.05f).coerceIn(0.58f, 0.82f),
        ),
    )

    val tertiary: Color get() = tertiaryOverride ?: customFromHsv(
        CustomHsv(
            hue = (anchorHue + 155f) % 360f,
            saturation = (primaryHsv.saturation * 0.90f).coerceIn(0.50f, 0.78f),
            value = primaryHsv.value.coerceIn(0.56f, 0.83f),
        ),
    )

    companion object {
        // Corrupt stored values are repaired at read time without modifying the user's preference.
        fun fromStored(primary: String, secondary: String, tertiary: String): ModernCustomColors =
            ModernCustomColors(
                primary = customHexColor(primary) ?: requireNotNull(customHexColor(DEFAULT_CUSTOM_PRIMARY)),
                secondaryOverride = customHexColor(secondary),
                tertiaryOverride = customHexColor(tertiary),
            )
    }
}

internal data class ModernCustomDraft(
    val primary: String,
    val secondaryOverride: String?,
    val tertiaryOverride: String?,
) {
    val isValid: Boolean get() =
        normalizeCustomHex(primary) != null &&
            (secondaryOverride == null || normalizeCustomHex(secondaryOverride) != null) &&
            (tertiaryOverride == null || normalizeCustomHex(tertiaryOverride) != null)

    fun normalized(): ModernCustomDraft? = if (!isValid) {
        null
    } else {
        ModernCustomDraft(
            primary = requireNotNull(normalizeCustomHex(primary)),
            secondaryOverride = secondaryOverride?.let { requireNotNull(normalizeCustomHex(it)) },
            tertiaryOverride = tertiaryOverride?.let { requireNotNull(normalizeCustomHex(it)) },
        )
    }

    fun colors(): ModernCustomColors? = normalized()?.let {
        ModernCustomColors.fromStored(
            primary = it.primary,
            secondary = it.secondaryOverride.orEmpty(),
            tertiary = it.tertiaryOverride.orEmpty(),
        )
    }
}
