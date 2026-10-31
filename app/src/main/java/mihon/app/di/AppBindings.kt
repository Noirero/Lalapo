package mihon.app.di

import android.content.Context
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.sqldelight.db.SqlDriver
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteConfiguration
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteDatabaseType
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteDriver
import com.eygraber.sqldelight.androidx.driver.FileProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.XML
import tachiyomi.data.Animes
import tachiyomi.data.Database
import tachiyomi.data.DateColumnAdapter
import tachiyomi.data.Episodes
import tachiyomi.data.FetchTypeColumnAdapter
import tachiyomi.data.History
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.UpdateStrategyColumnAdapter

@BindingContainer
object AppBindings {

    @Provides
    @SingleIn(AppScope::class)
    fun providesSqlDriver(context: Context): SqlDriver {
        return AndroidxSqliteDriver(
            driver = BundledSQLiteDriver(),
            databaseType = AndroidxSqliteDatabaseType.FileProvider(context, "tachiyomi.animedb"),
            schema = Database.Schema,
            configuration = AndroidxSqliteConfiguration(
                isForeignKeyConstraintsEnabled = true,
            ),
            // The SQLDelight parser cannot compile triggers referencing NEW.
            // Install these idempotently on every open, after create/migrate.
            // This callback is serialized by AndroidxSqliteDriver initialization.
            onOpen = {
                execute(
                    null,
                    """
                    CREATE TRIGGER IF NOT EXISTS category_set_auto_assign
                    AFTER INSERT ON categories WHEN NEW._id > 0
                    BEGIN
                        INSERT INTO category_set_assignments(category_id, set_id)
                        SELECT NEW._id, _id FROM category_sets ORDER BY sort, _id LIMIT 1;
                        SELECT CASE WHEN NOT EXISTS (
                            SELECT 1 FROM category_set_assignments WHERE category_id = NEW._id
                        ) THEN RAISE(ABORT, 'Missing category Set') END;
                    END
                    """.trimIndent(),
                    0,
                ).await()
                execute(
                    null,
                    """
                    CREATE TRIGGER IF NOT EXISTS category_set_prevent_last_delete
                    BEFORE DELETE ON category_sets
                    WHEN (SELECT count(*) FROM category_sets) <= 1
                    BEGIN
                        SELECT RAISE(ABORT, 'Cannot delete last Set');
                    END
                    """.trimIndent(),
                    0,
                ).await()
            },
        )
    }

    @Provides
    @SingleIn(AppScope::class)
    fun providesDatabase(driver: SqlDriver): Database {
        return Database(
            driver = driver,
            historyAdapter = History.Adapter(
                last_seenAdapter = DateColumnAdapter,
            ),
            animesAdapter = Animes.Adapter(
                genreAdapter = StringListColumnAdapter,
                update_strategyAdapter = UpdateStrategyColumnAdapter,
                // AY -->
                fetch_typeAdapter = FetchTypeColumnAdapter,
                // <-- AY
                memoAdapter = MemoColumnAdapter,
            ),
            episodesAdapter = Episodes.Adapter(
                memoAdapter = MemoColumnAdapter,
            ),
        )
    }

    @Provides
    @SingleIn(AppScope::class)
    fun providesJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @SingleIn(AppScope::class)
    fun providesXML(): XML = XML.v1 {
        policy {
            ignoreUnknownChildren()
            autoPolymorphic = true
        }
        xmlDeclMode = XmlDeclMode.Charset
        xmlVersion = XmlVersion.XML10
        setIndent(2)
    }

    @Provides
    @SingleIn(AppScope::class)
    fun providesProtoBuf(): ProtoBuf = ProtoBuf
}
