// AM -->
package eu.kanade.tachiyomi.data.animelib

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Anime
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import eu.kanade.domain.anime.model.Anime as DomainAnime

class CustomAnimeManager(val context: Context) {

    private val editJson = File(context.getExternalFilesDir(null), "edits.json")

    private val customAnimeMap = fetchCustomData()

    fun getAnime(anime: Anime): CustomAnimeInfo? = customAnimeMap[anime.id]
    fun getAnime(anime: DomainAnime): CustomAnimeInfo? = customAnimeMap[anime.id]
    fun getAnime(animeId: Long): CustomAnimeInfo? = customAnimeMap[animeId]

    private fun fetchCustomData(): MutableMap<Long, CustomAnimeInfo> {
        if (!editJson.exists() || !editJson.isFile) return mutableMapOf()

        val json = try {
            Json.decodeFromString<AnimeList>(
                editJson.bufferedReader().use { it.readText() },
            )
        } catch (e: Exception) {
            null
        } ?: return mutableMapOf()

        val animesJson = json.animes ?: return mutableMapOf()
        return animesJson
            .mapNotNull { animeJson ->
                val id = animeJson.id ?: return@mapNotNull null
                id to animeJson.toAnime()
            }
            .toMap()
            .toMutableMap()
    }

    fun saveAnimeInfo(anime: AnimeJson) {
        if (
            anime.title == null &&
            anime.author == null &&
            anime.artist == null &&
            anime.description == null &&
            anime.genre == null &&
            anime.status == null
        ) {
            customAnimeMap.remove(anime.id!!)
        } else {
            customAnimeMap[anime.id!!] = anime.toAnime()
        }
        saveCustomInfo()
    }

    private fun saveCustomInfo() {
        val jsonElements = customAnimeMap.values.map { it.toJson() }
        if (jsonElements.isNotEmpty()) {
            editJson.delete()
            editJson.writeText(Json.encodeToString(AnimeList(jsonElements)))
        }
    }

    @Serializable
    data class AnimeList(
        val animes: List<AnimeJson>? = null,
    )

    @Serializable
    data class AnimeJson(
        var id: Long? = null,
        val title: String? = null,
        val author: String? = null,
        val artist: String? = null,
        val description: String? = null,
        val genre: List<String>? = null,
        val status: Long? = null,
    ) {

        fun toAnime() = CustomAnimeInfo(
            id = this@AnimeJson.id!!,
            title = this@AnimeJson.title?.takeUnless { it.isBlank() },
            author = this@AnimeJson.author,
            artist = this@AnimeJson.artist,
            description = this@AnimeJson.description,
            genre = this@AnimeJson.genre,
            status = this@AnimeJson.status?.takeUnless { it == 0L },
        )
    }

    data class CustomAnimeInfo(
        var id: Long,
        val title: String?,
        val author: String? = null,
        val artist: String? = null,
        val description: String? = null,
        val genre: List<String>? = null,
        val status: Long? = null,
    ) {
        val genreString by lazy {
            genre?.joinToString()
        }
        val statusLong = status?.toLong()

        fun toJson(): AnimeJson {
            return AnimeJson(
                id,
                title,
                author,
                artist,
                description,
                genre,
                status,
            )
        }
    }
}
// AM <--
