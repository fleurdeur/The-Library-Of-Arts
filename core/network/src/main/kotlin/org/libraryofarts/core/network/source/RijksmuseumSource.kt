package org.libraryofarts.core.network.source

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.libraryofarts.core.model.ArtSource
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.License
import org.libraryofarts.core.network.MuseumRemoteSource
import org.libraryofarts.core.network.SourceCredentials
import org.libraryofarts.core.network.imageUrlOrEmpty
import org.libraryofarts.core.network.mapper.MapperSupport
import org.libraryofarts.core.network.model.RijksApi
import javax.inject.Inject

class RijksmuseumSource @Inject constructor(
    private val api: RijksApi,
    private val credentials: SourceCredentials,
) : MuseumRemoteSource {
    override val source: ArtSource = ArtSource.RIJKSMUSEUM
    override val isConfigured: Boolean = credentials.rijksmuseumKey.isNotBlank()

    override suspend fun highlights(pageSize: Int): List<Artwork> =
        search("Rembrandt", page = 0, pageSize = pageSize)

    override suspend fun search(query: String, page: Int, pageSize: Int): List<Artwork> {
        if (!isConfigured) return emptyList()
        return api.artworks(
            key = credentials.rijksmuseumKey,
            query = query.ifBlank { "painting" },
            page = page + 1,
            pageSize = pageSize,
        ).artObjects.mapNotNull { it.toArtwork() }
    }

    private fun RijksObject.toArtwork(): Artwork? {
        if (!hasImage || !permitDownload) return null
        val image = headerImage ?: webImage ?: return null
        val url = image.url.imageUrlOrEmpty()
        if (url.isBlank()) return null
        val credit = "Rijksmuseum, Amsterdam. Public domain."
        return MapperSupport.baseArtwork(
            source = source,
            sourceId = objectNumber,
            title = longTitle.ifBlank { title },
            artistName = principalOrFirstMaker,
            yearStart = null,
            yearEnd = null,
            medium = physicalMedium,
            genreRaw = objectType,
            image = org.libraryofarts.core.model.ArtworkImage(
                thumbnailUrl = url,
                fullUrl = url,
                width = image.width ?: 1600,
                height = image.height ?: 2000,
                creditLine = credit,
                altText = title,
            ),
            license = License.CC0.copy(creditLine = credit),
            description = null,
            credit = credit,
            provenance = MapperSupport.provenance(provenance),
        ).copy(
            year = dating?.presentationDate ?: "Unknown date",
            period = MapperSupport.periodFor(null),
            location = productionPlaces.firstOrNull()?.let { "Made in $it" },
            subjects = productionPlaces,
        )
    }
}

@Serializable
data class RijksResponse(val artObjects: List<RijksObject> = emptyList())

@Serializable
data class RijksObject(
    val objectNumber: String,
    val title: String = "",
    val longTitle: String = "",
    @SerialName("principalOrFirstMaker") val principalOrFirstMaker: String? = null,
    val webImage: RijksImage? = null,
    val headerImage: RijksImage? = null,
    val hasImage: Boolean = false,
    val permitDownload: Boolean = false,
    @SerialName("objectType") val objectType: String? = null,
    @SerialName("physicalMedium") val physicalMedium: String? = null,
    val productionPlaces: List<String> = emptyList(),
    val dating: RijksDating? = null,
    val provenance: String? = null,
)

@Serializable
data class RijksImage(
    val url: String = "",
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
data class RijksDating(val presentationDate: String? = null)
