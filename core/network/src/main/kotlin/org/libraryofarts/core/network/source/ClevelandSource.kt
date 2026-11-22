package org.libraryofarts.core.network.source

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.libraryofarts.core.model.ArtSource
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.License
import org.libraryofarts.core.network.MuseumRemoteSource
import org.libraryofarts.core.network.mapper.MapperSupport
import org.libraryofarts.core.network.model.ClevelandApi
import javax.inject.Inject

class ClevelandSource @Inject constructor(
    private val api: ClevelandApi,
) : MuseumRemoteSource {
    override val source: ArtSource = ArtSource.CLEVELAND
    override val isConfigured: Boolean = true

    override suspend fun highlights(pageSize: Int): List<Artwork> =
        api.artworks(query = null, limit = pageSize, skip = 0).data.mapNotNull { it.toArtwork() }

    override suspend fun search(query: String, page: Int, pageSize: Int): List<Artwork> =
        api.artworks(
            query = query.takeIf { it.isNotBlank() },
            limit = pageSize,
            skip = page * pageSize,
        ).data.mapNotNull { it.toArtwork() }

    private fun ClevelandArtwork.toArtwork(): Artwork? {
        if (!shareLicenseStatus.equals("CC0", true)) return null
        val imageUrl = images?.web?.url ?: images?.print?.url ?: return null
        val credit = creditline.orEmpty()
        return MapperSupport.baseArtwork(
            source = source,
            sourceId = id.toString(),
            title = title,
            artistName = creators?.firstOrNull()?.description,
            yearStart = creationDate?.earliestYear,
            yearEnd = creationDate?.latestYear,
            medium = technique,
            genreRaw = type ?: department,
            image = org.libraryofarts.core.model.ArtworkImage(
                thumbnailUrl = imageUrl,
                fullUrl = images?.print?.url ?: imageUrl,
                width = images?.print?.width ?: 2000,
                height = images?.print?.height ?: 2500,
                creditLine = credit,
                altText = title,
            ),
            license = License.CC0.copy(creditLine = credit),
            description = description ?: wallDescription,
            credit = credit,
            provenance = MapperSupport.provenance(provenance?.mapNotNull { it.description }?.joinToString("; ")),
        ).copy(
            dimensions = measurements,
            accessionNumber = accessionNumber,
            movement = culture,
            location = gallery?.takeIf { it.isNotBlank() }?.let { "Gallery $it" },
            subjects = listOfNotNull(culture, department, type).filter { it.isNotBlank() },
        )
    }
}

@Serializable
data class ClevelandResponse(val data: List<ClevelandArtwork> = emptyList())

@Serializable
data class ClevelandArtwork(
    val id: Long,
    val title: String = "",
    val description: String? = null,
    @SerialName("wall_description") val wallDescription: String? = null,
    val creators: List<ClevelandCreator>? = null,
    @SerialName("creation_date") val creationDate: ClevelandDate? = null,
    val culture: String? = null,
    val technique: String? = null,
    val department: String? = null,
    val type: String? = null,
    val measurements: String? = null,
    val images: ClevelandImages? = null,
    @SerialName("accession_number") val accessionNumber: String? = null,
    val creditline: String? = null,
    val gallery: String? = null,
    val provenance: List<ClevelandProvenance>? = null,
    @SerialName("share_license_status") val shareLicenseStatus: String? = null,
)

@Serializable
data class ClevelandCreator(val description: String? = null, val role: String? = null)

@Serializable
data class ClevelandDate(
    @SerialName("earliest") val earliestYear: Int? = null,
    @SerialName("latest") val latestYear: Int? = null,
)

@Serializable
data class ClevelandImages(
    val web: ClevelandImage? = null,
    val print: ClevelandImage? = null,
)

@Serializable
data class ClevelandImage(val url: String? = null, val width: Int? = null, val height: Int? = null)

@Serializable
data class ClevelandProvenance(val description: String? = null)
