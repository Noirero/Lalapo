package eu.kanade.presentation.browse

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import eu.kanade.domain.ui.model.DesignStyle
import eu.kanade.presentation.browse.components.GlobalSearchToolbar
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModernGlobalSearchToolbarTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lightQueryHasRoomForItsWholeLine() = query(dark = false, fontScale = 1f)

    @Test
    fun darkQueryHasRoomForItsWholeLine() = query(dark = true, fontScale = 1f)

    @Test
    fun lightLargeFontQueryHasRoomForItsWholeLine() = query(dark = false, fontScale = 2f)

    @Test
    fun darkLargeFontQueryHasRoomForItsWholeLine() = query(dark = true, fontScale = 2f)

    private fun query(dark: Boolean, fontScale: Float) {
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDensity provides Density(LocalDensity.current.density, fontScale),
            ) {
                TachiyomiPreviewTheme(designStyle = DesignStyle.MODERN) {
                    Box(Modifier.width(320.dp)) {
                        GlobalSearchToolbar(
                            searchQuery = "Query", progress = 0, total = 0, navigateUp = {},
                            onChangeSearchQuery = {}, onSearch = {},
                            hideSourceFilter = true, sourceFilter = SourceFilter.All, onChangeSearchFilter = {},
                            onlyShowHasResults = false, onToggleResults = {},
                            scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                        )
                    }
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val text = layouts.single()
        assertTrue(
            "Query paragraph is clipped: ${text.multiParagraph.height}px in ${text.size.height}px",
            text.size.height >= text.multiParagraph.height - 1f,
        )
    }
}
