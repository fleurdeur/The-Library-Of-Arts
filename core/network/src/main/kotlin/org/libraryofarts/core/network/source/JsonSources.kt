package org.libraryofarts.core.network.source

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.libraryofarts.core.model.ArtSource
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.License
import org.libraryofarts.core.network.MuseumRemoteSource
import org.libraryofarts.core.network.SourceCredentials
import org.libraryofarts.core.network.arr
import org.libraryofarts.core.network.deepFirstString
import org.libraryofarts.core.network.deepString
import org.libraryofarts.core.network.firstString
import org.libraryofarts.core.network.int
import org.libraryofarts.core.network.long
import org.libraryofarts.core.network.mapper.MapperSupport
import org.libraryofarts.core.network.model.GettyApi
import org.libraryofarts.core.network.model.HarvardApi
import org.libraryofarts.core.network.model.ParisMuseesApi
import org.libraryofarts.core.network.model.SmithsonianApi
import org.libraryofarts.core.network.obj
import org.libraryofarts.core.network.objects
import org.libraryofarts.core.network.str
import org.libraryofarts.core.network.stripHtml
import javax.inject.Inject

class SmithsonianSource @Inject constructor(
    private val api: SmithsonianApi,
    private val credentials: SourceCredentials,
) : MuseumRemoteSource {
    override val source: ArtSource = ArtSource.SMITHSONIAN
    override val isConfigured: Boolean = credentials.smithsonianKey.isNotBlank()

    override suspend fun highlights(pageSize: Int): List<Artwork> =
        search("painting", 0, pageSize)

    override suspend fun search(query: String, page: Int, pageSize: Int): List<Artwork> {
        if (!isConfigured) return emptyList()
        val response = api.search(
            key = credentials.smithsonianKey,
            query = query.ifBlank { "painting" },
            rows = pageSize,
            start = page * pageSize,
        )
        return response.obj("response")?.arr("rows")?.objects().orEmpty().mapNotNull { it.toArtwork() }
    }

    private fun JsonObject.toArtwork(): Artwork? {
        val id = str("id") ?: return null
        val media = arr("online_media").objects().firstOrNull()
            ?: obj("content")?.obj("online_media")?.arr("media")?.objects()?.firstOrNull()
        val url = media?.str("content") ?: media?.str("thumbnail") ?: return null
        val content = obj("content") ?: JsonObject(emptyMap())
        val free = content.obj("freetext") ?: JsonObject(emptyMap())
        val narrative = content.obj("descriptiveNonRecessedText") ?: JsonObject(emptyMap())
        val access = content.obj("metadata_usage")?.str("access").orEmpty()
        if (!access.contains("CC0", true) && !access.contains("public domain", true)) return null
        val credit = stripHtml(narrative.str("creditLine")) ?: "Smithsonian Institution"
        return MapperSupport.baseArtwork(
            source = source,
            sourceId = id,
            title = str("title").orEmpty(),
            artistName = free.arr("name").objects().firstOrNull()?.str("content"),
            yearStart = free.arr("date").objects().firstOrNull()?.str("content")?.take(4)?.toIntOrNull(),
            yearEnd = null,
            medium = free.arr("physicalDescription").objects().firstOrNull()?.str("content"),
            genreRaw = str("objectType") ?: content.obj("indexedStructured")?.firstString("object_type"),
            image = org.libraryofarts.core.model.ArtworkImage(
                thumbnailUrl = media?.str("thumbnail") ?: url,
                fullUrl = url,
                width = 1600,
                height = 2000,
                creditLine = credit,
                altText = str("title").orEmpty(),
            ),
            license = License.CC0.copy(creditLine = credit),
            description = stripHtml(narrative.str("content")),
            credit = credit,
        )
    }
}

class HarvardSource @Inject constructor(
    private val api: HarvardApi,
    private val credentials: SourceCredentials,
) : MuseumRemoteSource {
    override val source: ArtSource = ArtSource.HARVARD
    override val isConfigured: Boolean = credentials.harvardKey.isNotBlank()

    override suspend fun highlights(pageSize: Int): List<Artwork> = search("painting", 0, pageSize)

    override suspend fun search(query: String, page: Int, pageSize: Int): List<Artwork> {
        if (!isConfigured) return emptyList()
        val response = api.objects(
            key = credentials.harvardKey,
            query = "hasimage:1 ${query}".trim(),
            size = pageSize,
            page = page + 1,
        )
        return response.arr("records").objects().mapNotNull { it.toArtwork() }
    }

    private fun JsonObject.toArtwork(): Artwork? {
        if (int("imagepermissionlevel") != 0) return null
        val id = long("objectid") ?: long("id") ?: return null
        val url = str("primaryimageurl") ?: return null
        val credit = str("creditline") ?: "Harvard Art Museums"
        val people = arr("people").objects()
        val artist = people.firstOrNull { it.str("role")?.contains("Artist", true) == true }?.str("name")
            ?: people.firstOrNull()?.str("name")
        return MapperSupport.baseArtwork(
            source = source,
            sourceId = id.toString(),
            title = str("title").orEmpty(),
            artistName = artist,
            yearStart = str("dated")?.take(4)?.toIntOrNull(),
            yearEnd = null,
            medium = str("medium"),
            genreRaw = str("classification"),
            image = org.libraryofarts.core.model.ArtworkImage(
                thumbnailUrl = url,
                fullUrl = url,
                width = int("width") ?: 1600,
                height = int("height") ?: 2000,
                creditLine = credit,
                altText = str("title").orEmpty(),
            ),
            license = License.CC0.copy(creditLine = credit),
            description = stripHtml(str("labeltext") ?: str("description")),
            credit = credit,
            provenance = MapperSupport.provenance(str("provenance")),
        ).copy(
            dimensions = str("dimensions"),
            period = str("century") ?: str("period"),
        )
    }
}

class ParisMuseesSource @Inject constructor(
    private val api: ParisMuseesApi,
    private val credentials: SourceCredentials,
) : MuseumRemoteSource {
    override val source: ArtSource = ArtSource.PARIS_MUSEES
    override val isConfigured: Boolean = credentials.parisMuseesKey.isNotBlank()

    override suspend fun highlights(pageSize: Int): List<Artwork> = search("peinture", 0, pageSize)

    override suspend fun search(query: String, page: Int, pageSize: Int): List<Artwork> {
        if (!isConfigured) return emptyList()
        val body = buildJsonObject {
            put(
                "query",
                """
                query Works(${'$'}limit: Int, ${'$'}search: String) {
                  arItems(first: ${'$'}limit, search: ${'$'}search) {
                    edges { node { id title creator date image credit } }
                  }
                }
                """.trimIndent(),
            )
            put(
                "variables",
                buildJsonObject {
                    put("limit", pageSize)
                    put("search", query.ifBlank { "peinture" })
                },
            )
        }
        val response = api.graphql(credentials.parisMuseesKey, body)
        val edges = response.obj("data")?.obj("arItems")?.arr("edges")
            ?: response.obj("data")?.obj("items")?.arr("edges")
            ?: return emptyList()
        return edges.objects().mapNotNull { edge -> edge.obj("node")?.toArtwork() }
    }

    private fun JsonObject.toArtwork(): Artwork? {
        val id = str("id") ?: return null
        val url = str("image") ?: str("imageUrl") ?: return null
        val credit = str("credit") ?: "Paris Musées"
        return MapperSupport.baseArtwork(
            source = source,
            sourceId = id,
            title = str("title").orEmpty(),
            artistName = str("creator"),
            yearStart = str("date")?.take(4)?.toIntOrNull(),
            yearEnd = null,
            medium = null,
            genreRaw = "painting",
            image = org.libraryofarts.core.model.ArtworkImage(
                thumbnailUrl = url,
                fullUrl = url,
                width = 1600,
                height = 2000,
                creditLine = credit,
                altText = str("title").orEmpty(),
            ),
            license = License.CC0.copy(creditLine = credit),
            description = null,
            credit = credit,
        )
    }
}

class GettySource @Inject constructor(
    private val api: GettyApi,
    private val credentials: SourceCredentials,
) : MuseumRemoteSource {
    override val source: ArtSource = ArtSource.GETTY
    override val isConfigured: Boolean = credentials.gettyKey.isNotBlank()

    override suspend fun highlights(pageSize: Int): List<Artwork> = search("painting", 0, pageSize)

    override suspend fun search(query: String, page: Int, pageSize: Int): List<Artwork> {
        if (!isConfigured) return emptyList()
        val response = api.objects(
            key = credentials.gettyKey,
            query = query.takeIf { it.isNotBlank() },
            limit = pageSize,
            offset = page * pageSize,
        )
        val objects = (response["objects"] as? kotlinx.serialization.json.JsonArray)?.takeIf { it.isNotEmpty() }
            ?: response.arr("data")
        return objects.objects().mapNotNull { it.toArtwork() }
    }

    private fun JsonObject.toArtwork(): Artwork? {
        val id = str("objectId") ?: str("id") ?: return null
        val url = str("primaryImage") ?: str("image") ?: return null
        val credit = str("creditLine") ?: "The J. Paul Getty Museum, Los Angeles"
        return MapperSupport.baseArtwork(
            source = source,
            sourceId = id,
            title = str("title").orEmpty(),
            artistName = str("artist") ?: str("artistName"),
            yearStart = str("date")?.take(4)?.toIntOrNull(),
            yearEnd = null,
            medium = str("medium"),
            genreRaw = str("classification") ?: "painting",
            image = org.libraryofarts.core.model.ArtworkImage(
                thumbnailUrl = url,
                fullUrl = url,
                width = int("width") ?: 1600,
                height = int("height") ?: 2000,
                creditLine = credit,
                altText = str("title").orEmpty(),
            ),
            license = License.CC0.copy(creditLine = credit),
            description = stripHtml(str("description")),
            credit = credit,
        )
    }
}
