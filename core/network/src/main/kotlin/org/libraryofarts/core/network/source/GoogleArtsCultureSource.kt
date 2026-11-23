package org.libraryofarts.core.network.source

import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import org.libraryofarts.core.model.ArtSource
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.ArtworkImage
import org.libraryofarts.core.model.License
import org.libraryofarts.core.model.MuseumCatalog
import org.libraryofarts.core.model.PaletteColor
import org.libraryofarts.core.network.MuseumRemoteSource
import org.libraryofarts.core.network.mapper.MapperSupport
import javax.inject.Inject

/**
 * Google Arts & Culture does not publish an official public API. This adapter
 * reads the same structured painting index that the public search page embeds
 * for its own client. It never bypasses a login or a paywall, it keeps the
 * original museum credit line, and it marks every record as "check source
 * rights" rather than pretending the work is CC0.
 *
 * The adapter is deliberately defensive: Google can change its markup at any
 * time, in which case it returns an empty list and the rest of the library
 * continues to work.
 */
class GoogleArtsCultureSource @Inject constructor(
    private val client: OkHttpClient,
) : MuseumRemoteSource {
    override val source: ArtSource = ArtSource.GOOGLE_ARTS

    override suspend fun highlights(pageSize: Int): List<Artwork> = search("painting", 0, pageSize)

    override suspend fun search(query: String, page: Int, pageSize: Int): List<Artwork> =
        withContext(Dispatchers.IO) {
            val term = query.ifBlank { "painting" }
            val url = "https://artsandculture.google.com/search?q=" +
                URLEncoder.encode(term, "UTF-8") + "&hl=en"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) TheGreatLibraryOfArts/1.1")
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "en")
                .build()
            val html = runCatching {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) response.body?.string() else null
                }
            }.getOrNull() ?: return@withContext emptyList()

            val all = extractPaintings(html)
            all.drop(page.coerceAtLeast(0) * pageSize).take(pageSize)
        }

    private fun extractPaintings(html: String): List<Artwork> {
        val found = mutableListOf<Artwork>()
        val marker = "[\"stella.common.cobject\""
        var cursor = html.indexOf(marker)
        while (cursor >= 0 && found.size < 120) {
            val end = matchingBracket(html, cursor)
            if (end <= cursor) break
            val raw = html.substring(cursor, end + 1)
            runCatching { mapper(raw) }.getOrNull()?.let(found::add)
            cursor = html.indexOf(marker, end + 1)
        }
        return found.distinctBy { it.id }
    }

    private fun mapper(raw: String): Artwork? {
        val array = NetworkJson.parseToJsonElement(raw) as? JsonArray ?: return null
        val title = array.string(1)?.takeIf { it.isNotBlank() } ?: return null
        val artistRaw = array.string(2)?.takeIf { it.isNotBlank() }
        val imagePath = array.string(3)?.takeIf { it.startsWith("//") } ?: return null
        val assetPath = array.string(4)
            ?.takeIf { it.startsWith("/asset/") && !it.contains('�') }
            ?: return null
        val museumName = (array.getOrNull(10) as? JsonArray)?.string(12)
            ?.takeIf { it.isNotBlank() && !it.contains('�') }
            ?: "Google Arts & Culture"
        val sourceId = assetPath.substringAfterLast('/').takeIf { it.isNotBlank() } ?: return null
        val assetUrl = "https://artsandculture.google.com$assetPath"
        val base = "https:$imagePath"
        val (artistName, birth, death) = parseArtist(artistRaw)
        val color = array.string(8)?.removePrefix("#")?.toLongOrNull(16)
        val palette = color?.let {
            listOf(PaletteColor(argb = 0xFF000000L or it, hex = "#%06X".format(it), population = 1f, name = "Source colour"))
        }.orEmpty()
        return MapperSupport.baseArtwork(
            source = source,
            sourceId = sourceId,
            title = title.replace('\uFFFD', ' ').trim(),
            artistName = artistName,
            yearStart = null,
            yearEnd = null,
            medium = "Painting",
            genreRaw = "Painting",
            image = ArtworkImage(
                thumbnailUrl = "$base=w700",
                fullUrl = "$base=w1800",
                width = 1800,
                height = 2200,
                creditLine = museumName,
                altText = "$title${artistName?.let { " by $it" } ?: ""}, via Google Arts & Culture",
            ),
            license = License(
                name = "Google Arts & Culture — verify source rights",
                url = assetUrl,
                isPublicDomain = false,
                requiresAttribution = true,
                creditLine = museumName,
            ),
            description = "Discovered through Google Arts & Culture. The holding collection is $museumName; check the source record before reusing the image.",
            credit = museumName,
            provenance = listOf(
                org.libraryofarts.core.model.ProvenanceEntry(
                    description = "Google Arts & Culture search result",
                    sourceUrl = assetUrl,
                ),
            ),
        ).copy(
            museum = MuseumCatalog.googleArts,
            location = museumName,
            period = null,
            movement = null,
            palette = palette,
            tags = listOf("painting", "Google Arts & Culture", museumName),
            isHighlight = foundHighlight(title),
        )
    }

    private fun foundHighlight(title: String): Boolean =
        listOf("starry night", "great wave", "mona lisa", "girl with a pearl", "sunflowers", "birth of venus")
            .any { title.contains(it, ignoreCase = true) }

    private fun parseArtist(raw: String?): Triple<String?, Int?, Int?> {
        if (raw.isNullOrBlank()) return Triple(null, null, null)
        val name = raw.substringBefore('(').trim().takeIf { it.isNotBlank() }
        val range = Regex("""\((\d{3,4})\s*[-–]\s*(\d{3,4})\)""").find(raw)
        return Triple(name, range?.groupValues?.getOrNull(1)?.toIntOrNull(), range?.groupValues?.getOrNull(2)?.toIntOrNull())
    }

    private fun JsonArray.string(index: Int): String? =
        getOrNull(index)?.let { element ->
            (element as? JsonPrimitive)?.contentOrNull
        }

    private fun matchingBracket(text: String, start: Int): Int {
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until text.length) {
            val c = text[i]
            if (inString) {
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
            } else {
                when (c) {
                    '"' -> inString = true
                    '[' -> depth++
                    ']' -> {
                        depth--
                        if (depth == 0) return i
                    }
                }
            }
            if (i - start > 12_000) return -1
        }
        return -1
    }

    private companion object {
        val NetworkJson = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}
