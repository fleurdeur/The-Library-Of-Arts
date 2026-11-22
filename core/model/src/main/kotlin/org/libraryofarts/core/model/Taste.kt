package org.libraryofarts.core.model

import kotlinx.serialization.Serializable

/**
 * Signals are produced entirely on-device. Nothing here is ever uploaded.
 * Weights decay exponentially (half-life configurable in the taste engine).
 */
@Serializable
data class TasteSignal(
    val id: String,
    val artworkId: String,
    val kind: TasteSignalKind,
    val genre: String? = null,
    val artistId: String? = null,
    val artistName: String? = null,
    val period: String? = null,
    val museumId: String? = null,
    val mood: String? = null,
    val value: Float = 0.05f,
    val dwellMillis: Long = 0,
    val createdAt: Long,
)

@Serializable
enum class TasteSignalKind(val baseWeight: Float) {
    VIEW(0.05f),
    DWELL(0.15f),
    ZOOM_REGION(0.10f),
    SAVE(0.55f),
    SHARE(0.45f),
    JOURNAL(0.65f),
    COLLECTION_ADD(0.50f),
    LIKE(0.35f),
    SKIP(-0.20f),
    HIDE(-0.40f);

    val isPositive: Boolean get() = this != SKIP && this != HIDE
}

@Serializable
data class Interest(
    val key: String,
    val label: String,
    val dimension: TasteDimension,
    val weight: Float,
    val updatedAt: Long,
    val evidence: Int,
    val pinned: Boolean = false,
    val muted: Boolean = false,
)

@Serializable
enum class TasteDimension(val label: String) {
    GENRE("Genre"),
    ARTIST("Artist"),
    PERIOD("Period"),
    MOVEMENT("Movement"),
    MUSEUM("Museum"),
    MOOD("Mood"),
    MEDIUM("Medium");

    val plainLanguage: String
        get() = when (this) {
            GENRE -> "subjects and kinds of pictures"
            ARTIST -> "artists"
            PERIOD -> "periods of history"
            MOVEMENT -> "art movements"
            MUSEUM -> "museums"
            MOOD -> "moods"
            MEDIUM -> "materials"
        }
}

@Serializable
data class TasteProfile(
    val interests: List<Interest> = emptyList(),
    val discoveryRatio: Float = 0.25f,
    val learningEnabled: Boolean = true,
    val lastUpdated: Long = 0,
    val totalSignals: Int = 0,
    val topMoods: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = interests.isEmpty()
    val topInterests: List<Interest> get() = interests.sortedByDescending { it.weight }.take(6)

    fun plainLanguageSummary(): String {
        if (isEmpty) {
            return "I haven't learned a taste yet. As you look at art, I'll notice the genres, artists and periods you linger on."
        }
        val groups = interests
            .filter { it.weight > 0.05f && !it.muted }
            .sortedByDescending { it.weight }
            .groupBy { it.dimension }
            .map { (dimension, values) ->
                val names = values.take(2).joinToString(" and ") { it.label }
                if (values.size > 2) "you keep returning to $names in ${dimension.plainLanguage}" else "you enjoy $names (${dimension.plainLanguage})"
            }
        val discovery = (discoveryRatio * 100).toInt()
        return "I've noticed that " + groups.take(3).joinToString("; ") +
            ". I also keep about $discovery% of your feed for things outside this taste, so your eye keeps growing."
    }
}

@Serializable
data class FeedItem(
    val artwork: Artwork,
    val reason: FeedReason,
    val explanation: String,
    val score: Float,
)

@Serializable
enum class FeedReason(val label: String) {
    CONTINUE("Continue exploring"),
    BECAUSE_YOU_VIEWED("Because you viewed"),
    ON_THIS_DAY("On this day in art history"),
    TRENDING("Trending in museums this week"),
    RECOMMENDED("Chosen for your taste"),
    DISCOVERY("A little discovery"),
    NEW_IN_PACK("New in your offline pack"),
    HIDDEN_GEM("A hidden gem");
}

@Serializable
data class StreakState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastViewedEpochDay: Long = 0,
    val totalDays: Int = 0,
)
