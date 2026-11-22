package org.libraryofarts.core.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtworkDao {
    @Upsert
    suspend fun upsertAll(artworks: List<ArtworkEntity>)

    @Upsert
    suspend fun upsert(artwork: ArtworkEntity)

    @Query("SELECT * FROM artworks WHERE id = :id")
    suspend fun getById(id: String): ArtworkEntity?

    @Query("SELECT * FROM artworks WHERE id = :id")
    fun observeById(id: String): Flow<ArtworkEntity?>

    @Query("SELECT * FROM artworks ORDER BY title")
    suspend fun getAll(): List<ArtworkEntity>

    @Query("SELECT COUNT(*) FROM artworks")
    suspend fun count(): Int

    @Query("SELECT * FROM artworks WHERE artistName = :artist ORDER BY yearStart")
    suspend fun byArtist(artist: String): List<ArtworkEntity>

    @Query("SELECT * FROM artworks WHERE genre = :genre ORDER BY title LIMIT :limit")
    suspend fun byGenre(genre: String, limit: Int): List<ArtworkEntity>

    @Query("SELECT * FROM artworks WHERE period = :period ORDER BY yearStart LIMIT :limit")
    suspend fun byPeriod(period: String, limit: Int): List<ArtworkEntity>

    @Query("SELECT * FROM artworks WHERE museumId = :museumId ORDER BY title LIMIT :limit")
    suspend fun byMuseum(museumId: String, limit: Int): List<ArtworkEntity>

    @Query("SELECT * FROM artworks WHERE packId = :packId ORDER BY title")
    fun observePackArtworks(packId: String): Flow<List<ArtworkEntity>>

    @Query(
        "SELECT * FROM artworks WHERE isRarelyViewed = 1 OR viewCount < 5 " +
            "ORDER BY (viewCount + 1) * 1.0 / (ABS(RANDOM() % 1000) + 1) LIMIT :limit",
    )
    suspend fun hiddenGems(limit: Int): List<ArtworkEntity>

    @Query("SELECT * FROM artworks ORDER BY viewCount DESC, title LIMIT :limit")
    suspend fun trending(limit: Int): List<ArtworkEntity>

    @Query("SELECT * FROM artworks ORDER BY RANDOM() LIMIT :limit")
    suspend fun random(limit: Int): List<ArtworkEntity>

    @Query("SELECT * FROM artworks WHERE viewCount < 5 ORDER BY RANDOM() LIMIT :limit")
    suspend fun randomRare(limit: Int): List<ArtworkEntity>

    @Query("UPDATE artworks SET viewCount = viewCount + 1 WHERE id = :id")
    suspend fun incrementView(id: String)

    @Query("SELECT * FROM artworks ORDER BY ABS(COALESCE(yearStart, 0) - :year) LIMIT :limit")
    suspend fun closestToYear(year: Int, limit: Int): List<ArtworkEntity>

    @Query("SELECT DISTINCT artistName FROM artworks WHERE artistName != '' ORDER BY artistName")
    suspend fun artists(): List<String>

    @Query("SELECT DISTINCT genre FROM artworks WHERE genre IS NOT NULL ORDER BY genre")
    suspend fun genres(): List<String>

    @Query("SELECT DISTINCT period FROM artworks WHERE period IS NOT NULL ORDER BY period")
    suspend fun periods(): List<String>

    @Query("SELECT DISTINCT medium FROM artworks WHERE medium IS NOT NULL ORDER BY medium")
    suspend fun mediums(): List<String>

    @Query("DELETE FROM artworks WHERE packId = :packId")
    suspend fun deletePackArtworks(packId: String)

    @RawQuery(observedEntities = [ArtworkEntity::class])
    fun pagingSource(query: SupportSQLiteQuery): PagingSource<Int, ArtworkEntity>

    @RawQuery(observedEntities = [ArtworkEntity::class])
    suspend fun rawList(query: SupportSQLiteQuery): List<ArtworkEntity>

    @Query("SELECT COUNT(*) FROM artworks")
    fun observeCount(): Flow<Int>
}
