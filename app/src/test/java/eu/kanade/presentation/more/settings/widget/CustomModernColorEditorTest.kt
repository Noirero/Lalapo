package eu.kanade.presentation.more.settings.widget

import eu.kanade.presentation.theme.ModernCustomDraft
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore.InMemoryPreference

class CustomModernColorEditorTest {

    @Test
    fun `Apply normalizes and persists all fields with optional AUTO values`() {
        val primary = InMemoryPreference("custom_primary", "#485CE0", "#485CE0")
        val secondary = InMemoryPreference("custom_secondary", "", "")
        val tertiary = InMemoryPreference("custom_tertiary", "", "")

        assertTrue(
            applyCustomColorDraft(
                ModernCustomDraft("aabbcc", "#bb0022", null),
                primary,
                secondary,
                tertiary,
            ),
        )
        assertEquals("#AABBCC", primary.get())
        assertEquals("#BB0022", secondary.get())
        assertEquals("", tertiary.get())
        assertTrue(applyCustomColorDraft(ModernCustomDraft("#112233", null, null), primary, secondary, tertiary))
        assertEquals("#112233", primary.get())
        assertEquals("", secondary.get())
        assertEquals("", tertiary.get())
    }

    @Test
    fun `Cancel cannot commit and invalid HEX Apply makes no partial changes`() {
        val primary = InMemoryPreference("custom_primary", "#485CE0", "#485CE0")
        val secondary = InMemoryPreference("custom_secondary", "#66AACC", "")
        val tertiary = InMemoryPreference("custom_tertiary", "", "")

        // Dialog edits its local immutable draft; no Apply means no setter runs.
        val draft = ModernCustomDraft(primary.get(), secondary.get(), null)
            .copy(primary = "#AE5400", tertiaryOverride = "#FFFFFF")
        assertTrue(draft.isValid)
        assertEquals("#485CE0", primary.get())
        assertEquals("#66AACC", secondary.get())
        assertEquals("", tertiary.get())

        assertFalse(
            applyCustomColorDraft(
                draft.copy(secondaryOverride = "invalid"),
                primary,
                secondary,
                tertiary,
            ),
        )
        assertEquals("#485CE0", primary.get())
        assertEquals("#66AACC", secondary.get())
        assertEquals("", tertiary.get())
    }
}
