package org.libraryofarts.core.model

import kotlinx.serialization.Serializable

/**
 * A public-domain or openly licensed recording used behind a reel. Tracks are
 * fetched from open music libraries and always carry their licence and a link
 * back to the source so the app can show attribution.
 */
@Serializable
data class ReelTrack(
    val id: String,
    val title: String,
    val composer: String = "",
    val performer: String = "",
    val era: MusicEra = MusicEra.CLASSICAL,
    val durationMs: Long = 0L,
    val streamUrl: String,
    val pageUrl: String = "",
    val license: String = "",
    val licenseUrl: String = "",
    val source: String = "",
    val attribution: String = "",
) {
    val displayTitle: String get() = title.ifBlank { "Untitled recording" }
    val displayArtist: String get() = listOf(composer, performer)
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" · ")
        .ifBlank { attribution.ifBlank { source } }
}

enum class MusicEra(val label: String, val query: String) {
    MEDIEVAL("Medieval", "medieval music"),
    RENAISSANCE("Renaissance", "renaissance music"),
    BAROQUE("Baroque", "baroque music"),
    CLASSICAL("Classical", "classical music"),
    ROMANTIC("Romantic", "romantic music"),
    IMPRESSIONIST("Impressionist", "impressionist music"),
    MODERN("Modern", "modern classical music"),
}

/** Picks the period soundtrack that suits an artwork. */
fun eraForArtwork(artwork: Artwork): MusicEra {
    val text = buildString {
        append(artwork.period.orEmpty()).append(' ')
        append(artwork.movement.orEmpty()).append(' ')
        append(artwork.genre.orEmpty()).append(' ')
        append(artwork.tags.joinToString(" ")).append(' ')
        append(artwork.subjects.joinToString(" "))
    }.lowercase()

    fun has(vararg keys: String) = keys.any { it in text }

    return when {
        has("medieval", "gothic", "byzantine", "romanesque", "icon", "illumination", "manuscript") -> MusicEra.MEDIEVAL
        has("renaissance", "mannerism", "high renaissance") -> MusicEra.RENAISSANCE
        has("baroque", "rococo", "still life", "vanitas") -> MusicEra.BAROQUE
        has("romantic", "romanticism", "realism", "pre-raphaelite") -> MusicEra.ROMANTIC
        has("impression", "post-impression", "symbolism", "art nouveau") -> MusicEra.IMPRESSIONIST
        has("classical", "neoclassicism", "enlightenment") -> MusicEra.CLASSICAL
        has("modern", "cubism", "surrealism", "expressionism", "abstract") -> MusicEra.MODERN
        else -> eraForYear(artwork)
    }
}

private fun eraForYear(artwork: Artwork): MusicEra {
    val year = artwork.yearStart
        ?: Regex("""(1[0-9]{3}|20[0-2][0-9])""").find(artwork.year)?.groupValues?.getOrNull(1)?.toIntOrNull()
    return when {
        year == null -> MusicEra.CLASSICAL
        year < 1400 -> MusicEra.MEDIEVAL
        year < 1600 -> MusicEra.RENAISSANCE
        year < 1750 -> MusicEra.BAROQUE
        year < 1820 -> MusicEra.CLASSICAL
        year < 1900 -> MusicEra.ROMANTIC
        year < 1940 -> MusicEra.IMPRESSIONIST
        else -> MusicEra.MODERN
    }
}
