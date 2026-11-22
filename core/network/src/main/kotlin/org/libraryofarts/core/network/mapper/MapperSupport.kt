package org.libraryofarts.core.network.mapper

import org.libraryofarts.core.model.ArtSource
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.ArtworkImage
import org.libraryofarts.core.model.License
import org.libraryofarts.core.model.MuseumCatalog
import org.libraryofarts.core.model.PaletteColor
import org.libraryofarts.core.model.ProvenanceEntry
import org.libraryofarts.core.network.imageUrlOrEmpty

internal object MapperSupport {
    private val paletteSets = listOf(
        listOf(0xFFB5452B, 0xFFE7C67A, 0xFF3A4B5C, 0xFFF2E8D5, 0xFF27211D),
        listOf(0xFF2B4C6F, 0xFF8FA9BF, 0xFFD9C7A7, 0xFF5B4636, 0xFFF4EFE6),
        listOf(0xFF3E6B4F, 0xFFA9C5A0, 0xFFE3C07A, 0xFF6B4F3A, 0xFF1E241D),
        listOf(0xFF9A3B5A, 0xFFE0A3B8, 0xFFE8D8C3, 0xFF4A2E3B, 0xFFF7EDF0),
        listOf(0xFFC59A48, 0xFF7A5A2A, 0xFFE9DFC9, 0xFF2E2A24, 0xFFFBF6EA),
    )

    fun paletteFor(seed: String, accent: Long? = null): List<PaletteColor> {
        val set = paletteSets[(seed.hashCode() and 0x7FFFFFFF) % paletteSets.size]
        val colors = if (accent != null) listOf(accent) + set.drop(1) else set
        return colors.mapIndexed { index, value ->
            PaletteColor(
                argb = value,
                hex = "#%06X".format(value and 0xFFFFFF),
                population = (1f - index * 0.16f).coerceAtLeast(0.1f),
                name = colorName(value),
            )
        }
    }

    fun colorName(argb: Long): String {
        val r = ((argb shr 16) and 0xFF).toInt()
        val g = ((argb shr 8) and 0xFF).toInt()
        val b = (argb and 0xFF).toInt()
        return when {
            r > 180 && g < 110 && b < 110 -> "Vermilion"
            r < 90 && g < 90 && b > 110 -> "Prussian blue"
            r > 170 && g > 140 && b < 100 -> "Ochre"
            r < 100 && g > 100 && b < 110 -> "Sage"
            r > 200 && g > 200 && b > 200 -> "Chalk"
            r < 60 && g < 60 && b < 60 -> "Umber"
            else -> "Mixed pigment"
        }
    }

    fun periodFor(year: Int?): String = when {
        year == null -> "Unknown period"
        year < 500 -> "Ancient"
        year < 1400 -> "Medieval"
        year < 1600 -> "Renaissance"
        year < 1720 -> "Baroque"
        year < 1780 -> "Rococo"
        year < 1840 -> "Neoclassicism / Romanticism"
        year < 1880 -> "Realism"
        year < 1900 -> "Impressionism"
        year < 1910 -> "Post-Impressionism"
        year < 1945 -> "Modernism"
        else -> "Contemporary"
    }

    fun genreFor(raw: String?): String {
        val value = raw.orEmpty().lowercase()
        return when {
            value.contains("paint") -> "Painting"
            value.contains("sculpt") -> "Sculpture"
            value.contains("draw") -> "Drawing"
            value.contains("print") || value.contains("etch") || value.contains("woodcut") -> "Print"
            value.contains("photo") -> "Photograph"
            value.contains("textile") || value.contains("costume") -> "Textile"
            value.contains("ceramic") || value.contains("porcelain") -> "Ceramic"
            value.contains("manuscript") -> "Manuscript"
            value.isBlank() -> "Painting"
            else -> "Decorative arts"
        }
    }

    fun moodsFor(genre: String?, title: String): List<String> {
        val text = "${genre.orEmpty()} $title".lowercase()
        val moods = mutableListOf<String>()
        if (text.contains("portrait") || text.contains("madonna") || text.contains("mother")) moods += "Intimate"
        if (text.contains("storm") || text.contains("battle") || text.contains("dramatic")) moods += "Dramatic"
        if (text.contains("garden") || text.contains("calm") || text.contains("still")) moods += "Serene"
        if (text.contains("night") || text.contains("dream")) moods += "Mysterious"
        if (text.contains("dance") || text.contains("festival")) moods += "Joyful"
        if (moods.isEmpty()) moods += "Reflective"
        return moods.distinct()
    }

    fun licenseFor(publicDomain: Boolean, rights: String?, credit: String): License = when {
        publicDomain -> License.CC0.copy(creditLine = credit)
        rights.isNullOrBlank() -> License.PD.copy(creditLine = credit)
        else -> License(
            name = rights,
            spdxId = rights.take(32),
            isPublicDomain = false,
            requiresAttribution = true,
            creditLine = credit,
        )
    }

    fun provenance(text: String?): List<ProvenanceEntry> = text
        ?.takeIf { it.isNotBlank() }
        ?.split("\n", ";")
        ?.map { it.trim() }
        ?.filter { it.length > 3 }
        ?.take(8)
        ?.map { ProvenanceEntry(description = it) }
        .orEmpty()

    fun image(url: String?, thumb: String?, width: Int, height: Int, iiif: String? = null, credit: String = ""): ArtworkImage {
        val full = url.imageUrlOrEmpty()
        return ArtworkImage(
            thumbnailUrl = thumb.imageUrlOrEmpty().ifBlank { full },
            fullUrl = full,
            width = width.coerceAtLeast(1),
            height = height.coerceAtLeast(1),
            iiifBase = iiif,
            creditLine = credit,
            altText = "Artwork image",
        )
    }

    fun artworkId(source: ArtSource, sourceId: String): String = "${source.id}:$sourceId"

    fun baseArtwork(
        source: ArtSource,
        sourceId: String,
        title: String,
        artistName: String?,
        yearStart: Int?,
        yearEnd: Int?,
        medium: String?,
        genreRaw: String?,
        image: ArtworkImage,
        license: License,
        description: String?,
        credit: String,
        provenance: List<ProvenanceEntry> = emptyList(),
        museumOverride: org.libraryofarts.core.model.Museum? = null,
    ): Artwork {
        val genre = genreFor(genreRaw)
        return Artwork(
            id = artworkId(source, sourceId),
            source = source,
            sourceId = sourceId,
            title = title.ifBlank { "Untitled" },
            artist = artistName?.takeIf { it.isNotBlank() && !it.equals("unknown", true) }
                ?.let { org.libraryofarts.core.model.Artist(name = it.trim()) },
            year = yearStart?.let { if (yearEnd != null && yearEnd != it) "$it–$yearEnd" else "$it" } ?: "Unknown date",
            yearStart = yearStart,
            yearEnd = yearEnd,
            genre = genre,
            period = periodFor(yearStart),
            medium = medium,
            museum = museumOverride ?: MuseumCatalog.bySource(source),
            palette = paletteFor(title),
            image = image,
            description = description,
            license = license,
            provenance = provenance,
            tags = listOfNotNull(genre, periodFor(yearStart)),
            moods = moodsFor(genre, title),
            creditLine = credit,
            isHighlight = false,
        )
    }
}
