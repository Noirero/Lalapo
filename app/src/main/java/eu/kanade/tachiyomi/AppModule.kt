package eu.kanade.tachiyomi

import android.app.Application
import androidx.core.content.ContextCompat
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.squareup.sqldelight.android.AndroidSqliteDriver
import dataanime.Animehistory
import dataanime.Animes
import eu.kanade.data.AndroidAnimeDatabaseHandler
import eu.kanade.data.AnimeDatabaseHandler
import eu.kanade.data.dateAdapter
import eu.kanade.data.listOfStringsAdapter
import eu.kanade.tachiyomi.animesource.AnimeSourceManager
import eu.kanade.tachiyomi.data.animelib.CustomAnimeManager
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.cache.EpisodeCache
import eu.kanade.tachiyomi.data.database.AnimeDatabaseHelper
import eu.kanade.tachiyomi.data.database.AnimeDbOpenCallback
import eu.kanade.tachiyomi.data.download.AnimeDownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.job.DelayedTrackingStore
import eu.kanade.tachiyomi.extension.AnimeExtensionManager
import eu.kanade.tachiyomi.mi.AnimeDatabase
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

class AppModule(val app: Application) : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingleton(app)

        // This is used to allow incremental migration from Storio
        val openHelperAnime = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(app)
                .callback(AnimeDbOpenCallback())
                .name(AnimeDbOpenCallback.DATABASE_NAME)
                .noBackupDirectory(false)
                .build(),
        )

        val sqlDriverAnime = AndroidSqliteDriver(openHelper = openHelperAnime)

        addSingletonFactory {
            AnimeDatabase(
                driver = sqlDriverAnime,
                animehistoryAdapter = Animehistory.Adapter(
                    last_seenAdapter = dateAdapter,
                ),
                animesAdapter = Animes.Adapter(
                    genreAdapter = listOfStringsAdapter,
                ),
            )
        }

        addSingletonFactory<AnimeDatabaseHandler> { AndroidAnimeDatabaseHandler(get(), sqlDriverAnime) }

        addSingletonFactory { Json { ignoreUnknownKeys = true } }

        addSingletonFactory { PreferencesHelper(app) }

        addSingletonFactory { AnimeDatabaseHelper(openHelperAnime) }

        addSingletonFactory { EpisodeCache(app) }

        addSingletonFactory { AnimeCoverCache(app) }

        addSingletonFactory { NetworkHelper(app) }

        addSingletonFactory { AnimeSourceManager(app).also { get<AnimeExtensionManager>().init(it) } }

        addSingletonFactory { AnimeExtensionManager(app) }

        addSingletonFactory { AnimeDownloadManager(app) }

        addSingletonFactory { TrackManager(app) }

        addSingletonFactory { DelayedTrackingStore(app) }

        addSingletonFactory { ImageSaver(app) }

        addSingletonFactory { CustomAnimeManager(app) }

        // Asynchronously init expensive components for a faster cold start
        ContextCompat.getMainExecutor(app).execute {
            get<PreferencesHelper>()

            get<NetworkHelper>()

            get<AnimeSourceManager>()

            get<AnimeDatabase>()

            get<AnimeDatabaseHelper>()

            get<AnimeDownloadManager>()

            get<CustomAnimeManager>()
        }
    }
}
