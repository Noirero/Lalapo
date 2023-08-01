// AM (BROWSE) -->
package eu.kanade.tachiyomi.ui.browse.anime

import eu.kanade.tachiyomi.extension.InstallStep
import eu.kanade.tachiyomi.extension.anime.model.AnimeExtension
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import rx.Observable

class AnimeSourceExtensionFunctions {
    companion object {
        var currentDownloads = MutableStateFlow<Map<String, InstallStep>>(hashMapOf())

        private fun removeDownloadState(extension: AnimeExtension) {
            currentDownloads.update { _map ->
                val map = _map.toMutableMap()
                map.remove(extension.pkgName)
                map
            }
        }

        private fun addDownloadState(extension: AnimeExtension, installStep: InstallStep) {
            currentDownloads.update { _map ->
                val map = _map.toMutableMap()
                map[extension.pkgName] = installStep
                map
            }
        }

        fun Observable<InstallStep>.subscribeToInstallUpdate(extension: AnimeExtension) {
            this
                .doOnUnsubscribe { removeDownloadState(extension) }
                .subscribe(
                    { installStep -> addDownloadState(extension, installStep) },
                    { removeDownloadState(extension) },
                )
        }
    }
}
// <-- AM (BROWSE)
