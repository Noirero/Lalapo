package eu.kanade.presentation.anime

enum class DownloadAction {
    NEXT_1_EPISODES,
    NEXT_5_EPISODES,
    NEXT_10_EPISODES,
    CUSTOM,
    UNSEEN_EPISODES,
    ALL_EPISODES
}

enum class EpisodeDownloadAction {
    START,
    START_NOW,
    CANCEL,
    DELETE,
    START_ALT,
}

enum class EditCoverAction {
    EDIT,
    DELETE,
}
