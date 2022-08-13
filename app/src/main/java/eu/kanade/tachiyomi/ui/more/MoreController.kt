package eu.kanade.tachiyomi.ui.more

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import eu.kanade.presentation.more.MoreScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.ComposeController
import eu.kanade.tachiyomi.ui.base.controller.NoAppBarElevationController
import eu.kanade.tachiyomi.ui.base.controller.RootController
import eu.kanade.tachiyomi.ui.base.controller.pushController
import eu.kanade.tachiyomi.ui.download.DownloadController
import eu.kanade.tachiyomi.ui.recent.animehistory.AnimeHistoryController
import eu.kanade.tachiyomi.ui.recent.animeupdates.AnimeUpdatesController
import eu.kanade.tachiyomi.ui.setting.SettingsBackupController
import eu.kanade.tachiyomi.ui.setting.SettingsMainController
import eu.kanade.tachiyomi.data.connections.discord.DiscordRPCService as DRPC
import eu.kanade.tachiyomi.ui.animecategory.CategoryController as AnimeCategoryController

class MoreController :
    ComposeController<MorePresenter>(),
    RootController,
    NoAppBarElevationController {

    override fun getTitle(): String {
        // AM -->
        DRPC.setDRPC("more", resources!!)
        // AM <--
        return resources!!.getString(R.string.label_more)
    }

    override fun createPresenter() = MorePresenter()

    @Composable
    override fun ComposeContent(nestedScrollInterop: NestedScrollConnection) {
        MoreScreen(
            nestedScrollInterop = nestedScrollInterop,
            presenter = presenter,
            onClickDownloadQueue = { router.pushController(DownloadController()) },
            onClickAnimeCategories = { router.pushController(AnimeCategoryController()) },
            onClickBackupAndRestore = { router.pushController(SettingsBackupController()) },
            onClickSettings = { router.pushController(SettingsMainController()) },
            onClickAbout = { router.pushController(AboutController()) },
            onClickUpdates = { router.pushController(AnimeUpdatesController()) },
            onClickHistory = { router.pushController(AnimeHistoryController()) },
        )
    }

    companion object {
        const val URL_HELP = "https://aniyomi.jmir.xyz/help/"
    }
}
