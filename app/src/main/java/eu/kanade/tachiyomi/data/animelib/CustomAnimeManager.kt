package eu.kanade.tachiyomi.data.animelib

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Anime
import eu.kanade.tachiyomi.data.database.models.AnimeImpl
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class CustomAnimeManager(val context: Context) {

    private val editJson = File(context.getExternalFilesDir(null), "edits.json")

    private val customAnimeMap = fetchCustomData()

    fun getAnime(anime: Anime): Anime? = customAnimeMap[anime.id]

    private fun fetchCustomData(): MutableMap<Long, Anime> {
        if (!editJson.exists() || !editJson.isFile) return mutableMapOf()

        val json = try {
            Json.decodeFromString<AnimeList>(
                editJson.bufferedReader().use { it.readText() }
            )
        } catch (e: Exception) {
            null
        } ?: return mutableMapOf()

        val animesJson = json.animes ?: return mutableMapOf()
        return animesJson.mapNotNull { animeJson ->
            val id = animeJson.id ?: return@mapNotNull null
            id to animeJson.toAnime()
        }.toMap().toMutableMap()
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

    private fun Anime.toJson(): AnimeJson {
        return AnimeJson(
            id!!,
            title,
            author,
            artist,
            description,
            genre?.split(", "),
            status
        )
    }

    @Serializable
    data class AnimeList(
        val animes: List<AnimeJson>? = null
    )

    @Serializable
    data class AnimeJson(
        var id: Long? = null,
        val title: String? = null,
        val author: String? = null,
        val artist: String? = null,
        val description: String? = null,
        val genre: List<String>? = null,
        val status: Int? = null
    ) {

        fun toAnime() = AnimeImpl().apply {
            id = this@AnimeJson.id
            title = this@AnimeJson.title ?: ""
            author = this@AnimeJson.author
            artist = this@AnimeJson.artist
            description = this@AnimeJson.description
            genre = this@AnimeJson.genre?.joinToString(", ")
            status = this@AnimeJson.status ?: 0
        }
    }
}
