package org.libraryofarts.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ArtworkFilter(
    val query: String = "",
    val genres: Set<String> = emptySet(),
    val periods: Set<String> = emptySet(),
    val artists: Set<String> = emptySet(),
    val museumIds: Set<String> = emptySet(),
    val mediums: Set<String> = emptySet(),
    val colors: Set<String> = emptySet(),
    val moods: Set<String> = emptySet(),
    val subjects: Set<String> = emptySet(),
    val centuryFrom: Int? = null,
    val centuryTo: Int? = null,
    val publicDomainOnly: Boolean = true,
    val sort: SortOrder = SortOrder.RELEVANCE,
) {
    val isActive: Boolean
        get() = query.isNotBlank() || genres.isNotEmpty() || periods.isNotEmpty() ||
            artists.isNotEmpty() || museumIds.isNotEmpty() || mediums.isNotEmpty() ||
            colors.isNotEmpty() || moods.isNotEmpty() || subjects.isNotEmpty() ||
            centuryFrom != null || centuryTo != null

    val activeFilterCount: Int
        get() = listOf(
            genres, periods, artists, museumIds, mediums, colors, moods, subjects,
        ).count { it.isNotEmpty() } + listOfNotNull(centuryFrom, centuryTo).size

    fun toggleGenre(value: String) = copy(genres = genres.toggle(value))
    fun togglePeriod(value: String) = copy(periods = periods.toggle(value))
    fun toggleMuseum(value: String) = copy(museumIds = museumIds.toggle(value))
    fun toggleArtist(value: String) = copy(artists = artists.toggle(value))
    fun toggleMedium(value: String) = copy(mediums = mediums.toggle(value))
    fun toggleColor(value: String) = copy(colors = colors.toggle(value))
    fun toggleMood(value: String) = copy(moods = moods.toggle(value))
    fun toggleSubject(value: String) = copy(subjects = subjects.toggle(value))

    private fun Set<String>.toggle(value: String): Set<String> =
        if (contains(value)) this - value else this + value
}

@Serializable
enum class SortOrder(val label: String) {
    RELEVANCE("Most relevant"),
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    MOST_VIEWED("Most viewed"),
    TITLE("Title A–Z"),
}

@Serializable
data class ColorSwatch(val name: String, val argb: Long)

object FilterOptions {
    val colors = listOf("Red", "Blue", "Green", "Gold", "Black", "White", "Pink", "Earth")
    val colorSwatches = listOf(
        ColorSwatch("Red", 0xFFB5452B),
        ColorSwatch("Blue", 0xFF2B4C6F),
        ColorSwatch("Green", 0xFF3E6B4F),
        ColorSwatch("Gold", 0xFFC59A48),
        ColorSwatch("Black", 0xFF1A1714),
        ColorSwatch("White", 0xFFF4EFE6),
        ColorSwatch("Pink", 0xFFC98A9E),
        ColorSwatch("Earth", 0xFF8A6A4A),
    )
    val moods = listOf("Serene", "Dramatic", "Joyful", "Melancholy", "Mysterious", "Intimate", "Majestic", "Playful")
    val subjects = listOf(
        "Portrait", "Landscape", "Still life", "City", "Sea", "Religion",
        "Mythology", "Animals", "Flowers", "Interior", "War", "Dance",
    )
    val genres = listOf(
        "Painting", "Sculpture", "Drawing", "Print", "Photograph",
        "Textile", "Ceramic", "Manuscript", "Decorative arts",
    )
    val mediums = listOf(
        "Oil on canvas", "Oil on panel", "Tempera on panel", "Watercolor",
        "Ink on paper", "Charcoal", "Marble", "Bronze", "Etching",
        "Woodblock print", "Pastel", "Photograph",
    )
    val periods = listOf(
        "Ancient", "Medieval", "Renaissance", "Baroque", "Rococo",
        "Neoclassicism", "Romanticism", "Realism", "Impressionism",
        "Post-Impressionism", "Modernism", "Contemporary",
    )
    val centuries = (12..21).toList()
}
