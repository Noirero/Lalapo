// AM (BROWSE) -->
package eu.kanade.tachiyomi.ui.browse.anime

import eu.kanade.tachiyomi.extension.InstallStep
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class AnimeSourceExtensionFunctions {
    companion object {
        var currentDownloads = MutableStateFlow<Map<String, InstallStep>>(hashMapOf())

        private fun addDownloadState(extension: AnimeExtension, installStep: InstallStep) {
            currentDownloads.update { it + Pair(extension.pkgName, installStep) }
        }

        private fun removeDownloadState(extension: AnimeExtension) {
            currentDownloads.update { it - extension.pkgName }
        }

        suspend fun Flow<InstallStep>.collectToInstallUpdate(extension: AnimeExtension) =
            this
                .onEach { installStep -> addDownloadState(extension, installStep) }
                .onCompletion { removeDownloadState(extension) }
                .collect()
    }
}
// <-- AM (BROWSE)
