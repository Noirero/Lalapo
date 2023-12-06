package eu.kanade.presentation.history.anime

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import java.util.Date
import tachiyomi.domain.history.anime.model.AnimeHistoryWithRelations

internal class AnimeHistoryWithRelationsProvider : PreviewParameterProvider<AnimeHistoryWithRelations> {

    private val simple = AnimeHistoryWithRelations(
        id = 1L,
        episodeId = 2L,
        animeId = 3L,
        ogTitle = "Test Title",
        episodeNumber = 10.2,
        seenAt = Date(1697247357L),
        coverData = tachiyomi.domain.entries.anime.model.AnimeCover(
            animeId = 3L,
            sourceId = 4L,
            isAnimeFavorite = false,
            url = "https://example.com/cover.png",
            lastModified = 5L,
        ),
    )

    private val historyWithoutReadAt = AnimeHistoryWithRelations(
        id = 1L,
        episodeId = 2L,
        animeId = 3L,
        ogTitle = "Test Title",
        episodeNumber = 10.2,
        seenAt = null,
        coverData = tachiyomi.domain.entries.anime.model.AnimeCover(
            animeId = 3L,
            sourceId = 4L,
            isAnimeFavorite = false,
            url = "https://example.com/cover.png",
            lastModified = 5L,
        ),
    )

    private val historyWithNegativeChapterNumber = AnimeHistoryWithRelations(
        id = 1L,
        episodeId = 2L,
        animeId = 3L,
        ogTitle = "Test Title",
        episodeNumber = -2.0,
        seenAt = Date(1697247357L),
        coverData = tachiyomi.domain.entries.anime.model.AnimeCover(
            animeId = 3L,
            sourceId = 4L,
            isAnimeFavorite = false,
            url = "https://example.com/cover.png",
            lastModified = 5L,
        ),
    )

    override val values: Sequence<AnimeHistoryWithRelations>
        get() = sequenceOf(simple, historyWithoutReadAt, historyWithNegativeChapterNumber)
}
