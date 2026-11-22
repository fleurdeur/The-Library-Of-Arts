package org.libraryofarts.core.common

/**
 * Feature flags. Anything experimental can be switched off without a release.
 * Defaults are conservative: every user gets the safe Compose path.
 */
data class FeatureFlags(
    val gpuTileRenderer: Boolean = false,
    val onDeviceLanguageModel: Boolean = true,
    val arWallPreview: Boolean = true,
    val castGallery: Boolean = true,
    val liveWallpaper: Boolean = true,
    val kidsMode: Boolean = true,
    val scanPainting: Boolean = true,
    val storyTours: Boolean = true,
    val widgetGallery: Boolean = true,
    val debugStrictMode: Boolean = true,
    val packDeltaUpdates: Boolean = true,
) {
    companion object {
        val Default = FeatureFlags()
    }
}

object BuildInfo {
    const val APP_NAME = "The Great Library of Arts"
    const val PACKAGE_NAME = "org.libraryofarts.app"
    const val PRIVACY_EMAIL = "privacy@libraryofarts.org"
    const val DEFAULT_CDN_BASE = "https://cdn.libraryofarts.org/packs/"
    const val DEFAULT_LLM_MODEL_FILE = "gemma-3n-E2B-it-int4.task"
}
