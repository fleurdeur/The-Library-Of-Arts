package org.libraryofarts.core.network.source

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.libraryofarts.core.model.ArtSource
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.License
import org.libraryofarts.core.network.MuseumRemoteSource
import org.libraryofarts.core.network.SourceCredentials
import org.libraryofarts.core.network.mapper.MapperSupport
import org.libraryofarts.core.network.model.NgaApi
import javax.inject.Inject

class NationalGallerySource @Inject constructor(
    private val api: NgaApi,
    private val credentials: SourceCredentials,
) : MuseumRemoteSource {
    override val source: ArtSource = ArtSource.NATIONAL_GALLERY
    override val isConfigured: Boolean = true

    override suspend fun highlights(pageSize: Int): List<Artwork> =
        api.artworks(page = 1, pageSize = pageSize, apiKey = credentials.ngaKey.takeIf { it.isNotBlank() })
            .data.mapNotNull { it.toArtwork() }

    override suspend fun search(query: String, page: Int, pageSize: Int): List<Artwork> =
        api.artworks(
            query = query.takeIf { it.isNotBlank() },
            page = page + 1,
            pageSize = pageSize,
            apiKey = credentials.ngaKey.takeIf { it.isNotBlank() },
        ).data.mapNotNull { it.toArtwork() }

    private fun NgaArtwork.toArtwork(): Artwork? {
        if (primaryImage.isNullOrBlank()) return null
        val credit = creditLine.orEmpty()
        return MapperSupport.baseArtwork(
            source = source,
            sourceId = objectID.toString(),
            title = title,
            artistName = attribution,
            yearStart = beginYear,
            yearEnd = endYear,
            medium = medium,
            genreRaw = classification,
            image = MapperSupport.image(primaryImage, primaryImage, imageWidth ?: 2000, imageHeight ?: 2500, credit = credit),
            license = License.CC0.copy(creditLine = credit),
            description = null,
            credit = credit,
        ).copy(
            dimensions = dimensions,
            period = period ?: MapperSupport.periodFor(beginYear),
            subjects = listOfNotNull(classification, nationality).filter { it.isNotBlank() },
        )
    }
}

@Serializable
data class NgaResponse(val data: List<NgaArtwork> = emptyList())

@Serializable
data class NgaArtwork(
    @SerialName("objectID") val objectID: Long,
    val title: String = "",
    val attribution: String? = null,
    @SerialName("beginYear") val beginYear: Int? = null,
    @SerialName("endYear") val endYear: Int? = null,
    val displayDate: String? = null,
    val medium: String? = null,
    val dimensions: String? = null,
    val classification: String? = null,
    val nationality: String? = null,
    val period: String? = null,
    @SerialName("primaryImage") val primaryImage: String? = null,
    val creditLine: String? = null,
    @SerialName("imageWidth") val imageWidth: Int? = null,
    @SerialName("imageHeight") val imageHeight: Int? = null,
)
