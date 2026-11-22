package org.libraryofarts.core.data.repository

import org.libraryofarts.core.common.TimeProvider
import org.libraryofarts.core.data.mapper.ArtworkJson
import org.libraryofarts.core.database.JournalDao
import org.libraryofarts.core.database.JournalEntryEntity
import org.libraryofarts.core.model.JournalEntry
import org.libraryofarts.core.model.JournalTimelinePoint
import org.libraryofarts.core.model.Mood
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class JournalStats(
    val totalEntries: Int = 0,
    val distinctArtworks: Int = 0,
    val topMoods: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
)

/**
 * The journal never leaves the device unless the user explicitly exports it.
 * Entries are stored in the encrypted Room database.
 */
@Singleton
class JournalRepository @Inject constructor(
    private val dao: JournalDao,
    private val artworkRepository: ArtworkRepository,
    private val time: TimeProvider,
) {
    val entries: Flow<List<JournalEntry>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    fun search(query: String): Flow<List<JournalEntry>> =
        dao.search(query).map { list -> list.map { it.toModel() } }

    fun forArtwork(artworkId: String): Flow<List<JournalEntry>> =
        dao.forArtwork(artworkId).map { list -> list.map { it.toModel() } }

    fun byMood(mood: Mood): Flow<List<JournalEntry>> =
        dao.byMood(mood.name).map { list -> list.map { it.toModel() } }

    fun byTag(tag: String): Flow<List<JournalEntry>> =
        dao.byTag(tag).map { list -> list.map { it.toModel() } }

    suspend fun save(entry: JournalEntry) {
        val now = time.nowMillis()
        val stored = entry.copy(updatedAt = now)
        dao.upsert(stored.toEntity())
        artworkRepository.getArtwork(entry.artworkId)?.let { artworkRepository.recordJournal(it) }
        if (entry.text.isBlank()) {
            dao.delete(stored.toEntity())
        }
    }

    suspend fun saveInterpretation(
        artworkId: String,
        artworkTitle: String,
        imageUrl: String,
        text: String,
        mood: Mood,
        tags: List<String>,
    ): String {
        val id = "journal-${time.nowMillis()}-${(1000..9999).random()}"
        save(
            JournalEntry(
                id = id,
                artworkId = artworkId,
                artworkTitle = artworkTitle,
                artworkImageUrl = imageUrl,
                text = text.trim(),
                mood = mood,
                tags = tags.distinct().take(8),
                createdAt = time.nowMillis(),
            ),
        )
        return id
    }

    suspend fun delete(entry: JournalEntry) = dao.delete(entry.toEntity())

    suspend fun stats(): JournalStats {
        val all = dao.all().map { it.toModel() }
        val moods = all.groupingBy { it.mood }.eachCount().entries
            .sortedByDescending { it.value }
            .take(3)
            .map { "${it.key.emoji} ${it.key.label}" }
        val tags = all.flatMap { it.tags }.groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }
            .take(12)
            .map { it.key }
        return JournalStats(
            totalEntries = all.size,
            distinctArtworks = all.map { it.artworkId }.distinct().size,
            topMoods = moods,
            tags = tags,
        )
    }

    suspend fun timeline(): List<JournalTimelinePoint> {
        val all = dao.all()
        return all.groupBy { org.libraryofarts.core.common.DateFormats.monthKey(it.createdAt) }
            .toSortedMap(compareByDescending { it })
            .map { (month, entries) ->
                val topMood = entries.groupingBy { it.mood }.eachCount().entries
                    .maxByOrNull { it.value }?.key
                    ?.let { runCatching { Mood.valueOf(it) }.getOrNull() }
                JournalTimelinePoint(
                    monthKey = month,
                    label = java.time.YearMonth.parse(month)
                        .month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()) +
                        " " + java.time.YearMonth.parse(month).year,
                    entries = entries.size,
                    topMood = topMood,
                )
            }
    }

    private fun JournalEntryEntity.toModel(): JournalEntry = JournalEntry(
        id = id,
        artworkId = artworkId,
        artworkTitle = artworkTitle,
        artworkImageUrl = artworkImageUrl,
        text = text,
        mood = runCatching { Mood.valueOf(mood) }.getOrDefault(Mood.REFLECTIVE),
        tags = tagsCsv.split('|').filter { it.isNotBlank() },
        createdAt = createdAt,
        updatedAt = updatedAt,
        audioNotePath = audioNotePath,
    )

    private fun JournalEntry.toEntity(): JournalEntryEntity = JournalEntryEntity(
        id = id,
        artworkId = artworkId,
        artworkTitle = artworkTitle,
        artworkImageUrl = artworkImageUrl,
        text = text,
        mood = mood.name,
        tagsCsv = tags.joinToString("|"),
        createdAt = createdAt,
        updatedAt = updatedAt,
        audioNotePath = audioNotePath,
    )
}
