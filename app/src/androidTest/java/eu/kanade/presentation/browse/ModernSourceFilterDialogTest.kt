package eu.kanade.presentation.browse

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import eu.kanade.domain.ui.model.DesignStyle
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.ui.browse.source.browse.SourceFilterDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class ModernSourceFilterDialogTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lightSourceFilterRetainsIndependentUpdateResetAndApplyCallbacks() = sourceFilter(false)

    @Test
    fun darkSourceFilterRetainsIndependentUpdateResetAndApplyCallbacks() = sourceFilter(true)

    private fun sourceFilter(dark: Boolean) {
        val updateCalls = AtomicInteger()
        val resetCalls = AtomicInteger()
        val applyCalls = AtomicInteger()
        val dismissCalls = AtomicInteger()
        val option = object : AnimeFilter.CheckBox("Fixture option") {}
        val filters = AnimeFilterList(AnimeFilter.Header("Fixture"), AnimeFilter.Separator(), option)
        lateinit var resetLabel: String
        lateinit var applyLabel: String
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                TachiyomiPreviewTheme(designStyle = DesignStyle.MODERN) {
                    resetLabel = stringResource(MR.strings.action_reset)
                    applyLabel = stringResource(MR.strings.action_filter)
                    SourceFilterDialog(
                        onDismissRequest = { dismissCalls.incrementAndGet() },
                        filters = filters,
                        onReset = { resetCalls.incrementAndGet() },
                        onFilter = { applyCalls.incrementAndGet() },
                        onUpdate = { updateCalls.incrementAndGet() },
                    )
                }
            }
        }
        compose.onNodeWithText("Fixture option").performTouchInput { click(center) }
        compose.waitForIdle()
        assertTrue(option.state)
        assertEquals(1, updateCalls.get())
        compose.onNodeWithText(resetLabel).performTouchInput { click(center) }
        compose.waitForIdle()
        assertEquals(1, resetCalls.get())
        assertEquals(0, applyCalls.get())
        assertEquals(0, dismissCalls.get())
        compose.onNodeWithText(applyLabel).performTouchInput { click(center) }
        compose.waitForIdle()
        assertEquals(1, applyCalls.get())
        assertEquals(1, dismissCalls.get())
        assertEquals(1, updateCalls.get())
    }
}
