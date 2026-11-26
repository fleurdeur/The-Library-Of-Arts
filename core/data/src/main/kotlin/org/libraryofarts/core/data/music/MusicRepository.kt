package org.libraryofarts.core.data.music

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.MusicEra
import org.libraryofarts.core.model.ReelTrack
import org.libraryofarts.core.model.eraForArtwork
import org.libraryofarts.core.network.NetworkFactory

/**
 * Finds period music for a reel from several open libraries:
 * Wikimedia Commons, Openverse and the Internet Archive. Only public-domain,
 * CC0 or attribution licences are kept, and every track carries its credit.
 */
@Singleton
class MusicRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val client: OkHttpClient,
) {
    private val json = NetworkFactory.json
    private val settings = context.getSharedPreferences("reel_music", Context.MODE_PRIVATE)
    private val cache = context.getSharedPreferences("reel_music_cache", Context.MODE_PRIVATE)
    private val _muted = MutableStateFlow(settings.getBoolean(KEY_MUTED, false))
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    fun setMuted(muted: Boolean) {
        settings.edit().putBoolean(KEY_MUTED, muted).apply()
        _muted.value = muted
    }

    suspend fun trackFor(artwork: Artwork): ReelTrack? {
        val tracks = tracks(eraForArtwork(artwork))
        if (tracks.isEmpty()) return null
        val index = (artwork.id.hashCode().toLong().absoluteValue % tracks.size).toInt()
        return tracks[index]
    }

    suspend fun tracks(era: MusicEra): List<ReelTrack> = withContext(Dispatchers.IO) {
        val cached = readCache(era)
        val fresh = cached != null &&
            System.currentTimeMillis() - cached.first < CACHE_TTL &&
            cached.second.size >= 2
        if (fresh && cached != null) return@withContext cached.second

        val fetched = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
            runCatching { fetchAll(era) }.getOrDefault(emptyList())
        }.orEmpty()
        val merged = (fetched + cached?.second.orEmpty() + curated(era))
            .filter { it.streamUrl.startsWith("https://") }
            .distinctBy { it.streamUrl }
        if (fetched.isNotEmpty()) writeCache(era, merged.take(48))
        merged
    }

    private suspend fun fetchAll(era: MusicEra): List<ReelTrack> = coroutineScope {
        listOf(
            async(Dispatchers.IO) { runCatching { commons(era) }.getOrDefault(emptyList()) },
            async(Dispatchers.IO) { runCatching { openverse(era) }.getOrDefault(emptyList()) },
            async(Dispatchers.IO) { runCatching { archive(era) }.getOrDefault(emptyList()) },
        ).awaitAll().flatten()
    }

    private fun commons(era: MusicEra): List<ReelTrack> {
        val search = URLEncoder.encode(era.query + " filetype:audio", "UTF-8")
        val url = "https://commons.wikimedia.org/w/api.php?action=query&format=json" +
            "&generator=search&gsrsearch=" + search +
            "&gsrnamespace=6&gsrlimit=16&prop=imageinfo&iiprop=url%7Cextmetadata%7Csize"
        val root = get(url) ?: return emptyList()
        val pages = root["query"]?.jsonObject?.get("pages")?.jsonObject ?: return emptyList()
        return pages.values.mapNotNull { it as? JsonObject }
            .sortedBy { it["index"]?.jsonPrimitive?.intOrNull ?: 0 }
            .mapNotNull { page ->
                val info = page["imageinfo"]?.jsonArray?.firstOrNull()?.jsonObject
                    ?: return@mapNotNull null
                val streamUrl = info["url"]?.jsonPrimitive?.contentOrNull
                    ?.substringBefore("?")
                    ?: return@mapNotNull null
                if (AUDIO_EXTENSIONS.none { streamUrl.lowercase().endsWith(it) }) return@mapNotNull null
                val size = info["size"]?.jsonPrimitive?.longOrNull ?: 0L
                if (size in 1..119_999L) return@mapNotNull null
                if (size > 12_000_000L) return@mapNotNull null
                val meta = info["extmetadata"]?.jsonObject
                val license = meta?.value("LicenseShortName").orEmpty().cleanHtml()
                if (!allowedLicense(license)) return@mapNotNull null
                val title = page["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    .removePrefix("File:")
                    .replace(EXTENSION_REGEX, "")
                    .cleanHtml()
                if (title.isBlank()) return@mapNotNull null
                if (SPOKEN_WORDS.any { it in title.lowercase() }) return@mapNotNull null
                val artist = meta?.value("Artist").orEmpty().cleanHtml()
                val credit = meta?.value("Credit").orEmpty().cleanHtml()
                val pageUrl = info["descriptionurl"]?.jsonPrimitive?.contentOrNull.orEmpty()
                ReelTrack(
                    id = "commons:" + streamUrl.hashCode(),
                    title = title,
                    performer = artist.ifBlank { credit },
                    era = era,
                    streamUrl = streamUrl,
                    pageUrl = pageUrl,
                    license = license,
                    licenseUrl = licenseUrlFor(license),
                    source = "Wikimedia Commons",
                    attribution = "Wikimedia Commons · " + license,
                )
            }
    }

    private fun openverse(era: MusicEra): List<ReelTrack> {
        val query = URLEncoder.encode(era.query, "UTF-8")
        val url = "https://api.openverse.org/v1/audio/?q=" + query +
            "&license=cc0,pdm&page_size=20"
        val root = get(url) ?: return emptyList()
        val results = root["results"]?.jsonArray ?: return emptyList()
        return results.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val streamUrl = item.text("url")
            if (streamUrl.isBlank() || "freesound.org" in streamUrl) return@mapNotNull null
            if (AUDIO_EXTENSIONS.none { streamUrl.lowercase().endsWith(it) }) return@mapNotNull null
            val title = item.text("title").cleanHtml()
            if (title.length < 3 || title.startsWith("LL-") || "pronunciation" in title.lowercase()) {
                return@mapNotNull null
            }
            val duration = item["duration"]?.jsonPrimitive?.longOrNull ?: 0L
            if (duration in 1..59_999) return@mapNotNull null
            val rawLicense = item.text("license").lowercase()
            val version = item.text("license_version")
            val license = when (rawLicense) {
                "cc0" -> "CC0" + if (version.isBlank()) "" else " " + version
                "pdm" -> "Public domain"
                else -> return@mapNotNull null
            }
            val creator = item.text("creator").cleanHtml()
            val provider = item.text("source")
            ReelTrack(
                id = "openverse:" + streamUrl.hashCode(),
                title = title,
                performer = creator,
                era = era,
                durationMs = duration,
                streamUrl = streamUrl,
                pageUrl = item.text("foreign_landing_url"),
                license = license,
                licenseUrl = item.text("license_url"),
                source = "Openverse" + if (provider.isBlank()) "" else " · " + provider,
                attribution = "Openverse" + if (provider.isBlank()) "" else " · " + provider,
            )
        }
    }

    private fun archive(era: MusicEra): List<ReelTrack> {
        val query = "mediatype:audio AND licenseurl:(*publicdomain* OR *zero*) AND " +
            "(subject:" + era.label + " OR title:" + era.label + ")"
        val url = "https://archive.org/advancedsearch.php?q=" + URLEncoder.encode(query, "UTF-8") +
            "&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=creator&fl%5B%5D=licenseurl" +
            "&rows=5&page=1&output=json&sort%5B%5D=downloads+desc"
        val root = get(url) ?: return emptyList()
        val docs = root["response"]?.jsonObject?.get("docs")?.jsonArray ?: return emptyList()
        val out = mutableListOf<ReelTrack>()
        for (element in docs.take(3)) {
            val doc = element as? JsonObject ?: continue
            val identifier = doc.text("identifier")
            if (identifier.isBlank()) continue
            val detail = get("https://archive.org/metadata/" + Uri.encode(identifier)) ?: continue
            val metadata = detail["metadata"]?.jsonObject ?: continue
            val files = detail["files"]?.jsonArray ?: continue
            val file = files.mapNotNull { it as? JsonObject }
                .filter { candidate ->
                    val name = candidate.text("name").lowercase()
                    AUDIO_EXTENSIONS.any { name.endsWith(it) }
                }
                .sortedByDescending { it.text("name").lowercase().endsWith(".mp3") }
                .firstOrNull { candidate ->
                    val name = candidate.text("name").lowercase()
                    val size = candidate.text("size").toLongOrNull() ?: 0L
                    val seconds = candidate.text("length").toDoubleOrNull() ?: 0.0
                    seconds in 60.0..1200.0 && size in 100_000L..45_000_000L && "sample" !in name
                } ?: continue
            val licenseUrl = metadata.text("licenseurl").ifBlank { doc.text("licenseurl") }
            if (licenseUrl.isBlank()) continue
            if ("publicdomain" !in licenseUrl && "zero" !in licenseUrl) continue
            val license = if ("zero" in licenseUrl) "CC0 1.0" else "Public domain"
            val seconds = file.text("length").toDoubleOrNull() ?: 0.0
            out += ReelTrack(
                id = "archive:" + identifier,
                title = metadata.text("title").ifBlank { doc.text("title") },
                composer = metadata.text("creator").ifBlank { doc.text("creator") },
                era = era,
                durationMs = (seconds * 1000).toLong(),
                streamUrl = "https://archive.org/download/" + Uri.encode(identifier) + "/" +
                    Uri.encode(file.text("name")),
                pageUrl = "https://archive.org/details/" + Uri.encode(identifier),
                license = license,
                licenseUrl = licenseUrl,
                source = "Internet Archive",
                attribution = "Internet Archive · " + license,
            )
        }
        return out
    }

    private fun get(url: String): JsonObject? = runCatching {
        val request = Request.Builder().url(url).header("Accept", "application/json").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val body = response.body?.string() ?: return@use null
            json.parseToJsonElement(body) as? JsonObject
        }
    }.getOrNull()

    private fun readCache(era: MusicEra): Pair<Long, List<ReelTrack>>? {
        val at = cache.getLong(era.name + KEY_SUFFIX_AT, 0L)
        val raw = cache.getString(era.name + KEY_SUFFIX_TRACKS, null) ?: return null
        if (at == 0L) return null
        val tracks = runCatching { json.decodeFromString<List<ReelTrack>>(raw) }.getOrNull()
            ?: return null
        return at to tracks
    }

    private fun writeCache(era: MusicEra, tracks: List<ReelTrack>) {
        cache.edit()
            .putLong(era.name + KEY_SUFFIX_AT, System.currentTimeMillis())
            .putString(era.name + KEY_SUFFIX_TRACKS, json.encodeToString(tracks))
            .apply()
    }

    private fun curated(era: MusicEra): List<ReelTrack> = when (era) {
        MusicEra.MEDIEVAL -> listOf(
            curatedTrack(
                era = era,
                title = "A Chantar",
                performer = "Beatritz de Dia · Makemi",
                streamUrl = "https://upload.wikimedia.org/wikipedia/commons/d/d4/A_Chantar2.ogg",
                pageUrl = "https://commons.wikimedia.org/wiki/File:A_Chantar2.ogg",
                license = "Public domain",
            ),
            curatedTrack(
                era = era,
                title = "O frondens virga",
                performer = "Hildegard von Bingen · Makemi",
                streamUrl = "https://upload.wikimedia.org/wikipedia/commons/a/ad/O_frondens_2.ogg",
                pageUrl = "https://commons.wikimedia.org/wiki/File:O_frondens_2.ogg",
                license = "Public domain",
            ),
        )
        MusicEra.RENAISSANCE -> listOf(
            curatedTrack(
                era = era,
                title = "Phanthasia Primi Toni (Gdańsk Tablature)",
                performer = "Sk8london",
                streamUrl = "https://upload.wikimedia.org/wikipedia/commons/9/99/Phanthasia_Primi_Toni_from_the_Gda%C5%84sk_Tablature.ogg",
                pageUrl = "https://commons.wikimedia.org/wiki/File:Phanthasia_Primi_Toni_from_the_Gda%C5%84sk_Tablature.ogg",
                license = "CC BY-SA 4.0",
            ),
        )
        MusicEra.BAROQUE -> listOf(
            curatedTrack(
                era = era,
                title = "Agnus Dei (Missa a 5 veus)",
                performer = "Perezmartin40",
                streamUrl = "https://upload.wikimedia.org/wikipedia/commons/8/8a/Agnus_Dei_%28Missa_a_5_veus%29.ogg",
                pageUrl = "https://commons.wikimedia.org/wiki/File:Agnus_Dei_(Missa_a_5_veus).ogg",
                license = "CC BY-SA 4.0",
            ),
        )
        MusicEra.CLASSICAL -> listOf(
            curatedTrack(
                era = era,
                title = "Two-Part Invention in F minor",
                performer = "J. S. Bach · Philip Kanellopoulos",
                streamUrl = "https://upload.wikimedia.org/wikipedia/commons/f/f8/Two-Part_Invention_in_F-minor_by_Philip_Kanellopoulos.ogg",
                pageUrl = "https://commons.wikimedia.org/wiki/File:Two-Part_Invention_in_F-minor_by_Philip_Kanellopoulos.ogg",
                license = "CC0 1.0",
            ),
        )
        MusicEra.ROMANTIC -> listOf(
            curatedTrack(
                era = era,
                title = "Romantic Pieces, III",
                performer = "Antonín Dvořák",
                streamUrl = "https://upload.wikimedia.org/wikipedia/commons/7/71/Dvo%C5%99%C3%A1k_-_Romantic_Pieces_-_III.ogg",
                pageUrl = "https://commons.wikimedia.org/wiki/File:Dvo%C5%99%C3%A1k_-_Romantic_Pieces_-_III.ogg",
                license = "CC0 1.0",
            ),
        )
        MusicEra.IMPRESSIONIST, MusicEra.MODERN -> listOf(
            curatedTrack(
                era = era,
                title = "Clair de lune",
                performer = "Claude Debussy · Jason M. C., Han",
                streamUrl = "https://upload.wikimedia.org/wikipedia/commons/d/d3/Moonlight_-_Clair_de_Lune_%28As_the_rememberance_of_Sir_Debussy.ogg",
                pageUrl = "https://commons.wikimedia.org/wiki/File:Moonlight_-_Clair_de_Lune_(As_the_rememberance_of_Sir_Debussy.ogg",
                license = "CC BY-SA 4.0",
            ),
        )
    }

    private fun curatedTrack(
        era: MusicEra,
        title: String,
        performer: String,
        streamUrl: String,
        pageUrl: String,
        license: String,
    ) = ReelTrack(
        id = "curated:" + streamUrl.hashCode(),
        title = title,
        performer = performer,
        era = era,
        streamUrl = streamUrl,
        pageUrl = pageUrl,
        license = license,
        licenseUrl = licenseUrlFor(license),
        source = "Wikimedia Commons",
        attribution = "Wikimedia Commons · " + license,
    )

    private fun allowedLicense(license: String): Boolean {
        val value = license.lowercase()
        if (value.isBlank()) return false
        if ("public domain" in value || "cc0" in value) return true
        if (!value.startsWith("cc by")) return false
        return "-nc" !in value && "-nd" !in value
    }

    private fun licenseUrlFor(license: String): String {
        val value = license.lowercase()
        val version = Regex("""[0-9]+(\.[0-9]+)?""").find(value)?.value.orEmpty()
        return when {
            "public domain" in value -> "https://creativecommons.org/publicdomain/mark/1.0/"
            "cc0" in value -> "https://creativecommons.org/publicdomain/zero/1.0/"
            "by-sa" in value -> "https://creativecommons.org/licenses/by-sa/" +
                version.ifBlank { "4.0" } + "/"
            "by" in value -> "https://creativecommons.org/licenses/by/" +
                version.ifBlank { "4.0" } + "/"
            else -> ""
        }
    }

    private fun JsonObject.value(key: String): String =
        this[key]?.jsonObject?.get("value")?.jsonPrimitive?.contentOrNull.orEmpty()

    private fun JsonObject.text(key: String): String {
        val element = this[key] ?: return ""
        return when (element) {
            is JsonPrimitive -> element.contentOrNull.orEmpty()
            is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                .joinToString(", ")
            else -> ""
        }
    }

    private fun String.cleanHtml(): String = replace(Regex("<[^>]*>"), " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private companion object {
        const val KEY_MUTED = "muted"
        const val KEY_SUFFIX_AT = "_at"
        const val KEY_SUFFIX_TRACKS = "_tracks"
        const val CACHE_TTL = 3L * 24 * 60 * 60 * 1000
        const val FETCH_TIMEOUT_MS = 14_000L
        val AUDIO_EXTENSIONS = listOf(".ogg", ".oga", ".mp3", ".wav", ".flac", ".opus", ".m4a")
        val SPOKEN_WORDS = listOf(
            "pronunciation",
            "spoken",
            "audio article",
            "wikipedia audio",
            "wikipedia -",
            "podcast",
            "interview",
            "lecture",
        )
        val EXTENSION_REGEX = Regex("\\.(ogg|oga|mp3|wav|flac|opus|m4a)$", RegexOption.IGNORE_CASE)
    }
}
