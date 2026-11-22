package org.libraryofarts.core.data.repository

import org.libraryofarts.core.common.TimeProvider
import org.libraryofarts.core.database.TasteDao
import org.libraryofarts.core.database.TasteSignalEntity
import org.libraryofarts.core.database.InterestEntity
import org.libraryofarts.core.datastore.SettingsRepository
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.Interest
import org.libraryofarts.core.model.TasteDimension
import org.libraryofarts.core.model.TasteProfile
import org.libraryofarts.core.model.TasteSignal
import org.libraryofarts.core.model.TasteSignalKind
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Learns a taste profile entirely on-device.
 *
 * Each signal contributes to the dimensions it touches. Old signals decay with
 * a 45-day half-life, so a summer obsession with Impressionism gradually makes
 * room for new interests. Discovery is a separate, reserved slice of the feed,
 * controlled by the user.
 */
@Singleton
class TasteRepository @Inject constructor(
    private val dao: TasteDao,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) {
    private val halfLifeDays = 45.0

    val profile: Flow<TasteProfile> = combine(
        dao.observeInterests(),
        dao.observeSignalCount(),
        settings.preferences,
    ) { interests, signalCount, prefs ->
        TasteProfile(
            interests = interests.map { it.toModel() },
            discoveryRatio = prefs.discoveryRatio,
            learningEnabled = prefs.tasteLearningEnabled,
            lastUpdated = interests.maxOfOrNull { it.updatedAt } ?: 0,
            totalSignals = signalCount,
            topMoods = interests.filter { it.dimension == TasteDimension.MOOD.name }
                .sortedByDescending { it.weight }
                .take(3)
                .map { it.label },
        )
    }

    fun observeInterests(): Flow<List<Interest>> = dao.observeInterests().map { list -> list.map { it.toModel() } }

    suspend fun recordSignal(
        artwork: Artwork,
        kind: TasteSignalKind,
        dwellMillis: Long = 0,
        mood: String? = null,
    ) {
        val now = time.nowMillis()
        val value = when (kind) {
            TasteSignalKind.DWELL -> kind.baseWeight * (1f + (dwellMillis / 20_000f).coerceAtMost(2f))
            TasteSignalKind.ZOOM_REGION -> kind.baseWeight
            else -> kind.baseWeight
        }
        val signal = TasteSignal(
            id = UUID.randomUUID().toString(),
            artworkId = artwork.id,
            kind = kind,
            genre = artwork.genre,
            artistId = artwork.artist?.id,
            artistName = artwork.artist?.name,
            period = artwork.period,
            museumId = artwork.museum.id,
            mood = mood ?: artwork.moods.firstOrNull(),
            value = value,
            dwellMillis = dwellMillis,
            createdAt = now,
        )
        dao.insertSignal(signal.toEntity())
        recalculate(now)
    }

    suspend fun recalculate(now: Long = time.nowMillis()) {
        val signals = dao.recentSignals(2_000)
        val scores = mutableMapOf<String, MutableInterest>()
        signals.forEach { entity ->
            val ageDays = (now - entity.createdAt).coerceAtLeast(0) / 86_400_000.0
            val decay = Math.pow(0.5, ageDays / halfLifeDays).toFloat()
            val contribution = entity.value * decay
            fun add(dimension: TasteDimension, key: String?, label: String?) {
                if (key.isNullOrBlank() || label.isNullOrBlank()) return
                val id = "${dimension.name}:$key"
                val current = scores.getOrPut(id) {
                    MutableInterest(id, label, dimension, 0f, 0, entity.createdAt)
                }
                current.weight += contribution
                current.evidence += 1
                current.updatedAt = maxOf(current.updatedAt, entity.createdAt)
            }
            add(TasteDimension.GENRE, entity.genre, entity.genre)
            add(TasteDimension.ARTIST, entity.artistId ?: entity.artistName, entity.artistName)
            add(TasteDimension.PERIOD, entity.period, entity.period)
            add(TasteDimension.MUSEUM, entity.museumId, entity.museumId.orEmpty().replaceFirstChar { it.uppercase() })
            add(TasteDimension.MOOD, entity.mood, entity.mood)
        }
        val existing = dao.interests().associateBy { it.key }
        val updated = scores.values
            .filter { it.evidence >= 1 }
            .sortedByDescending { it.weight }
            .take(40)
            .map { score ->
                val previous = existing[score.id]
                InterestEntity(
                    key = score.id,
                    label = score.label,
                    dimension = score.dimension.name,
                    weight = score.weight.coerceIn(-1f, 8f),
                    updatedAt = score.updatedAt,
                    evidence = score.evidence,
                    pinned = previous?.pinned ?: false,
                    muted = previous?.muted ?: false,
                )
            }
        dao.upsertInterests(updated)
        // Prune interests that no longer have any supporting signal.
        val keep = updated.map { it.key }.toSet()
        existing.keys.filterNot { it in keep }.forEach { dao.removeInterest(it) }
    }

    suspend fun setMuted(key: String, muted: Boolean) = dao.setMuted(key, muted)
    suspend fun setPinned(key: String, pinned: Boolean) = dao.setPinned(key, pinned)
    suspend fun removeInterest(key: String) = dao.removeInterest(key)

    suspend fun reset() {
        dao.clearSignals()
        dao.clearInterests()
    }

    suspend fun signalCount(): Int = dao.signalCount()

    private fun TasteSignal.toEntity(): TasteSignalEntity = TasteSignalEntity(
        id = id,
        artworkId = artworkId,
        kind = kind.name,
        genre = genre,
        artistId = artistId,
        artistName = artistName,
        period = period,
        museumId = museumId,
        mood = mood,
        value = value,
        dwellMillis = dwellMillis,
        createdAt = createdAt,
    )

    private fun InterestEntity.toModel(): Interest = Interest(
        key = key,
        label = label,
        dimension = runCatching { TasteDimension.valueOf(dimension) }.getOrDefault(TasteDimension.GENRE),
        weight = weight,
        updatedAt = updatedAt,
        evidence = evidence,
        pinned = pinned,
        muted = muted,
    )

    private class MutableInterest(
        val id: String,
        val label: String,
        val dimension: TasteDimension,
        var weight: Float,
        var evidence: Int,
        var updatedAt: Long,
    )
}
