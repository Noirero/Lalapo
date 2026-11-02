package eu.kanade.presentation.theme

/**
 * Compatibility alias for the original PR #24 header-artwork mapping.
 *
 * Built-in Modern now uses the same full portrait for the sharp Pustaka background and the shared
 * blurred derivative, so there is no second toolbar-only artwork resource to decode.
 */
internal fun modernHeaderArtworkResource(
    isModern: Boolean,
    identity: ModernArtworkIdentity?,
): Int? = modernWallpaperResource(isModern = isModern, identity = identity)
