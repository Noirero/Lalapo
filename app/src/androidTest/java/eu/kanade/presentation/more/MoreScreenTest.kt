package eu.kanade.presentation.more

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MoreScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun togglesAndRoutesUseTheirOwnCallbacks() {
        val privateSession = mutableStateOf(false)
        val offline = mutableStateOf(false)
        val sync = mutableStateOf(SyncHealthState())
        val extensions = mutableStateOf(ExtensionHealthState())
        val storage = mutableStateOf(StorageHealthState())
        val destinations = mutableListOf<String>()
        compose.setContent {
            TachiyomiPreviewTheme {
                MoreScreen(
                    downloadQueueStateProvider = { DownloadQueueState.Stopped },
                    downloadedOnly = offline.value,
                    onDownloadedOnlyChange = { offline.value = it },
                    incognitoMode = privateSession.value,
                    onIncognitoModeChange = { privateSession.value = it },
                    syncHealth = sync.value,
                    extensionHealth = extensions.value,
                    storageHealth = storage.value,
                    onClickDownloadQueue = { destinations += "queue" },
                    onClickSyncAndBackup = { destinations += "sync settings" },
                    onSyncNow = { destinations += "sync now" },
                    onClickExtensionHealth = { destinations += "extensions" },
                    onClickStorageHealth = { destinations += "storage" },
                    onClickStats = { destinations += "stats" },
                    onClickSettings = { destinations += "settings" },
                    onClickAbout = { destinations += "about" },
                )
            }
        }

        compose.onNodeWithText("Pauses watching history.").assertExists()
        compose.onNodeWithText("Filters all entries in your library.").assertExists()
        compose.onNodeWithText("Private session").assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText("Offline mode").assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText("Private session").performClick().assertIsOff()
        compose.onNodeWithText("Offline mode").performClick().assertIsOff()

        clickRow("Download queue")
        compose.onNodeWithText("Sync is not configured").assertExists()
        compose.onNodeWithText("Source & extension health").assertDoesNotExist()
        compose.runOnIdle {
            sync.value = SyncHealthState(enabled = true, provider = SyncProvider.GoogleDrive)
            extensions.value = ExtensionHealthState(untrusted = 7)
            storage.value = StorageHealthState(isLow = true)
        }
        clickRow("Sync & backup")
        compose.onNodeWithContentDescription("Sync now").performClick()
        clickRow("Source & extension health")
        compose.onNodeWithText("7 untrusted extensions need attention").assertExists()
        clickRow("Low storage")
        clickRow("Statistics")
        clickRow("Settings")
        clickRow("About")

        compose.runOnIdle {
            assertEquals(
                listOf("queue", "sync settings", "sync now", "extensions", "storage", "stats", "settings", "about"),
                destinations,
            )
        }
    }

    private fun clickRow(label: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(label))
        compose.onNode(hasText(label) and hasClickAction()).performClick()
    }
}
