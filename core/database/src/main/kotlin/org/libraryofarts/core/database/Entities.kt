package org.libraryofarts.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "artworks", indices = [Index("artistName"), Index("genre"), Index("period"), Index("museumId"), Index("packId")])
data class ArtworkEntity(
    @PrimaryKey val id: String,
    val source: String,
    val sourceId: String,
    val title: String,
    val description: String,
    val artistName: String,
    val artistId: String?,
    val year: String,
    val yearStart: Int?,
    val yearEnd: Int?,
    val genre: String?,
    val period: String?,
    val movement: String?,
    val medium: String?,
    val museumId: String,
    val museumName: String,
    val moodsCsv: String,
    val subjectsCsv: String,
    val tagsCsv: String,
    val paletteHexCsv: String,
    val colorFamiliesCsv: String,
    val isHighlight: Boolean,
    val isRarelyViewed: Boolean,
    val viewCount: Long,
    val packId: String?,
    @ColumnInfo(name = "json") val json: String,
    val updatedAt: Long,
)

@Entity(tableName = "journal_entries", indices = [Index("artworkId"), Index("createdAt"), Index("mood")])
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val artworkId: String,
    val artworkTitle: String,
    val artworkImageUrl: String,
    val text: String,
    val mood: String,
    val tagsCsv: String,
    val createdAt: Long,
    val updatedAt: Long,
    val audioNotePath: String?,
)

@Entity(tableName = "collections", indices = [Index("updatedAt")])
data class CollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val coverArtworkId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isFavorites: Boolean,
    val tagsCsv: String,
)

@Entity(
    tableName = "collection_artworks",
    primaryKeys = ["collectionId", "artworkId"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("collectionId"), Index("artworkId")],
)
data class CollectionArtworkEntity(
    val collectionId: String,
    val artworkId: String,
    val addedAt: Long,
    val position: Int,
)

@Entity(tableName = "taste_signals", indices = [Index("createdAt"), Index("artworkId")])
data class TasteSignalEntity(
    @PrimaryKey val id: String,
    val artworkId: String,
    val kind: String,
    val genre: String?,
    val artistId: String?,
    val artistName: String?,
    val period: String?,
    val museumId: String?,
    val mood: String?,
    val value: Float,
    val dwellMillis: Long,
    val createdAt: Long,
)

@Entity(tableName = "interests", indices = [Index("weight"), Index("dimension")])
data class InterestEntity(
    @PrimaryKey val key: String,
    val label: String,
    val dimension: String,
    val weight: Float,
    val updatedAt: Long,
    val evidence: Int,
    val pinned: Boolean,
    val muted: Boolean,
)

@Entity(tableName = "installed_packs")
data class InstalledPackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val version: Int,
    val period: String,
    val genre: String,
    val quality: String,
    val artworkCount: Int,
    val sizeBytes: Long,
    val installedAt: Long,
    val localPath: String,
    val checksumVerified: Boolean,
    val signatureVerified: Boolean,
    val deltaApplied: Boolean,
    val manifestJson: String,
)

@Entity(tableName = "flashcards", indices = [Index("dueAt")])
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val front: String,
    val back: String,
    val artworkId: String?,
    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val dueAt: Long,
    val lapses: Int,
)

@Entity(tableName = "reading_progress", indices = [Index("lastViewedAt")])
data class ReadingProgressEntity(
    @PrimaryKey val artworkId: String,
    val progress: Float,
    val lastViewedAt: Long,
    val zoomedRegions: Int,
    val dwellMillis: Long,
)

@Entity(tableName = "hidden_works")
data class HiddenWorkEntity(
    @PrimaryKey val artworkId: String,
    val hiddenAt: Long,
    val reason: String,
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val lastUsedAt: Long,
    val uses: Int,
)

@Entity(tableName = "quiz_results")
data class QuizResultEntity(
    @PrimaryKey val id: String,
    val quizId: String,
    val kind: String,
    val correct: Int,
    val total: Int,
    val completedAt: Long,
)

@Entity(tableName = "saved_filters")
data class SavedFilterEntity(
    @PrimaryKey val id: String,
    val label: String,
    val filterJson: String,
    val createdAt: Long,
)
