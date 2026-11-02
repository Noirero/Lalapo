package eu.kanade.presentation.more.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.widget.AppearanceSelectionWidget
import mihon.app.di.appGraph

internal class ThemeStep : OnboardingStep {
    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val uiPreferences = remember { context.appGraph.uiPreferences }
        AppearanceSelectionWidget(uiPreferences)
    }
}
