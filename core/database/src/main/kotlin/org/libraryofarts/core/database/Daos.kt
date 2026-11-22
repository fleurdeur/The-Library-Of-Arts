package org.libraryofarts.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Upsert
    suspend fun upsert(entry: JournalEntryEntity)

    @Delete
    suspend fun delete(entry: JournalEntryEntity)

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getById(id: String): JournalEntryEntity?

    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC")
    suspend fun all(): List<JournalEntryEntity>

    @Query(
        "SELECT * FROM journal_entries WHERE text LIKE '%' || :query || '%' " +
            "OR artworkTitle LIKE '%' || :query || '%' OR tagsCsv LIKE '%' || :query || '%' " +
            "ORDER BY createdAt DESC",
    )
    fun search(query: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE artworkId = :artworkId ORDER BY createdAt DESC")
    fun forArtwork(artworkId: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE mood = :mood ORDER BY createdAt DESC")
    fun byMood(mood: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE tagsCsv LIKE '%' || :tag || '%' ORDER BY createdAt DESC")
    fun byTag(tag: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT COUNT(*) FROM journal_entries")
    suspend fun count(): Int

    @Query("SELECT DISTINCT tagsCsv FROM journal_entries")
    suspend fun allTagsRaw(): List<String>
}

@Dao
interface CollectionDao {
    @Upsert
    suspend fun upsertCollection(collection: CollectionEntity)

    @Query("SELECT * FROM collections ORDER BY updatedAt DESC")
    fun observeCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getCollection(id: String): CollectionEntity?

    @Query("SELECT * FROM collections WHERE id = :id")
    fun observeCollection(id: String): Flow<CollectionEntity?>

    @Query("SELECT * FROM collections WHERE isFavorites = 1 LIMIT 1")
    suspend fun favorites(): CollectionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addArtwork(entry: CollectionArtworkEntity)

    @Query("DELETE FROM collection_artworks WHERE collectionId = :collectionId AND artworkId = :artworkId")
    suspend fun removeArtwork(collectionId: String, artworkId: String)

    @Query("SELECT * FROM collection_artworks WHERE collectionId = :collectionId ORDER BY position")
    fun observeArtworkIds(collectionId: String): Flow<List<CollectionArtworkEntity>>

    @Query("SELECT * FROM collection_artworks WHERE artworkId = :artworkId")
    suspend fun memberships(artworkId: String): List<CollectionArtworkEntity>

    @Query("SELECT COUNT(*) FROM collection_artworks WHERE collectionId = :collectionId")
    suspend fun count(collectionId: String): Int

    @Query("DELETE FROM collections WHERE id = :id AND isFavorites = 0")
    suspend fun deleteCollection(id: String)
}

@Dao
interface TasteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: TasteSignalEntity)

    @Query("SELECT * FROM taste_signals ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recentSignals(limit: Int): List<TasteSignalEntity>

    @Query("SELECT * FROM taste_signals ORDER BY createdAt DESC")
    suspend fun allSignals(): List<TasteSignalEntity>

    @Query("DELETE FROM taste_signals WHERE createdAt < :before")
    suspend fun deleteSignalsBefore(before: Long)

    @Query("DELETE FROM taste_signals")
    suspend fun clearSignals()

    @Query("SELECT COUNT(*) FROM taste_signals")
    suspend fun signalCount(): Int

    @Query("SELECT COUNT(*) FROM taste_signals")
    fun observeSignalCount(): Flow<Int>

    @Upsert
    suspend fun upsertInterests(interests: List<InterestEntity>)

    @Query("SELECT * FROM interests ORDER BY weight DESC")
    fun observeInterests(): Flow<List<InterestEntity>>

    @Query("SELECT * FROM interests ORDER BY weight DESC")
    suspend fun interests(): List<InterestEntity>

    @Query("DELETE FROM interests")
    suspend fun clearInterests()

    @Query("UPDATE interests SET muted = :muted WHERE key = :key")
    suspend fun setMuted(key: String, muted: Boolean)

    @Query("UPDATE interests SET pinned = :pinned WHERE key = :key")
    suspend fun setPinned(key: String, pinned: Boolean)

    @Query("DELETE FROM interests WHERE key = :key")
    suspend fun removeInterest(key: String)
}

@Dao
interface PackDao {
    @Upsert
    suspend fun upsert(pack: InstalledPackEntity)

    @Query("SELECT * FROM installed_packs ORDER BY installedAt DESC")
    fun observeInstalled(): Flow<List<InstalledPackEntity>>

    @Query("SELECT * FROM installed_packs ORDER BY installedAt DESC")
    suspend fun allInstalled(): List<InstalledPackEntity>

    @Query("SELECT * FROM installed_packs WHERE id = :id")
    suspend fun getById(id: String): InstalledPackEntity?

    @Query("DELETE FROM installed_packs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM installed_packs")
    fun observeTotalBytes(): Flow<Long>
}

@Dao
interface LearningDao {
    @Upsert
    suspend fun upsertCard(card: FlashcardEntity)

    @Upsert
    suspend fun upsertCards(cards: List<FlashcardEntity>)

    @Query("SELECT * FROM flashcards WHERE dueAt <= :now ORDER BY dueAt LIMIT :limit")
    suspend fun dueCards(now: Long, limit: Int): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards")
    fun observeAll(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE artworkId = :artworkId")
    suspend fun forArtwork(artworkId: String): List<FlashcardEntity>

    @Query("SELECT COUNT(*) FROM flashcards WHERE dueAt <= :now")
    fun observeDueCount(now: Long): Flow<Int>

    @Upsert
    suspend fun saveQuizResult(result: QuizResultEntity)

    @Query("SELECT * FROM quiz_results ORDER BY completedAt DESC LIMIT :limit")
    fun observeQuizResults(limit: Int): Flow<List<QuizResultEntity>>
}

@Dao
interface ProgressDao {
    @Upsert
    suspend fun upsert(progress: ReadingProgressEntity)

    @Query("SELECT * FROM reading_progress ORDER BY lastViewedAt DESC LIMIT :limit")
    fun observeContinue(limit: Int): Flow<List<ReadingProgressEntity>>

    @Query("SELECT * FROM reading_progress WHERE artworkId = :artworkId")
    suspend fun forArtwork(artworkId: String): ReadingProgressEntity?

    @Query("DELETE FROM reading_progress")
    suspend fun clear()

    @Upsert
    suspend fun hide(work: HiddenWorkEntity)

    @Query("SELECT artworkId FROM hidden_works")
    suspend fun hiddenIds(): List<String>

    @Query("DELETE FROM hidden_works WHERE artworkId = :artworkId")
    suspend fun unhide(artworkId: String)
}

@Dao
interface SearchDao {
    @Upsert
    suspend fun record(query: SearchHistoryEntity)

    @Query("SELECT * FROM search_history ORDER BY lastUsedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history WHERE query = :query")
    suspend fun find(query: String): SearchHistoryEntity?

    @Query("DELETE FROM search_history")
    suspend fun clear()

    @Upsert
    suspend fun saveFilter(filter: SavedFilterEntity)

    @Query("SELECT * FROM saved_filters ORDER BY createdAt DESC")
    fun observeSavedFilters(): Flow<List<SavedFilterEntity>>

    @Query("DELETE FROM saved_filters WHERE id = :id")
    suspend fun deleteFilter(id: String)
}
