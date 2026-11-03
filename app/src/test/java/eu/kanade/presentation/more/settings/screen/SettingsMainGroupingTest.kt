package eu.kanade.presentation.more.settings.screen

import eu.kanade.presentation.more.settings.screen.about.AboutScreen
import eu.kanade.tachiyomi.ui.setting.PlayerSettingsScreen
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingsMainGroupingTest {

    @Test
    fun `settings destinations remain unique and grouped by their function`() {
        val expected = listOf(
            listOf("SettingsAppearanceScreen", "SettingsGeneralScreen"),
            listOf("SettingsLibraryScreen", "SettingsBrowseScreen"),
            listOf("PlayerSettingsScreen", "SettingsDownloadScreen"),
            listOf(
                "SettingsTrackingScreen",
                "SettingsSyncBackupScreen",
                "SettingsConnectionScreen",
                "SettingsNetworkScreen",
            ),
            listOf("SettingsDataScreen", "SettingsSecurityScreen", "SettingsAdvancedScreen"),
            listOf("AboutScreen"),
        )
        val actual = settingsRootDestinationNamesByGroup()
        assertEquals(expected, actual)
        assertEquals(14, actual.flatten().distinct().size)
    }

    @Test
    fun `tablet selected index accounts for all six section headers`() {
        assertEquals(1, settingsRootSelectedListIndex(SettingsAppearanceScreen::class.java))
        assertEquals(4, settingsRootSelectedListIndex(SettingsLibraryScreen::class.java))
        assertEquals(7, settingsRootSelectedListIndex(PlayerSettingsScreen::class.java))
        assertEquals(10, settingsRootSelectedListIndex(SettingsTrackingScreen::class.java))
        assertEquals(13, settingsRootSelectedListIndex(SettingsNetworkScreen::class.java))
        assertEquals(15, settingsRootSelectedListIndex(SettingsDataScreen::class.java))
        assertEquals(19, settingsRootSelectedListIndex(AboutScreen::class.java))
        assertTrue(settingsRootSelectedListIndex(String::class.java) == 0)
    }
}
