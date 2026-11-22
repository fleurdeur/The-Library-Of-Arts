package org.libraryofarts.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.libraryofarts.core.common.TimeProvider
import org.libraryofarts.core.data.mapper.toArtwork
import org.libraryofarts.core.data.mapper.toEntity
import org.libraryofarts.core.data.seed.SeedArtworks
import org.libraryofarts.core.database.ArtworkDao
import org.libraryofarts.core.database.ProgressDao
import org.libraryofarts.core.database.ReadingProgressEntity
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.ArtworkFilter
import org.libraryofarts.core.model.TasteSignalKind
import org.libraryofarts.core.network.MuseumNetwork
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtworkRepository @Inject constructor(
    private val artworkDao: ArtworkDao,
    private val progressDao: ProgressDao,
    private val network: MuseumNetwork,
    private val taste: TasteRepository,
    private val time: TimeProvider,
) {
    val artworkCount: Flow<Int> = artworkDao.observeCount()

    suspend fun seedIfNeeded() {
        if (artworkDao.count() == 0) {
            artworkDao.upsertAll(SeedArtworks.all.map { it.toEntity() })
        }
    }

    suspend fun ensureSeed() = seedIfNeeded()

    fun search(filter: ArtworkFilter): Flow<PagingData<Artwork>> = Pager(
        config = PagingConfig(
            pageSize = 24,
            prefetchDistance = 6,
            enablePlaceholders = false,
            initialLoadSize = 36,
        ),
        pagingSourceFactory = { artworkDao.pagingSource(ArtworkQueryBuilder.build(filter)) },
    ).flow.map { paging -> paging.map { entity -> entity.toArtwork() ?: SeedArtworks.all.first() } }

    suspend fun searchOnce(filter: ArtworkFilter, limit: Int = 40): List<Artwork> =
        artworkDao.rawList(ArtworkQueryBuilder.build(filter, limit))
            .mapNotNull { it.toArtwork() }

    suspend fun getArtwork(id: String): Artwork? {
        val seeded = SeedArtworks.byId[id]
        val cached = artworkDao.getById(id)?.toArtwork()
        if (cached != null) return cached
        if (seeded != null) {
            artworkDao.upsert(seeded.toEntity())
            return seeded
        }
        val remote = network.artwork(id) ?: return null
        artworkDao.upsert(remote.toEntity())
        return remote
    }

    fun observeArtwork(id: String): Flow<Artwork?> = artworkDao.observeById(id).map { it?.toArtwork() }

    suspend fun refreshFromNetwork(query: String = ""): Int {
        val result = network.search(query, page = 0, pageSize = 40)
        if (result.artworks.isNotEmpty()) {
            artworkDao.upsertAll(result.artworks.map { it.toEntity() })
        }
        return result.artworks.size
    }

    suspend fun dailyArtwork(epochDay: Long): Artwork {
        val pool = artworkDao.random(12).mapNotNull { it.toArtwork() }
        if (pool.isNotEmpty()) {
            return pool[((epochDay % pool.size) + pool.size).toInt() % pool.size]
        }
        return SeedArtworks.all[((epochDay % SeedArtworks.all.size) + SeedArtworks.all.size).toInt() % SeedArtworks.all.size]
    }

    suspend fun allCandidates(limit: Int = 160): List<Artwork> {
        val fromDb = artworkDao.getAll().mapNotNull { it.toArtwork() }
        return (fromDb + SeedArtworks.all).distinctBy { it.id }.take(limit)
    }

    suspend fun related(artwork: Artwork, limit: Int = 12): List<Artwork> {
        val sameArtist = artwork.artist?.name?.let { artworkDao.byArtist(it) }.orEmpty()
        val sameGenre = artwork.genre?.let { artworkDao.byGenre(it, 12) }.orEmpty()
        val samePeriod = artwork.period?.let { artworkDao.byPeriod(it, 12) }.orEmpty()
        return (sameArtist + sameGenre + samePeriod)
            .mapNotNull { it.toArtwork() }
            .filter { it.id != artwork.id }
            .distinctBy { it.id }
            .take(limit)
            .ifEmpty {
                SeedArtworks.all.filter { it.id != artwork.id }
                    .sortedByDescending { candidate -> overlap(artwork, candidate) }
                    .take(limit)
            }
    }

    suspend fun similarByEmbedding(artwork: Artwork, limit: Int = 12): List<Artwork> {
        val terms = buildSet {
            artwork.tags.forEach { add(it.lowercase()) }
            artwork.moods.forEach { add(it.lowercase()) }
            artwork.subjects.forEach { add(it.lowercase()) }
            artwork.genre?.let { add(it.lowercase()) }
            artwork.period?.let { add(it.lowercase()) }
        }
        val candidates = allCandidates(300)
        return candidates.asSequence()
            .filter { it.id != artwork.id }
            .map { candidate ->
                val candidateTerms = buildSet {
                    candidate.tags.forEach { add(it.lowercase()) }
                    candidate.moods.forEach { add(it.lowercase()) }
                    candidate.subjects.forEach { add(it.lowercase()) }
                    candidate.genre?.let { add(it.lowercase()) }
                    candidate.period?.let { add(it.lowercase()) }
                }
                val intersection = terms.intersect(candidateTerms).size.toFloat()
                val union = terms.union(candidateTerms).size.coerceAtLeast(1).toFloat()
                candidate to (intersection / union + paletteSimilarity(artwork, candidate) * 0.35f)
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .toList()
    }

    private fun paletteSimilarity(a: Artwork, b: Artwork): Float {
        val aColors = a.palette.map { it.argb }.toSet()
        val bColors = b.palette.map { it.argb }.toSet()
        if (aColors.isEmpty() || bColors.isEmpty()) return 0f
        val shared = aColors.intersect(bColors).size.toFloat()
        return shared / maxOf(aColors.size, bColors.size).toFloat()
    }

    private fun overlap(a: Artwork, b: Artwork): Int {
        var score = 0
        if (a.genre == b.genre) score += 3
        if (a.period == b.period) score += 3
        if (a.artist?.name == b.artist?.name) score += 3
        score += a.moods.intersect(b.moods.toSet()).size
        score += a.subjects.intersect(b.subjects.toSet()).size
        return score
    }

    suspend fun recordView(artwork: Artwork, dwellMillis: Long = 0, zoomed: Boolean = false) {
        artworkDao.incrementView(artwork.id)
        val now = time.nowMillis()
        val existing = progressDao.forArtwork(artwork.id)
        progressDao.upsert(
            ReadingProgressEntity(
                artworkId = artwork.id,
                progress = if (dwellMillis > 0) 1f else existing?.progress ?: 0.4f,
                lastViewedAt = now,
                zoomedRegions = (existing?.zoomedRegions ?: 0) + if (zoomed) 1 else 0,
                dwellMillis = (existing?.dwellMillis ?: 0) + dwellMillis,
            ),
        )
        taste.recordSignal(
            artwork = artwork,
            kind = if (dwellMillis >= 4_000) TasteSignalKind.DWELL else TasteSignalKind.VIEW,
            dwellMillis = dwellMillis,
        )
        if (zoomed) {
            taste.recordSignal(artwork, TasteSignalKind.ZOOM_REGION, dwellMillis)
        }
    }

    suspend fun recordZoom(artwork: Artwork) = taste.recordSignal(artwork, TasteSignalKind.ZOOM_REGION)
    suspend fun recordSkip(artwork: Artwork) = taste.recordSignal(artwork, TasteSignalKind.SKIP)
    suspend fun recordShare(artwork: Artwork) = taste.recordSignal(artwork, TasteSignalKind.SHARE)
    suspend fun recordSave(artwork: Artwork) = taste.recordSignal(artwork, TasteSignalKind.SAVE)
    suspend fun recordJournal(artwork: Artwork) = taste.recordSignal(artwork, TasteSignalKind.JOURNAL)

    suspend fun toggleHidden(artwork: Artwork, hidden: Boolean) {
        if (hidden) {
            progressDao.hide(
                org.libraryofarts.core.database.HiddenWorkEntity(
                    artworkId = artwork.id,
                    hiddenAt = time.nowMillis(),
                    reason = "hidden by user",
                ),
            )
            taste.recordSignal(artwork, TasteSignalKind.HIDE)
        } else {
            progressDao.unhide(artwork.id)
        }
    }

    suspend fun hiddenIds(): Set<String> = progressDao.hiddenIds().toSet()

    suspend fun continueExploring(limit: Int = 10): List<Artwork> {
        val ids = progressDao.observeContinue(limit).first()
        return ids.mapNotNull { getArtwork(it.artworkId) }
    }

    suspend fun trending(limit: Int = 12): List<Artwork> =
        artworkDao.trending(limit).mapNotNull { it.toArtwork() }

    suspend fun hiddenGems(limit: Int = 12): List<Artwork> =
        artworkDao.hiddenGems(limit).mapNotNull { it.toArtwork() }

    suspend fun byMuseum(museumId: String, limit: Int = 24): List<Artwork> =
        artworkDao.byMuseum(museumId, limit).mapNotNull { it.toArtwork() }

    suspend fun artists(limit: Int = 60): List<String> = artworkDao.artists().take(limit)

    suspend fun genres(): List<String> = artworkDao.genres()

    suspend fun periods(): List<String> = artworkDao.periods()

    suspend fun mediums(): List<String> = artworkDao.mediums()
}
