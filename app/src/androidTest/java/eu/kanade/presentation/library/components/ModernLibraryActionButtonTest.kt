package eu.kanade.presentation.library.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import eu.kanade.domain.ui.model.DesignStyle
import eu.kanade.domain.ui.model.ModernThemePreset
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class ModernLibraryActionButtonTest {
    @get:Rule
    val compose = createComposeRule()

    private val filterCalls = AtomicInteger()
    private val sortCalls = AtomicInteger()
    private lateinit var filterLabel: String
    private lateinit var sortLabel: String

    @Test
    fun referenceLightEdgeTapsUseOneOwner() = toolbarEdgeTaps(432, false, 1f)

    @Test
    fun referenceDarkEdgeTapsUseOneOwner() = toolbarEdgeTaps(432, true, 1f)

    @Test
    fun narrowLightLargeFontEdgeTapsUseOneOwner() = toolbarEdgeTaps(320, false, 2f)

    @Test
    fun narrowDarkLargeFontEdgeTapsUseOneOwner() = toolbarEdgeTaps(320, true, 2f)

    private fun toolbarEdgeTaps(widthDp: Int, dark: Boolean, fontScale: Float) {
        requireViewport(widthDp)
        compose.setContent {
            FixtureTheme(dark, fontScale) {
                filterLabel = stringResource(MR.strings.action_filter)
                sortLabel = stringResource(MR.strings.action_sort)
                Box(Modifier.width(widthDp.dp)) {
                    LibraryToolbar(
                        hasActiveFilters = true,
                        selectedCount = 0,
                        title = LibraryToolbarTitle("Library fixture"),
                        sets = emptyList(),
                        activeSetKey = "",
                        categories = emptyList(),
                        activeCategoryIndex = 0,
                        showCategoryTabs = false,
                        groupType = LibraryGroup.UNGROUPED,
                        hasCategories = false,
                        getItemCountForCategory = { null },
                        onClickSettings = {},
                        onClickUnselectAll = {},
                        onClickSelectAll = {},
                        onClickInvertSelection = {},
                        onSelectSet = {},
                        onManageSets = {},
                        onSelectCategory = {},
                        onClickFilter = { filterCalls.incrementAndGet() },
                        onClickSort = { sortCalls.incrementAndGet() },
                        onClickRefresh = {},
                        onClickGlobalUpdate = {},
                        onClickOpenRandomAnime = {},
                        searchQuery = null,
                        onSearchQueryChange = {},
                        scrollBehavior = null,
                    )
                }
            }
        }
        compose.waitForIdle()
        val filterMatcher = hasText(filterLabel) and hasClickAction()
        val sortMatcher = hasText(sortLabel) and hasClickAction()
        compose.onAllNodes(filterMatcher, useUnmergedTree = true).assertCountEquals(1)
        val filter = compose.onNode(filterMatcher)
        val sort = compose.onNode(sortMatcher)
        filter.assertIsSelected().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        sort.assertIsNotSelected()

        val bounds = filter.fetchSemanticsNode().boundsInRoot
        val sibling = sort.fetchSemanticsNode().boundsInRoot
        val minimum = with(compose.density) { 48.dp.toPx() }
        val inset = with(compose.density) { 1.dp.toPx() }
        assertTrue(bounds.height >= minimum - 0.5f)
        assertTrue(bounds.right < sibling.left)
        val xs = listOf(bounds.left + inset, bounds.center.x, bounds.right - inset)
        val ys = listOf(bounds.top + inset, bounds.center.y, bounds.bottom - inset)
        var expected = 0
        for (x in xs) {
            for (y in ys) {
                compose.onRoot().performTouchInput { click(Offset(x, y)) }
                compose.waitForIdle()
                assertEquals(++expected, filterCalls.get())
                assertEquals(0, sortCalls.get())
            }
        }
        // The gap and outer margin must not borrow either neighbour's touch expansion.
        compose.onRoot().performTouchInput {
            click(Offset((bounds.right + sibling.left) / 2f, bounds.center.y))
            click(Offset(bounds.left - inset, bounds.center.y))
        }
        compose.waitForIdle()
        assertEquals(expected, filterCalls.get())
        assertEquals(0, sortCalls.get())
        sort.performClick()
        filter.performClick()
        compose.waitForIdle()
        assertEquals(expected + 1, filterCalls.get())
        assertEquals(1, sortCalls.get())
    }

    @Test
    fun longLabelGrowsAtLargeFontWithoutClipping() = longLabel(ModernThemePreset.LALAPO)

    @Test
    fun cutCornerIdentityKeepsRectangularTouchOwner() = longLabel(ModernThemePreset.VIOLET)

    private fun longLabel(preset: ModernThemePreset) {
        requireViewport(320)
        val label = "Filter a collection with a long label"
        compose.setContent {
            FixtureTheme(dark = true, fontScale = 2f, preset = preset) {
                Row(
                    modifier = Modifier.width(320.dp).padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ModernLibraryActionButton(
                        onClick = { filterCalls.incrementAndGet() },
                        icon = Icons.Outlined.FilterList,
                        label = label,
                        selected = true,
                        modifier = Modifier.weight(1f),
                    )
                    ModernLibraryActionButton(
                        onClick = { sortCalls.incrementAndGet() },
                        icon = Icons.Outlined.FilterList,
                        label = "Sort",
                        selected = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        val owner = compose.onNode(hasText(label) and hasClickAction()).fetchSemanticsNode().boundsInRoot
        val text = compose.onNodeWithText(label, useUnmergedTree = true)
        val layouts = mutableListOf<TextLayoutResult>()
        text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        assertTrue(layouts.single().lineCount > 1)
        assertFalse(layouts.single().hasVisualOverflow)
        val textBounds = text.fetchSemanticsNode().boundsInRoot
        assertTrue(textBounds.top >= owner.top && textBounds.bottom <= owner.bottom)
        assertTrue(textBounds.left >= owner.left && textBounds.right <= owner.right)
        val inset = with(compose.density) { 1.dp.toPx() }
        compose.onRoot().performTouchInput { click(Offset(owner.left + inset, owner.top + inset)) }
        compose.waitForIdle()
        assertEquals(1, filterCalls.get())
        assertEquals(0, sortCalls.get())
    }

    private fun requireViewport(widthDp: Int) {
        val configuration = InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration
        assumeTrue("Set the device viewport to at least ${widthDp}dp wide", configuration.screenWidthDp >= widthDp)
    }

    @Composable
    private fun FixtureTheme(
        dark: Boolean,
        fontScale: Float,
        preset: ModernThemePreset = ModernThemePreset.LALAPO,
        content: @Composable () -> Unit,
    ) {
        val configuration = Configuration(LocalConfiguration.current).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalConfiguration provides configuration,
            LocalDensity provides Density(density.density, fontScale),
        ) {
            TachiyomiPreviewTheme(designStyle = DesignStyle.MODERN, modernThemePreset = preset, content = content)
        }
    }
}
