package eu.kanade.presentation.theme

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ModernNavigationSurfaceTest {

    @Test
    fun `Classic keeps default NavigationSuite container and Modern uses semantic surface`() {
        val definitions = ModernThemeDefinitions.authoredBuiltIns + ModernThemeDefinitions.custom(
            ModernCustomColors.fromStored("#EE6600", "", ""),
        )
        for (definition in definitions) {
            for ((dark, amoled) in listOf(false to false, true to false, true to true)) {
                val scheme = ModernThemeResolver.resolve(definition, dark, amoled)
                assertNull(modernNavigationContainerColor(scheme, isModern = false))
                assertEquals(
                    scheme.surfaceContainerHigh,
                    modernNavigationContainerColor(scheme, isModern = true),
                )
                assertEquals(
                    scheme.surfaceContainerHigh.copy(alpha = 0f),
                    modernNavigationContainerColor(scheme, isModern = true, useGlass = true),
                )
                if (dark && amoled) {
                    assertEquals(Color.Black, scheme.background)
                }
            }
        }
    }
}
