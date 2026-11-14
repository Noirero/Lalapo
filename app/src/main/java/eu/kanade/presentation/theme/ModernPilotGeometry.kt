package eu.kanade.presentation.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Local recipes for the phone shell and Library; these are not application-wide Material tokens. */
internal object ModernPhoneNavigationGeometry {
    val HorizontalMargin = 18.dp
    val VerticalMargin = 8.dp

    // NavigationSuite may impose a taller minimum; it continues to own system insets.
    val MinHeight = 70.dp
    val SelectedHeight = 60.dp
    val IconSize = 20.dp
    val SelectedIconSize = 22.dp
    val LabelSize = 13.sp
    val LabelLineHeight = 16.sp
    val BorderWidth = 1.dp
}

/** Palette and authored preset shapes remain owned by [ModernLibraryChrome]. */
internal object ModernLibraryGeometry {
    val TouchTarget = 48.dp
    val SearchHeight = 44.dp
    val SearchTouchInset = (TouchTarget - SearchHeight) / 2
    val ScreenMargin = 18.dp
    val TitleMargin = 24.dp
    val TitleSize = 30.sp
    val SubtitleSize = 14.sp
    val CategoryMargin = 14.dp
    val CategoryRailHeight = 48.dp
    val CategoryVisualHeight = 32.dp
    val CategoryVerticalInset = (CategoryRailHeight - CategoryVisualHeight) / 2
    val CategoryHorizontalPadding = 7.dp
    val FilterHeight = 34.dp
    val FilterGap = 8.dp
    val FilterIcon = 16.dp
    val FilterText = 13.sp
    val FilterLineHeight = 16.sp

    // Recycler padding + the item's own inset produces 18dp outer edges and 8dp cover gutters.
    val GridPadding = 14.dp
    val GridItemInset = 4.dp
    val GridSpacing = 0.dp
    const val COVER_ASPECT_RATIO = 0.845f
    val ScrimHeight = 54.dp
    val CoverTitleSize = 10.5.sp
    val ListCoverSize = 48.dp
    val ListInset = 12.dp

    // The active cover overlay scales text from measured bounds, independently of grid preferences.
    fun overlayTitleSize(widthDp: Float, heightDp: Float): Float {
        val scale = minOf(widthDp / 128f, heightDp / 152f).coerceIn(0.64f, 1.4f)
        return if (scale < 0.8f) {
            9f + ((scale - 0.64f) / 0.16f).coerceIn(0f, 1f)
        } else {
            (10.5f * scale).coerceIn(10f, 15f)
        }
    }
}
