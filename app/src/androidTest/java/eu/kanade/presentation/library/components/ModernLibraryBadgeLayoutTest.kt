package eu.kanade.presentation.library.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import eu.kanade.domain.ui.model.DesignStyle
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.domain.anime.model.AnimeCover
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class ModernLibraryBadgeLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lightDenseCardKeepsEveryCountVisibleAndRetainsItsImageRatio() = denseCard(false)

    @Test
    fun darkDenseCardKeepsEveryCountVisibleAndRetainsItsImageRatio() = denseCard(true)

    private fun denseCard(dark: Boolean) {
        val clicks = AtomicInteger()
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDensity provides Density(LocalDensity.current.density, 2f),
            ) {
                TachiyomiPreviewTheme(designStyle = DesignStyle.MODERN) {
                    Box(Modifier.width(98.dp)) {
                        AnimeCompactGridItem(
                            coverData = AnimeCover(1, 0, false, null, 0),
                            onClick = { clicks.incrementAndGet() },
                            onLongClick = {},
                            title = "Fixture",
                            isSelected = true,
                            useModernLibraryTreatment = true,
                            coverBadgeStart = {
                                DownloadsBadge(12)
                                UnseenBadge(3)
                            },
                            coverBadgeEnd = { LanguageBadge(false, "en") },
                        )
                    }
                }
            }
        }
        val language = compose.onNodeWithText("EN", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val download = compose.onNodeWithText("12", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val unseen = compose.onNodeWithText("3", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertFalse(language.overlaps(download))
        assertFalse(language.overlaps(unseen))
        val image = compose.onNode(hasContentDescription(""), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(0.845f, image.width / image.height, 0.01f)
        compose.onNode(hasClickAction() and hasText("Fixture")).performTouchInput { click(center) }
        compose.waitForIdle()
        assertEquals(1, clicks.get())
    }
}
