package org.libraryofarts.core.data.scan

import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.libraryofarts.core.common.PerceptualHash
import org.libraryofarts.core.data.mapper.ArtworkJson
import org.libraryofarts.core.data.repository.ArtworkRepository
import org.libraryofarts.core.database.PackDao
import org.libraryofarts.core.model.Artwork

@Serializable
data class ScanIndexEntry(
    val artworkId: String,
    val dhash: String,
    val title: String = "",
    val artist: String = "",
)

data class ScanMatch(
    val artwork: Artwork,
    val distance: Int,
    val confidence: Float,
)

/**
 * "Scan a Painting" works fully offline: offline packs may include a
 * scan-index.json of perceptual dHashes. We compare a 64-bit difference hash
 * and accept only a close match. No image leaves the device.
 */
@Singleton
class ScanRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packDao: PackDao,
    private val artworkRepository: ArtworkRepository,
) {
    suspend fun identify(bitmap: Bitmap): ScanMatch? = withContext(Dispatchers.IO) {
        val hash = PerceptualHash.dHash(bitmap)
        val entries = loadEntries()
        if (entries.isEmpty()) return@withContext null
        val best = entries.minByOrNull { PerceptualHash.hammingDistance(hash, PerceptualHash.fromHex(it.dhash)) }
            ?: return@withContext null
        val distance = PerceptualHash.hammingDistance(hash, PerceptualHash.fromHex(best.dhash))
        if (distance > MAX_DISTANCE) return@withContext null
        val artwork = artworkRepository.getArtwork(best.artworkId) ?: return@withContext null
        ScanMatch(
            artwork = artwork,
            distance = distance,
            confidence = (1f - distance / 64f).coerceIn(0f, 1f),
        )
    }

    suspend fun hasIndex(): Boolean = loadEntries().isNotEmpty()

    private suspend fun loadEntries(): List<ScanIndexEntry> {
        val installed = packDao.allInstalled()
        return installed.flatMap { pack ->
            val index = File(pack.localPath, "scan-index.json")
            if (!index.exists()) return@flatMap emptyList()
            runCatching {
                index.readLines()
                    .filter { it.isNotBlank() }
                    .map { ArtworkJson.decodeFromString(ScanIndexEntry.serializer(), it) }
            }.getOrDefault(emptyList())
        }
    }

    companion object {
        private const val MAX_DISTANCE = 10
    }
}
