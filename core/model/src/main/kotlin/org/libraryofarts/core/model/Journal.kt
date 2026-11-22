package org.libraryofarts.core.model

import kotlinx.serialization.Serializable

@Serializable
data class JournalEntry(
    val id: String,
    val artworkId: String,
    val artworkTitle: String = "",
    val artworkImageUrl: String = "",
    val text: String,
    val mood: Mood = Mood.REFLECTIVE,
    val tags: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val audioNotePath: String? = null,
)

@Serializable
enum class Mood(val emoji: String, val label: String) {
    AWE("✨", "Awe"),
    CALM("🌿", "Calm"),
    CURIOUS("🔍", "Curious"),
    MOVED("💧", "Moved"),
    PLAYFUL("🎈", "Playful"),
    UNEASY("🌫️", "Uneasy"),
    REFLECTIVE("🕯️", "Reflective"),
    JOYFUL("☀️", "Joyful");

    companion object {
        val all: List<Mood> = entries
    }
}

@Serializable
data class JournalTimelinePoint(
    val monthKey: String,
    val label: String,
    val entries: Int,
    val topMood: Mood?,
)

@Serializable
data class EyesFirstComparison(
    val matches: List<String>,
    val originalInsights: List<String>,
    val encouragement: String,
    val artHistoricalReading: String,
    val score: Int,
    val usedOnDeviceModel: Boolean,
)
