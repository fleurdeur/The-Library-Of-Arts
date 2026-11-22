package org.libraryofarts.core.network.source

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.libraryofarts.core.model.ArtSource
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.License
import org.libraryofarts.core.model.PaletteColor
import org.libraryofarts.core.network.MuseumRemoteSource
import org.libraryofarts.core.network.mapper.MapperSupport
import org.libraryofarts.core.network.model.AicApi
import javax.inject.Inject

private const val AIC_FIELDS =
    "id,title,artist_display,date_display,date_start,date_end,medium_display,dimensions," +
        "department_title,classification_title,style_title,image_id,thumbnail,color,description," +
        "provenance_text,is_public_domain,credit_line,gallery_title,accession_number,place_of_origin"

class AicSource @Inject constructor(
    private val api: AicApi,
) : MuseumRemoteSource {
    override val source: ArtSource = ArtSource.ART_INSTITUTE
    override val isConfigured: Boolean = true

    override suspend fun highlights(pageSize: Int): List<Artwork> =
        api.artworks(page = 1, limit = pageSize, fields = AIC_FIELDS)
            .data.mapNotNull { it.toArtwork() }

    override suspend fun search(query: String, page: Int, pageSize: Int): List<Artwork> =
        api.artworks(
            page = page + 1,
            limit = pageSize,
            fields = AIC_FIELDS,
            query = query.takeIf { it.isNotBlank() },
        ).data.mapNotNull { it.toArtwork() }

    override suspend fun artwork(sourceId: String): Artwork? =
        runCatching { api.artwork(sourceId).data?.toArtwork() }.getOrNull()

    private fun AicArtwork.toArtwork(): Artwork? {
        if (!isPublicDomain || imageId.isNullOrBlank()) return null
        val iiif = "https://www.artic.edu/iiif/2/$imageId"
        val full = "$iiif/full/1686,/0/default.jpg"
        val thumb = "$iiif/full/600,/0/default.jpg"
        val credit = creditLine.orEmpty()
        val palette = color?.let { c ->
            val h = c.h
            val sat = c.s
            val light = c.l
            if (h == null || sat == null || light == null) {
                null
            } else {
                val argb = hslToArgb(h, sat, light)
                listOf(
                    PaletteColor(argb, "#%06X".format(argb and 0xFFFFFF), c.population ?: 1f, MapperSupport.colorName(argb)),
                ) + MapperSupport.paletteFor(title).drop(1)
            }
        } ?: MapperSupport.paletteFor(title)

        return MapperSupport.baseArtwork(
            source = source,
            sourceId = id.toString(),
            title = title,
            artistName = artistDisplay,
            yearStart = dateStart,
            yearEnd = dateEnd,
            medium = mediumDisplay,
            genreRaw = classificationTitle ?: departmentTitle,
            image = org.libraryofarts.core.model.ArtworkImage(
                thumbnailUrl = thumb,
                fullUrl = full,
                width = 1686,
                height = 1686,
                iiifBase = iiif,
                creditLine = credit,
                altText = thumbnail?.altText ?: title,
            ),
            license = License.CC0.copy(creditLine = credit),
            description = description?.replace(Regex("<[^>]+>"), "")?.trim()?.takeIf { it.isNotBlank() },
            credit = credit,
            provenance = MapperSupport.provenance(provenanceText),
        ).copy(
            palette = palette,
            movement = styleTitle,
            dimensions = dimensions,
            location = galleryTitle,
            accessionNumber = accessionNumber,
            subjects = listOfNotNull(placeOfOrigin, classificationTitle, departmentTitle).filter { it.isNotBlank() },
        )
    }

    private fun hslToArgb(h: Double, s: Double, l: Double): Long {
        val hue = (h % 360 + 360) % 360
        val sat = s.coerceIn(0.0, 1.0)
        val light = l.coerceIn(0.0, 1.0)
        val c = (1 - kotlin.math.abs(2 * light - 1)) * sat
        val x = c * (1 - kotlin.math.abs((hue / 60) % 2 - 1))
        val m = light - c / 2
        val (r, g, b) = when {
            hue < 60 -> Triple(c, x, 0.0)
            hue < 120 -> Triple(x, c, 0.0)
            hue < 180 -> Triple(0.0, c, x)
            hue < 240 -> Triple(0.0, x, c)
            hue < 300 -> Triple(x, 0.0, c)
            else -> Triple(c, 0.0, x)
        }
        val ri = ((r + m) * 255).toInt().coerceIn(0, 255)
        val gi = ((g + m) * 255).toInt().coerceIn(0, 255)
        val bi = ((b + m) * 255).toInt().coerceIn(0, 255)
        return (0xFFL shl 24) or (ri.toLong() shl 16) or (gi.toLong() shl 8) or bi.toLong()
    }
}

@Serializable
data class AicArtwork(
    val id: Int,
    val title: String = "",
    @SerialName("artist_display") val artistDisplay: String? = null,
    @SerialName("date_display") val dateDisplay: String? = null,
    @SerialName("date_start") val dateStart: Int? = null,
    @SerialName("date_end") val dateEnd: Int? = null,
    @SerialName("medium_display") val mediumDisplay: String? = null,
    val dimensions: String? = null,
    @SerialName("department_title") val departmentTitle: String? = null,
    @SerialName("classification_title") val classificationTitle: String? = null,
    @SerialName("style_title") val styleTitle: String? = null,
    @SerialName("image_id") val imageId: String? = null,
    val thumbnail: AicThumbnail? = null,
    val color: AicColor? = null,
    val description: String? = null,
    @SerialName("provenance_text") val provenanceText: String? = null,
    @SerialName("is_public_domain") val isPublicDomain: Boolean = false,
    @SerialName("credit_line") val creditLine: String? = null,
    @SerialName("gallery_title") val galleryTitle: String? = null,
    @SerialName("accession_number") val accessionNumber: String? = null,
    @SerialName("place_of_origin") val placeOfOrigin: String? = null,
)

@Serializable
data class AicThumbnail(val altText: String? = null)

@Serializable
data class AicColor(
    val h: Double? = null,
    val s: Double? = null,
    val l: Double? = null,
    val population: Float? = null,
)
