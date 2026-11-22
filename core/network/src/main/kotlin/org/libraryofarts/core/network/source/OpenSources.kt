package org.libraryofarts.core.network.source

import kotlinx.serialization.json.JsonObject
import org.libraryofarts.core.model.ArtSource
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.License
import org.libraryofarts.core.model.Museum
import org.libraryofarts.core.model.MuseumCatalog
import org.libraryofarts.core.network.MuseumRemoteSource
import org.libraryofarts.core.network.arr
import org.libraryofarts.core.network.deepFirstString
import org.libraryofarts.core.network.deepString
import org.libraryofarts.core.network.firstString
import org.libraryofarts.core.network.int
import org.libraryofarts.core.network.mapper.MapperSupport
import org.libraryofarts.core.network.model.EuropeanaApi
import org.libraryofarts.core.network.model.WikimediaApi
import org.libraryofarts.core.network.obj
import org.libraryofarts.core.network.objects
import org.libraryofarts.core.network.str
import org.libraryofarts.core.network.stripHtml
import javax.inject.Inject

class WikimediaSource @Inject constructor(
    private val api: WikimediaApi,
) : MuseumRemoteSource {
    override val source: ArtSource = ArtSource.WIKIMEDIA
    override val isConfigured: Boolean = true

    override suspend fun highlights(pageSize: Int): List<Artwork> =
        search("painting public domain", 0, pageSize)

    override suspend fun search(query: String, page: Int, pageSize: Int): List<Artwork> {
        val response = api.search(
            search = "filetype:bitmap ${query.ifBlank { "public domain painting" }}",
            limit = pageSize,
            offset = page * pageSize,
        )
        val pages = response.obj("query")?.obj("pages") ?: return emptyList()
        return pages.values.mapNotNull { element ->
            (element as? JsonObject)?.toArtwork()
        }
    }

    private fun JsonObject.toArtwork(): Artwork? {
        val pageId = int("pageid") ?: return null
        val info = arr("imageinfo").objects().firstOrNull() ?: return null
        val url = info.str("thumburl") ?: info.str("url") ?: return null
        val meta = info.obj("extmetadata") ?: JsonObject(emptyMap())
        val licenseName = meta.obj("LicenseShortName")?.str("value") ?: "Public domain"
        val isOpen = licenseName.contains("public", true) ||
            licenseName.contains("cc0", true) ||
            licenseName.contains("cc by", true)
        if (!isOpen) return null
        val artist = stripHtml(meta.obj("Artist")?.str("value"))
        val title = str("title").orEmpty()
            .removePrefix("File:")
            .substringBeforeLast('.')
            .replace('_', ' ')
        val credit = stripHtml(meta.obj("Credit")?.str("value"))
            ?: "Wikimedia Commons"
        val license = if (licenseName.contains("public", true) || licenseName.contains("cc0", true)) {
            License.PD.copy(creditLine = credit)
        } else {
            License(
                name = licenseName,
                spdxId = licenseName,
                isPublicDomain = false,
                requiresAttribution = true,
                creditLine = credit,
            )
        }
        val description = stripHtml(meta.obj("ImageDescription")?.str("value"))
        return MapperSupport.baseArtwork(
            source = source,
            sourceId = pageId.toString(),
            title = title.ifBlank { "Untitled" },
            artistName = artist,
            yearStart = null,
            yearEnd = null,
            medium = null,
            genreRaw = "painting",
            image = org.libraryofarts.core.model.ArtworkImage(
                thumbnailUrl = url,
                fullUrl = info.str("url") ?: url,
                width = info.int("width") ?: 1600,
                height = info.int("height") ?: 2000,
                creditLine = credit,
                altText = title,
            ),
            license = license,
            description = description,
            credit = credit,
        )
    }
}

class EuropeanaSource @Inject constructor(
    private val api: EuropeanaApi,
    private val credentials: org.libraryofarts.core.network.SourceCredentials,
) : MuseumRemoteSource {
    override val source: ArtSource = ArtSource.EUROPEANA
    override val isConfigured: Boolean = credentials.europeanaKey.isNotBlank()

    override suspend fun highlights(pageSize: Int): List<Artwork> =
        search("painting", 0, pageSize)

    override suspend fun search(query: String, page: Int, pageSize: Int): List<Artwork> {
        if (!isConfigured) return emptyList()
        val response = api.search(
            key = credentials.europeanaKey,
            query = query.ifBlank { "painting" },
            rows = pageSize,
            start = page * pageSize + 1,
        )
        return response.arr("items").objects().mapNotNull { it.toArtwork() }
    }

    private fun JsonObject.toArtwork(): Artwork? {
        val id = str("id") ?: return null
        val rights = firstString("rights").orEmpty()
        val open = rights.contains("publicdomain", true) || rights.contains("cc0", true) ||
            rights.contains("cc-by", true) || rights.contains("cc by", true)
        if (!open) return null
        val url = firstString("edmIsShownBy") ?: firstString("edmPreview") ?: return null
        val provider = firstString("dataProvider") ?: "Europeana"
        val museum = MuseumCatalog.europeana.copy(
            name = provider,
            shortName = provider.take(28),
        )
        val credit = "$provider via Europeana"
        val license = if (rights.contains("publicdomain", true) || rights.contains("cc0", true)) {
            License.PD.copy(creditLine = credit)
        } else {
            License(name = rights, spdxId = rights, isPublicDomain = false, requiresAttribution = true, creditLine = credit)
        }
        return MapperSupport.baseArtwork(
            source = source,
            sourceId = id,
            title = firstString("title").orEmpty(),
            artistName = firstString("dcCreator"),
            yearStart = firstString("year")?.take(4)?.toIntOrNull(),
            yearEnd = null,
            medium = firstString("dcFormat"),
            genreRaw = firstString("type"),
            image = org.libraryofarts.core.model.ArtworkImage(
                thumbnailUrl = url,
                fullUrl = firstString("edmIsShownBy") ?: url,
                width = 1600,
                height = 2000,
                creditLine = credit,
                altText = firstString("title").orEmpty(),
            ),
            license = license,
            description = firstString("dcDescription"),
            credit = credit,
            museumOverride = museum,
        )
    }
}
