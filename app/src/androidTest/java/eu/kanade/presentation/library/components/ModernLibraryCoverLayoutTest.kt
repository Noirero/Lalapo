package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import eu.kanade.domain.ui.model.DesignStyle
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.domain.anime.model.AnimeCover
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class ModernLibraryCoverLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun modernCompactCoverUsesPilotRatioAndKeepsSelectedItemInteractive() {
        compactCover(designStyle = DesignStyle.MODERN, expectedRatio = 0.845f)
    }

    @Test
    fun classicCompactCoverKeepsBookRatioAndClickCallback() {
        compactCover(designStyle = DesignStyle.CLASSIC, expectedRatio = 2f / 3f)
    }

    private fun compactCover(designStyle: DesignStyle, expectedRatio: Float) {
        val title = "Fixture"
        val badge = "3"
        val clickCalls = AtomicInteger()
        compose.setContent {
            TachiyomiPreviewTheme(designStyle = designStyle) {
                Box(Modifier.width(144.dp)) {
                    AnimeCompactGridItem(
                        coverData = AnimeCover(
                            animeId = 1L,
                            sourceId = 0L,
                            isAnimeFavorite = false,
                            url = null,
                            lastModified = 0L,
                        ),
                        onClick = { clickCalls.incrementAndGet() },
                        onLongClick = {},
                        isSelected = true,
                        title = title,
                        coverBadgeStart = { Text(badge) },
                        useModernLibraryTreatment = designStyle == DesignStyle.MODERN,
                    )
                }
            }
        }

        val item = compose.onNode(hasClickAction() and hasText(title))
        item.assertIsDisplayed()
        if (designStyle == DesignStyle.MODERN) item.assertIsSelected()
        compose.onNodeWithText(badge, useUnmergedTree = true).assertIsDisplayed()

        val bounds = item.fetchSemanticsNode().boundsInRoot
        val inset = with(compose.density) { 4.dp.roundToPx() }
        val expectedHeight = (bounds.width - 2 * inset) / expectedRatio + 2 * inset
        assertEquals(expectedHeight, bounds.height, 1.5f)

        item.performTouchInput { click(center) }
        compose.waitForIdle()
        assertEquals(1, clickCalls.get())
    }
}
