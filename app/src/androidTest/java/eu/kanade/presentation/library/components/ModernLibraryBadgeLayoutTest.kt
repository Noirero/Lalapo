package eu.kanade.presentation.library.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import eu.kanade.domain.ui.model.DesignStyle
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun lightCompactWrapsFullCountsAndLanguage() = denseCard(false, downloads = 100, unseen = 100, language = "zh-Hans")

    @Test
    fun darkCompactWrapsFullCountsAndLanguage() = denseCard(true, downloads = 100, unseen = 100, language = "zh-Hans")

    @Test
    fun lightComfortableWrapsFullCountsAndLanguage() = denseCard(false, true, 100, 100, "zh-Hans")

    @Test
    fun darkComfortableWrapsFullCountsAndLanguage() = denseCard(true, true, 100, 100, "zh-Hans")

    private fun denseCard(
        dark: Boolean,
        comfortable: Boolean = false,
        downloads: Int = 12,
        unseen: Long = 3,
        language: String = "en",
    ) {
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
                        val start: @Composable RowScope.() -> Unit = {
                            DownloadsBadge(downloads)
                            UnseenBadge(unseen)
                        }
                        val end: @Composable RowScope.() -> Unit = { LanguageBadge(false, language) }
                        if (comfortable) {
                            AnimeComfortableGridItem(
                                coverData = AnimeCover(1, 0, false, null, 0),
                                onClick = { clicks.incrementAndGet() },
                                onLongClick = {},
                                title = "Fixture",
                                isSelected = true,
                                useModernLibraryTreatment = true,
                                coverBadgeStart = start,
                                coverBadgeEnd = end,
                            )
                        } else {
                            AnimeCompactGridItem(
                                coverData = AnimeCover(1, 0, false, null, 0),
                                onClick = { clicks.incrementAndGet() },
                                onLongClick = {},
                                title = "Fixture",
                                isSelected = true,
                                useModernLibraryTreatment = true,
                                coverBadgeStart = start,
                                coverBadgeEnd = end,
                            )
                        }
                    }
                }
            }
        }
        val badgeBounds = listOf(downloads.toString(), unseen.toString(), language.uppercase())
            .groupingBy { it }.eachCount().flatMap { (text, count) ->
                val nodes = compose.onAllNodesWithText(text, useUnmergedTree = true)
                assertEquals(count, nodes.fetchSemanticsNodes().size)
                (0 until count).map { index ->
                    val layouts = mutableListOf<TextLayoutResult>()
                    nodes[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                    val layout = layouts.single()
                    assertEquals(text.length, layout.getLineEnd(layout.lineCount - 1))
                    assertTrue(layout.multiParagraph.height <= layout.size.height + 1f)
                    repeat(layout.lineCount) { line ->
                        assertTrue(layout.getLineLeft(line) >= -1f)
                        assertTrue(layout.getLineRight(line) <= layout.size.width + 1f)
                        assertFalse(layout.isLineEllipsized(line))
                    }
                    nodes[index].fetchSemanticsNode().boundsInRoot
                }
            }
        badgeBounds.forEachIndexed { index, bounds ->
            badgeBounds.drop(index + 1).forEach { other -> assertFalse(bounds.overlaps(other)) }
        }
        val image = compose.onNode(hasContentDescription(""), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(0.845f, image.width / image.height, 0.01f)
        compose.onNode(hasClickAction() and hasText("Fixture")).performTouchInput { click(center) }
        compose.waitForIdle()
        assertEquals(1, clicks.get())
    }
}
