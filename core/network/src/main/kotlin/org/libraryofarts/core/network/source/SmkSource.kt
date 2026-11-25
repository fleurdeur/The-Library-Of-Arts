package org.libraryofarts.core.network.source

import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.libraryofarts.core.model.ArtSource
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.ArtworkImage
import org.libraryofarts.core.model.License
import org.libraryofarts.core.model.PaletteColor
import org.libraryofarts.core.network.MuseumRemoteSource
import org.libraryofarts.core.network.mapper.MapperSupport
import javax.inject.Inject

/**
 * Statens Museum for Kunst (National Gallery of Denmark). Its search API is
 * open and CC0-labelled records ship IIIF thumbnails, so no key is needed.
 */
class SmkSource @Inject constructor(
    private val client: OkHttpClient,
) : MuseumRemoteSource {
    override val source: ArtSource = ArtSource.SMK
    override val isConfigured: Boolean = true

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun highlights(pageSize: Int): List<Artwork> =
        fetch(pageSize = pageSize, offset = 0, query = null)

    override suspend fun search(query: String, page: Int, pageSize: Int): List<Artwork> =
        fetch(pageSize = pageSize, offset = page.coerceAtLeast(0) * pageSize, query = query.takeIf { it.isNotBlank() })

    private suspend fun fetch(pageSize: Int, offset: Int, query: String?): List<Artwork> =
        withContext(Dispatchers.IO) {
            val url = buildString {
                append("https://api.smk.dk/api/v1/art/search/?keys=")
                append(URLEncoder.encode(query ?: "*", "UTF-8"))
                append("&filters=%5Bhas_image:true%5D%2C%5Bpublic_domain:true%5D")
                append("&page-size=").append(pageSize.coerceIn(1, 60))
                append("&offset=").append(offset)
            }
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()
            val body = runCatching {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) response.body?.string() else null
                }
            }.getOrNull() ?: return@withContext emptyList()
            val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
                ?: return@withContext emptyList()
            val items = root["items"] as? JsonArray ?: return@withContext emptyList()
            items.mapNotNull { element ->
                runCatching { (element as? JsonObject)?.toArtwork() }.getOrNull()
            }
        }

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.toArtwork(): Artwork? {
        val objectNumber = str("object_number") ?: return null
        if (str("public_domain")?.toBooleanStrictOrNull() != true) return null
        val titles = this["titles"] as? JsonArray
        val title = titles?.firstOrNull()
            ?.let { (it as? JsonObject)?.str("title") }
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val production = this["production"] as? JsonArray
        val firstProduction = production?.firstOrNull() as? JsonObject
        val artist = (this["artist"] as? JsonArray)?.firstOrNull()
            ?.let { (it as? JsonObject)?.str("artist") }
            ?: firstProduction?.str("creator")
        val yearRange = (this["production_date"] as? JsonArray)?.firstOrNull()
            ?.let { it as? JsonObject }
        val yearStart = yearRange?.str("start")?.take(4)?.toIntOrNull()
        val yearEnd = yearRange?.str("end")?.take(4)?.toIntOrNull()
        val medium = (this["techniques"] as? JsonArray)?.firstOrNull()?.jsonPrimitive?.contentOrNull
        val objectNames = (this["object_names"] as? JsonArray)
            ?.mapNotNull { (it as? JsonObject)?.str("name") }
        val thumb = str("image_thumbnail")?.replace("!1024,", "!640,")
            ?: run {
                val iiifId = str("image_iiif_id") ?: return@run null
                "$iiifId/full/!640,/0/default.jpg"
            } ?: return null
        val iiifId = str("image_iiif_id") ?: thumb.substringBefore("/full/")
        val colors = (this["colors"] as? JsonArray)
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?.take(4)
            .orEmpty()
        val palette = colors.map { hex ->
            val argb = runCatching { hexToArgb(hex) }.getOrDefault(0xFF2A2622L)
            PaletteColor(argb, hex, 1f, MapperSupport.colorName(argb))
        }.ifEmpty { MapperSupport.paletteFor(title) }
        val dimensions = (this["dimensions"] as? JsonArray)?.mapNotNull { d ->
            (d as? JsonObject)?.let { o -> o.str("type") to o.str("value") }
        }?.joinToString(" × ") { "${it.second} mm" }?.takeIf { it.isNotBlank() }

        return MapperSupport.baseArtwork(
            source = source,
            sourceId = objectNumber,
            title = title,
            artistName = artist,
            yearStart = yearStart,
            yearEnd = yearEnd,
            medium = medium,
            genreRaw = objectNames?.firstOrNull(),
            image = ArtworkImage(
                thumbnailUrl = thumb,
                fullUrl = "$iiifId/full/!1600,/0/default.jpg",
                width = str("image_width")?.toIntOrNull() ?: 1600,
                height = str("image_height")?.toIntOrNull() ?: 1600,
                iiifBase = iiifId.substringBefore("/full/").takeIf { it.startsWith("http") },
                creditLine = "Statens Museum for Kunst, Copenhagen (public domain)",
                altText = title,
            ),
            license = License.PD.copy(
                creditLine = "Statens Museum for Kunst, Copenhagen (public domain)",
            ),
            description = null,
            credit = "Statens Museum for Kunst, Copenhagen",
            provenance = MapperSupport.provenance(str("acquisition_date")),
        ).copy(
            palette = palette,
            dimensions = dimensions,
            location = "Copenhagen",
        )
    }

    private fun hexToArgb(hex: String): Long {
        val value = hex.removePrefix("#")
        if (value.length != 6) return 0xFF2A2622L
        val rgb = value.toLongOrNull(16) ?: return 0xFF2A2622L
        return 0xFF000000L or rgb
    }
}
