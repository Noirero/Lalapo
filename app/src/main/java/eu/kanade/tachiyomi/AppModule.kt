package eu.kanade.tachiyomi

import android.app.Application
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.squareup.sqldelight.android.AndroidSqliteDriver
import dataanime.Animehistory
import dataanime.Animes
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.anime.store.DelayedAnimeTrackingStore
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.preference.AndroidPreferenceStore
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.cache.AnimeCoverCache
import eu.kanade.tachiyomi.data.cache.EpisodeCache
import eu.kanade.tachiyomi.data.connections.ConnectionsManager
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadCache
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadManager
import eu.kanade.tachiyomi.data.download.anime.AnimeDownloadProvider
import eu.kanade.tachiyomi.data.library.anime.CustomAnimeManager
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager
import eu.kanade.tachiyomi.network.JavaScriptEngine
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.source.anime.AndroidAnimeSourceManager
import eu.kanade.tachiyomi.ui.player.ExternalIntents
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.util.system.isDevFlavor
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.UnknownChildHandler
import nl.adaptivity.xmlutil.serialization.XML
import tachiyomi.core.preference.PreferenceStore
import tachiyomi.core.provider.AndroidBackupFolderProvider
import tachiyomi.core.provider.AndroidDownloadFolderProvider
import tachiyomi.data.Database
import tachiyomi.data.dateAdapter
import tachiyomi.data.handlers.anime.AndroidAnimeDatabaseHandler
import tachiyomi.data.handlers.anime.AnimeDatabaseHandler
import tachiyomi.data.handlers.manga.AndroidMangaDatabaseHandler
import tachiyomi.data.handlers.manga.MangaDatabaseHandler
import tachiyomi.data.listOfStringsAdapter
import tachiyomi.data.updateStrategyAdapter
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.domain.source.manga.service.MangaSourceManager
import tachiyomi.mi.data.AnimeDatabase
import tachiyomi.source.local.image.anime.LocalAnimeCoverManager
import tachiyomi.source.local.image.manga.LocalMangaCoverManager
import tachiyomi.source.local.io.anime.LocalAnimeSourceFileSystem
import tachiyomi.source.local.io.manga.LocalMangaSourceFileSystem
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

class AppModule(val app: Application) : InjektModule {

    override fun InjektRegistrar.registerInjectables() {
        addSingleton(app)

        val sqlDriverAnime = AndroidSqliteDriver(
            schema = AnimeDatabase.Schema,
            context = app,
            name = "tachiyomi.animedb",
            factory = if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Support database inspector in Android Studio
                FrameworkSQLiteOpenHelperFactory()
            } else {
                RequerySQLiteOpenHelperFactory()
            },
            callback = object : AndroidSqliteDriver.Callback(AnimeDatabase.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    setPragma(db, "foreign_keys = ON")
                    setPragma(db, "journal_mode = WAL")
                    setPragma(db, "synchronous = NORMAL")
                }
                private fun setPragma(db: SupportSQLiteDatabase, pragma: String) {
                    val cursor = db.query("PRAGMA $pragma")
                    cursor.moveToFirst()
                    cursor.close()
                }
            },
        )

        addSingletonFactory {
            AnimeDatabase(
                driver = sqlDriverAnime,
                animehistoryAdapter = Animehistory.Adapter(
                    last_seenAdapter = dateAdapter,
                ),
                animesAdapter = Animes.Adapter(
                    genreAdapter = listOfStringsAdapter,
                    update_strategyAdapter = updateStrategyAdapter,
                ),
            )
        }

        addSingletonFactory<AnimeDatabaseHandler> { AndroidAnimeDatabaseHandler(get(), sqlDriverAnime) }

        addSingletonFactory {
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
        }
        addSingletonFactory {
            XML {
                unknownChildHandler = UnknownChildHandler { _, _, _, _, _ -> emptyList() }
                autoPolymorphic = true
                xmlDeclMode = XmlDeclMode.Charset
                indent = 4
                xmlVersion = XmlVersion.XML10
            }
        }

        addSingletonFactory { EpisodeCache(app) }

        addSingletonFactory { AnimeCoverCache(app) }

        addSingletonFactory { NetworkHelper(app, get()) }
        addSingletonFactory { JavaScriptEngine(app) }

        addSingletonFactory<AnimeSourceManager> { AndroidAnimeSourceManager(app, get(), get()) }

        addSingletonFactory { AnimeExtensionManager(app) }

        addSingletonFactory { AnimeDownloadProvider(app) }
        addSingletonFactory { AnimeDownloadManager(app) }
        addSingletonFactory { AnimeDownloadCache(app) }

        addSingletonFactory { TrackManager(app) }
        addSingletonFactory { DelayedAnimeTrackingStore(app) }

        addSingletonFactory { ImageSaver(app) }

        addSingletonFactory { LocalMangaSourceFileSystem(app) }
        addSingletonFactory { LocalMangaCoverManager(app, get()) }

        addSingletonFactory { LocalAnimeSourceFileSystem(app) }
        addSingletonFactory { LocalAnimeCoverManager(app, get()) }

        addSingletonFactory { ExternalIntents() }

        // AM (CN) -->
        addSingletonFactory { ConnectionsManager(app) }
        // AM (CN) <--

        // AM (CU) -->
        addSingletonFactory { CustomAnimeManager(app) }
        // <-- AM (CU)

        // Asynchronously init expensive components for a faster cold start
        ContextCompat.getMainExecutor(app).execute {
            get<NetworkHelper>()

            get<AnimeSourceManager>()

            get<AnimeDatabase>()

            get<AnimeDownloadManager>()

            // AM (CU) -->
            get<CustomAnimeManager>()
            // <-- AM (CU)
        }
    }
}

class PreferenceModule(val application: Application) : InjektModule {
    override fun InjektRegistrar.registerInjectables() {
        addSingletonFactory<PreferenceStore> {
            AndroidPreferenceStore(application)
        }
        addSingletonFactory {
            NetworkPreferences(
                preferenceStore = get(),
                verboseLogging = isDevFlavor,
            )
        }
        addSingletonFactory {
            SourcePreferences(get())
        }
        addSingletonFactory {
            SecurityPreferences(get())
        }
        addSingletonFactory {
            LibraryPreferences(get())
        }
        addSingletonFactory {
            PlayerPreferences(get())
        }
        addSingletonFactory {
            TrackPreferences(get())
        }
        addSingletonFactory {
            AndroidDownloadFolderProvider(application)
        }
        addSingletonFactory {
            DownloadPreferences(
                folderProvider = get<AndroidDownloadFolderProvider>(),
                preferenceStore = get(),
            )
        }
        addSingletonFactory {
            AndroidBackupFolderProvider(application)
        }
        addSingletonFactory {
            BackupPreferences(
                folderProvider = get<AndroidBackupFolderProvider>(),
                preferenceStore = get(),
            )
        }
        addSingletonFactory {
            UiPreferences(get())
        }
        addSingletonFactory {
            BasePreferences(application, get())
        }
        // AM (CN) -->
        addSingletonFactory {
            ConnectionsPreferences(get())
        }
        // AM (CN) <--
    }
}
