package org.libraryofarts.core.model

import kotlinx.serialization.Serializable

/**
 * The single normalized artwork model every museum source is mapped into.
 * All fields are immutable and serializable so a work can travel through
 * Room, a packed offline archive, a PDF export or a shared collection file.
 */
@Serializable
data class Artwork(
    val id: String,
    val source: ArtSource,
    val sourceId: String,
    val title: String,
    val artist: Artist? = null,
    val year: String = "",
    val yearStart: Int? = null,
    val yearEnd: Int? = null,
    val genre: String? = null,
    val period: String? = null,
    val movement: String? = null,
    val medium: String? = null,
    val dimensions: String? = null,
    val museum: Museum,
    val location: String? = null,
    val palette: List<PaletteColor> = emptyList(),
    val image: ArtworkImage,
    val description: String? = null,
    val license: License,
    val provenance: List<ProvenanceEntry> = emptyList(),
    val audio: List<AudioTrack> = emptyList(),
    val relatedIds: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val moods: List<String> = emptyList(),
    val subjects: List<String> = emptyList(),
    val annotations: List<ArtworkAnnotation> = emptyList(),
    val layers: List<ArtworkLayer> = emptyList(),
    val isHighlight: Boolean = false,
    val isRarelyViewed: Boolean = false,
    val viewCount: Long = 0,
    val accessionNumber: String? = null,
    val creditLine: String = "",
    val packId: String? = null,
) {
    val displayArtist: String get() = artist?.name ?: "Unknown artist"
    val displayPeriod: String? get() = period
        ?.takeUnless { it.isBlank() || it.equals("Unknown period", true) || it.equals("Unknown", true) }
        ?: movement?.takeIf { it.isNotBlank() }
    val dominantColor: Long get() = palette.maxByOrNull { it.population }?.argb ?: 0xFF2A2622
    val hasHighResolution: Boolean get() = image.width >= 4_000 || image.iiifBase != null
}

@Serializable
data class Artist(
    val id: String = "",
    val name: String,
    val birthYear: Int? = null,
    val deathYear: Int? = null,
    val nationality: String? = null,
    val bio: String? = null,
    val imageUrl: String? = null,
) {
    val lifespan: String
        get() = when {
            birthYear != null && deathYear != null -> "$birthYear–$deathYear"
            birthYear != null -> "b. $birthYear"
            deathYear != null -> "d. $deathYear"
            else -> ""
        }
}

@Serializable
enum class ArtSource(val id: String, val label: String) {
    MET("met", "The Metropolitan Museum of Art"),
    ART_INSTITUTE("aic", "Art Institute of Chicago"),
    RIJKSMUSEUM("rijksmuseum", "Rijksmuseum"),
    CLEVELAND("cleveland", "Cleveland Museum of Art"),
    SMITHSONIAN("smithsonian", "Smithsonian Institution"),
    NATIONAL_GALLERY("nga", "National Gallery of Art"),
    EUROPEANA("europeana", "Europeana"),
    HARVARD("harvard", "Harvard Art Museums"),
    PARIS_MUSEES("paris-musees", "Paris Musées"),
    GETTY("getty", "The J. Paul Getty Museum"),
    WIKIMEDIA("wikimedia", "Wikimedia Commons"),
    GOOGLE_ARTS("google-arts", "Google Arts & Culture"),
    OFFLINE_PACK("pack", "Offline Art Pack");

    companion object {
        fun fromId(id: String): ArtSource = entries.firstOrNull { it.id == id } ?: OFFLINE_PACK
    }
}

@Serializable
data class ProvenanceEntry(
    val date: String = "",
    val description: String,
    val sourceUrl: String? = null,
)

@Serializable
data class License(
    val name: String,
    val spdxId: String? = null,
    val url: String? = null,
    val isPublicDomain: Boolean = true,
    val requiresAttribution: Boolean = false,
    val creditLine: String = "",
) {
    val shortLabel: String
        get() = when {
            isPublicDomain && spdxId == "CC0-1.0" -> "Public domain (CC0)"
            isPublicDomain -> "Public domain"
            spdxId != null -> spdxId
            else -> name
        }

    companion object {
        val CC0 = License(
            name = "Creative Commons Zero (CC0 1.0)",
            spdxId = "CC0-1.0",
            url = "https://creativecommons.org/publicdomain/zero/1.0/",
            isPublicDomain = true,
        )
        val PD = License(
            name = "Public domain",
            spdxId = "PD",
            url = "https://creativecommons.org/publicdomain/mark/1.0/",
            isPublicDomain = true,
        )
    }
}
