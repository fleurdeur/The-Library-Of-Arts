package org.libraryofarts.core.network.source

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.libraryofarts.core.model.ArtSource
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.License
import org.libraryofarts.core.network.MuseumRemoteSource
import org.libraryofarts.core.network.imageUrlOrEmpty
import org.libraryofarts.core.network.mapper.MapperSupport
import org.libraryofarts.core.network.model.MetApi
import org.libraryofarts.core.network.model.MetObject
import javax.inject.Inject

@Serializable
data class MetSearchResponse(
    val total: Int = 0,
    @SerialName("objectIDs") val objectIDs: List<Int> = emptyList(),
)

class MetSource @Inject constructor(
    private val api: MetApi,
) : MuseumRemoteSource {
    override val source: ArtSource = ArtSource.MET
    override val isConfigured: Boolean = true

    override suspend fun highlights(pageSize: Int): List<Artwork> =
        api.search(query = "painting", hasImages = true, isHighlight = true)
            .objectIDs
            .take(pageSize)
            .mapNotNull { runCatching { api.getObject(it) }.getOrNull() }
            .mapNotNull { it.toArtwork() }

    override suspend fun search(query: String, page: Int, pageSize: Int): List<Artwork> {
        val ids = api.search(query = query.ifBlank { "art" }, hasImages = true).objectIDs
        val pageIds = ids.drop(page * pageSize).take(pageSize)
        return pageIds.mapNotNull { id -> runCatching { api.getObject(id) }.getOrNull() }
            .mapNotNull { it.toArtwork() }
    }

    override suspend fun artwork(sourceId: String): Artwork? =
        runCatching { api.getObject(sourceId.toInt()) }.getOrNull()?.toArtwork()

    private fun MetObject.toArtwork(): Artwork? {
        if (!isPublicDomain || primaryImage.isNullOrBlank()) return null
        val credit = creditLine.orEmpty()
        val image = MapperSupport.image(
            url = primaryImage,
            thumb = primaryImageSmall,
            width = 2400,
            height = 3000,
            credit = credit,
        )
        return MapperSupport.baseArtwork(
            source = source,
            sourceId = objectID.toString(),
            title = title.orEmpty(),
            artistName = artistDisplayName,
            yearStart = objectBeginDate?.takeIf { it > 0 } ?: parseYear(objectDate),
            yearEnd = objectEndDate?.takeIf { it > 0 },
            medium = medium,
            genreRaw = classification ?: objectName,
            image = image,
            license = License.CC0.copy(creditLine = credit),
            description = description?.takeIf { it.isNotBlank() },
            credit = credit,
            provenance = MapperSupport.provenance(credit),
        ).copy(
            period = period.ifBlank { dynasty.ifBlank { culture.ifBlank { MapperSupport.periodFor(objectBeginDate) } } },
            movement = movement,
            dimensions = dimensions,
            location = galleryNumber?.let { "Gallery $it" },
            accessionNumber = accessionNumber,
            subjects = listOfNotNull(objectName, classification, department).filter { it.isNotBlank() },
            isHighlight = isHighlight,
        )
    }

    private fun parseYear(raw: String?): Int? =
        Regex("-?\\d{3,4}").find(raw.orEmpty())?.value?.toIntOrNull()
}
