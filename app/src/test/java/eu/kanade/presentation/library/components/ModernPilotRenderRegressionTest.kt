package eu.kanade.presentation.library.components

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.asImage
import coil3.decode.DataSource
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import eu.kanade.domain.ui.model.DesignStyle
import eu.kanade.presentation.browse.GlobalSearchScope
import eu.kanade.presentation.browse.components.GlobalSearchToolbar
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.ui.browse.source.browse.SourceFilterDialog
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.robolectric.annotation.GraphicsMode
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.anime.model.AnimeCover
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.TriStateItem
import tachiyomi.presentation.core.i18n.stringResource
import java.util.concurrent.atomic.AtomicInteger

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w432dp-h768dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@ConscryptMode(ConscryptMode.Mode.OFF)
@OptIn(DelicateCoilApi::class)
class ModernPilotRenderRegressionTest(private val width: Int, private val dark: Boolean, private val fontScale: Float) {
    init {
        RuntimeEnvironment.setQualifiers("en-rUS-w${width}dp-h768dp-${if (dark) "night" else "notnight"}-xhdpi")
        RuntimeEnvironment.setFontScale(fontScale)
    }

    @get:Rule
    val compose = createComposeRule()

    private val coverLoads = AtomicInteger()
    private lateinit var imageLoader: ImageLoader
    private val suffix get() = "${width}_${if (dark) "dark" else "light"}_font$fontScale"

    @Before
    fun configureFixture() {
        val resources = ApplicationProvider.getApplicationContext<Application>().resources
        assertEquals(width, resources.configuration.screenWidthDp)
        assertEquals(768, resources.configuration.screenHeightDp)
        assertEquals(fontScale, resources.configuration.fontScale, 0.001f)
        assertEquals(2f, resources.displayMetrics.density, 0.001f)
        val bitmap = Bitmap.createBitmap(240, 360, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        repeat(6) { band ->
            paint.color = if (band % 2 == 0) 0xFF444444.toInt() else 0xFFCCCCCC.toInt()
            canvas.drawRect(0f, band * 60f, 240f, (band + 1) * 60f, paint)
        }
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawRect(117f, 0f, 123f, 360f, paint)
        paint.color = 0xFF000000.toInt()
        canvas.drawRect(0f, 28f, 240f, 32f, paint)
        canvas.drawRect(0f, 328f, 240f, 332f, paint)
        imageLoader = ImageLoader.Builder(ApplicationProvider.getApplicationContext())
            .components {
                add(object : Interceptor {
                    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
                        coverLoads.incrementAndGet()
                        return SuccessResult(bitmap.asImage(), chain.request, DataSource.MEMORY)
                    }
                })
            }
            .build()
        SingletonImageLoader.setUnsafe(imageLoader)
    }

    @After
    fun releaseFixture() {
        SingletonImageLoader.reset()
        imageLoader.shutdown()
    }

    @Test
    fun autoGridKeepsMeasuredCoverGutters() = grid(columns = 0)

    @Test
    fun fixedGridKeepsThreeTracks() = grid(columns = 3)

    @Test
    fun adaptiveTracksChangeAtMeasuredBreakpoints() {
        var containerWidth by mutableIntStateOf(275)
        render {
            LazyLibraryGrid(
                modifier = Modifier.width(containerWidth.dp),
                columns = 0,
                contentPadding = PaddingValues(0.dp),
            ) {
                items(6) { Box(Modifier.fillMaxWidth().height(72.dp).testTag("track")) }
            }
        }
        val cases = mapOf(275 to 1, 276 to 1, 283 to 1, 284 to 2, 407 to 2, 408 to 2, 411 to 2, 412 to 3)
        cases.filterKeys { it <= width }.forEach { (availableWidth, expectedColumns) ->
            compose.runOnIdle { containerWidth = availableWidth }
            val cells = compose.onAllNodesWithTag("track").fetchSemanticsNodes().map { it.boundsInRoot }
            val firstRow = cells.filter { kotlin.math.abs(it.top - cells.minOf { cell -> cell.top }) < 1f }
            assertEquals("Columns at ${availableWidth}dp", expectedColumns, firstRow.size)
        }
    }

    private fun grid(columns: Int) {
        render {
            LazyLibraryGrid(columns = columns, contentPadding = PaddingValues(0.dp)) {
                items(6) { index ->
                    AnimeCompactGridItem(
                        coverData = cover(index),
                        onClick = {},
                        onLongClick = {},
                        title = "Fixture $index",
                        isSelected = index == 0,
                        coverBadgeStart = {
                            DownloadsBadge(12)
                            UnseenBadge(3)
                        },
                        coverBadgeEnd = { LanguageBadge(false, "en") },
                        useModernLibraryTreatment = true,
                    )
                }
            }
        }
        compose.waitUntil(10_000) { coverLoads.get() >= 6 }
        compose.waitForIdle()
        val images = compose.onAllNodes(hasContentDescription(""), useUnmergedTree = true)
            .fetchSemanticsNodes().map { it.boundsInRoot }.sortedWith(compareBy({ it.top }, { it.left }))
        assertEquals(6, images.size)
        val firstRow = images.filter { kotlin.math.abs(it.top - images.first().top) < 1f }
        val expectedColumns = if (columns == 3 || width == 432) 3 else 2
        assertEquals(expectedColumns, firstRow.size)
        val density = compose.density.density
        assertEquals(18f * density, firstRow.first().left, 1.1f)
        assertEquals(18f * density, width * density - firstRow.last().right, 1.1f)
        firstRow.zipWithNext().forEach { (left, right) ->
            assertEquals(8f * density, right.left - left.right, 1.1f)
        }
        images.forEach { assertEquals(0.845f, it.width / it.height, 0.01f) }
        val downloads = compose.onAllNodesWithText("12", useUnmergedTree = true).fetchSemanticsNodes()
        val unseen = compose.onAllNodesWithText("3", useUnmergedTree = true).fetchSemanticsNodes()
        val languages = compose.onAllNodesWithText("EN", useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals(6, downloads.size)
        assertEquals(6, unseen.size)
        assertEquals(6, languages.size)
        languages.indices.forEach { index ->
            assertTrue(
                "Language obscures download count",
                !languages[index].boundsInRoot.overlaps(downloads[index].boundsInRoot),
            )
            assertTrue(
                "Language obscures unseen count",
                !languages[index].boundsInRoot.overlaps(unseen[index].boundsInRoot),
            )
        }
        compose.onNode(hasText("Fixture 0") and hasClickAction()).assertIsSelected()
        assertModernPilotGolden("grid_${columns}_$suffix", compose.onRoot())
    }

    @Test
    fun comfortableAndListUseTheirOwnRecipes() {
        render {
            Column {
                Row {
                    repeat(2) { index ->
                        Box(Modifier.weight(1f)) {
                            AnimeComfortableGridItem(
                                coverData = cover(index),
                                title = "Comfortable $index",
                                onClick = {},
                                onLongClick = {},
                                isSelected = index == 0,
                                coverBadgeStart = {
                                    DownloadsBadge(12)
                                    UnseenBadge(3)
                                },
                                useModernLibraryTreatment = true,
                            )
                        }
                    }
                }
                AnimeListItem(
                    coverData = cover(2),
                    title = "List fixture",
                    onClick = {},
                    onLongClick = {},
                    isSelected = true,
                    badge = {
                        DownloadsBadge(12)
                        UnseenBadge(3)
                        LanguageBadge(true, "")
                    },
                    useModernLibraryTreatment = true,
                )
            }
        }
        compose.waitUntil(10_000) { coverLoads.get() >= 3 }
        compose.waitForIdle()
        compose.onNodeWithText("List fixture").assertIsDisplayed()
        assertModernPilotGolden("rows_$suffix", compose.onRoot())
    }

    @Test
    fun headerCategoryAndSearchStatesRenderWithoutChangingCallbacks() {
        var query: String? by mutableStateOf(null)
        val filterCalls = AtomicInteger()
        render {
            LibraryToolbar(
                hasActiveFilters = true,
                selectedCount = 0,
                title = LibraryToolbarTitle("Library fixture"),
                sets = emptyList(), activeSetKey = "",
                categories = listOf(Category(1, "Watching", 0, 0, false), Category(2, "Completed", 1, 0, false)),
                activeCategoryIndex = 0, showCategoryTabs = true,
                groupType = LibraryGroup.UNGROUPED, hasCategories = true,
                getItemCountForCategory = { 6 },
                onClickSettings = {}, onClickUnselectAll = {}, onClickSelectAll = {}, onClickInvertSelection = {},
                onSelectSet = {}, onManageSets = {}, onSelectCategory = {},
                onClickFilter = { filterCalls.incrementAndGet() }, onClickSort = {}, onClickRefresh = {},
                onClickGlobalUpdate = {}, onClickOpenRandomAnime = {},
                searchQuery = query, onSearchQueryChange = { query = it }, scrollBehavior = null,
            )
        }
        assertModernPilotGolden("header_$suffix", compose.onRoot())
        compose.onNode(hasText("Filter") and hasClickAction()).performClick()
        assertEquals(1, filterCalls.get())
    }

    @Test
    fun sourceFilterOpensFromAControlAndKeepsUpdateResetApplyIndependent() {
        val option = object : AnimeFilter.CheckBox("Fixture option") {}
        val filters = AnimeFilterList(AnimeFilter.Header("Source fixture"), AnimeFilter.Separator(), option)
        val updates = AtomicInteger()
        val resets = AtomicInteger()
        val applied = AtomicInteger()
        lateinit var resetLabel: String
        lateinit var applyLabel: String
        render {
            var show by remember { mutableStateOf(false) }
            var revision by remember { mutableIntStateOf(0) }
            Button(onClick = { show = true }) { Text("Open source filters") }
            resetLabel = stringResource(MR.strings.action_reset)
            applyLabel = stringResource(MR.strings.action_filter)
            if (show) {
                @Suppress("UNUSED_EXPRESSION")
                revision
                SourceFilterDialog(
                    onDismissRequest = { show = false },
                    filters = filters,
                    onReset = {
                        option.state = false
                        revision++
                        resets.incrementAndGet()
                    },
                    onFilter = { applied.incrementAndGet() },
                    onUpdate = {
                        revision++
                        updates.incrementAndGet()
                    },
                )
            }
        }
        compose.onNodeWithText("Open source filters").performClick()
        assertModernPilotGolden("source_default_$suffix", compose.onNode(isDialog()))
        compose.onNodeWithText("Fixture option").performClick()
        assertTrue(option.state)
        assertEquals(1, updates.get())
        assertModernPilotGolden("source_selected_$suffix", compose.onNode(isDialog()))
        compose.onNodeWithText(resetLabel).performClick()
        assertEquals(1, resets.get())
        assertEquals(0, applied.get())
        compose.onNodeWithText(applyLabel).performClick()
        assertEquals(1, applied.get())
    }

    @Test
    fun librarySheetWrapperKeepsTabsAndDisabledState() {
        render {
            TabbedDialog(
                onDismissRequest = {},
                tabTitles = listOf("Filter", "Sort", "Display", "Group"),
                modifier = Modifier.testTag("sheet"),
            ) { page ->
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Text("Page $page")
                    TriStateItem(label = "Unavailable option", state = TriState.DISABLED, enabled = false, onClick = {})
                }
            }
        }
        compose.onNodeWithText("Unavailable option").assertIsDisplayed().assertIsNotEnabled()
        val bounds = compose.onNodeWithTag("sheet").fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.width <= 460f * compose.density.density + 1f)
        assertModernPilotGolden("sheet_filter_$suffix", compose.onNode(isDialog()))
        compose.onNodeWithText("Sort").performClick()
        compose.onNodeWithText("Sort").assertIsSelected()
        compose.onNodeWithText("Page 1").assertIsDisplayed()
        assertModernPilotGolden("sheet_sort_$suffix", compose.onNode(isDialog()))
    }

    @Test
    fun globalSearchKeepsInputSubmitAndSourceFilterState() {
        val submitted = mutableListOf<String>()
        var sourceFilter by mutableStateOf(SourceFilter.PinnedOnly)
        lateinit var allLabel: String
        render {
            var query: String? by remember { mutableStateOf("") }
            allLabel = stringResource(MR.strings.all)
            GlobalSearchToolbar(
                searchQuery = query, progress = 0, total = 0, navigateUp = {},
                onChangeSearchQuery = { query = it }, onSearch = { submitted += it },
                searchScope = GlobalSearchScope.Sources,
                hideSourceFilter = false, sourceFilter = sourceFilter,
                onChangeSearchFilter = { sourceFilter = it },
                onlyShowHasResults = false, onToggleResults = {},
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
            )
        }
        compose.onNode(hasSetTextAction()).performTextInput("Neutral fixture")
        compose.onNode(hasSetTextAction()).performImeAction()
        assertEquals(listOf("Neutral fixture"), submitted)
        compose.onAllNodesWithText(allLabel).onLast().performClick()
        assertEquals(SourceFilter.All, sourceFilter)
        assertModernPilotGolden("global_search_$suffix", compose.onRoot())
    }

    @Test
    fun globalSearchShortQueryPaintsCompleteLine() {
        render {
            GlobalSearchToolbar(
                searchQuery = "Query", progress = 0, total = 0, navigateUp = {},
                onChangeSearchQuery = {}, onSearch = {},
                hideSourceFilter = true, sourceFilter = SourceFilter.All, onChangeSearchFilter = {},
                onlyShowHasResults = false, onToggleResults = {},
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
            )
        }
        val field = compose.onNode(hasSetTextAction())
        val layouts = mutableListOf<TextLayoutResult>()
        field.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val text = layouts.single()
        assertTrue(
            "Query paragraph is clipped: ${text.multiParagraph.height}px in ${text.size.height}px",
            text.size.height >= text.multiParagraph.height - 1f,
        )
        assertModernPilotGolden("global_short_query_$suffix", compose.onRoot())
    }

    private fun render(content: @Composable () -> Unit) {
        compose.setContent {
            TachiyomiPreviewTheme(designStyle = DesignStyle.MODERN) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
            }
        }
    }

    private fun cover(index: Int) = AnimeCover(index.toLong(), 0, false, "fixture://cover/$index", 0)

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}dp dark={1} font={2}")
        fun fixtures() = listOf(
            arrayOf<Any>(432, false, 1f),
            arrayOf<Any>(432, true, 1f),
            arrayOf<Any>(320, false, 2f),
            arrayOf<Any>(320, true, 2f),
        )
    }
}
