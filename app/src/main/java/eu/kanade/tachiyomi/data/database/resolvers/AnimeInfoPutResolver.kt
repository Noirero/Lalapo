package eu.kanade.tachiyomi.data.database.resolvers

import androidx.core.content.contentValuesOf
import com.pushtorefresh.storio.sqlite.StorIOSQLite
import com.pushtorefresh.storio.sqlite.operations.put.PutResolver
import com.pushtorefresh.storio.sqlite.operations.put.PutResult
import com.pushtorefresh.storio.sqlite.queries.UpdateQuery
import eu.kanade.tachiyomi.data.database.inTransactionReturn
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.tables.AnimeTable

class AnimeInfoPutResolver(val reset: Boolean = false) : PutResolver<Anime>() {

    override fun performPut(db: StorIOSQLite, anime: Anime) = db.inTransactionReturn {
        val updateQuery = mapToUpdateQuery(anime)
        val contentValues = if (reset) resetToContentValues(anime) else mapToContentValues(anime)

        val numberOfRowsUpdated = db.lowLevel().update(updateQuery, contentValues)
        PutResult.newUpdateResult(numberOfRowsUpdated, updateQuery.table())
    }

    fun mapToUpdateQuery(anime: Anime) = UpdateQuery.builder()
        .table(AnimeTable.TABLE)
        .where("${AnimeTable.COL_ID} = ?")
        .whereArgs(anime.id)
        .build()

    fun mapToContentValues(anime: Anime) = contentValuesOf(
        AnimeTable.COL_TITLE to anime.originalTitle,
        AnimeTable.COL_GENRE to anime.originalGenre,
        AnimeTable.COL_AUTHOR to anime.originalAuthor,
        AnimeTable.COL_ARTIST to anime.originalArtist,
        AnimeTable.COL_DESCRIPTION to anime.originalDescription,
        AnimeTable.COL_STATUS to anime.originalStatus
    )

    private fun resetToContentValues(anime: Anime) = contentValuesOf(
        AnimeTable.COL_TITLE to anime.title.split(splitter).last(),
        AnimeTable.COL_GENRE to anime.genre?.split(splitter)?.lastOrNull(),
        AnimeTable.COL_AUTHOR to anime.author?.split(splitter)?.lastOrNull(),
        AnimeTable.COL_ARTIST to anime.artist?.split(splitter)?.lastOrNull(),
        AnimeTable.COL_DESCRIPTION to anime.description?.split(splitter)?.lastOrNull(),
        AnimeTable.COL_STATUS to anime.status.toString()?.split(splitter)?.lastOrNull()
    )

    companion object {
        const val splitter = "▒ ▒∩▒"
    }
}
