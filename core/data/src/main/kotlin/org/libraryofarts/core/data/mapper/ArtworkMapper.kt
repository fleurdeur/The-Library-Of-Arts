package org.libraryofarts.core.data.mapper

import kotlinx.serialization.json.Json
import org.libraryofarts.core.database.ArtworkEntity
import org.libraryofarts.core.model.Artwork

internal val ArtworkJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

fun Artwork.toEntity(now: Long = System.currentTimeMillis(), json: Json = ArtworkJson): ArtworkEntity {
    val families = palette.map { colorFamily(it.argb) }.distinct()
    return ArtworkEntity(
        id = id,
        source = source.id,
        sourceId = sourceId,
        title = title,
        description = description.orEmpty(),
        artistName = artist?.name.orEmpty(),
        artistId = artist?.id,
        year = year,
        yearStart = yearStart,
        yearEnd = yearEnd,
        genre = genre,
        period = period,
        movement = movement,
        medium = medium,
        museumId = museum.id,
        museumName = museum.name,
        moodsCsv = moods.joinToString("|"),
        subjectsCsv = subjects.joinToString("|"),
        tagsCsv = tags.joinToString("|"),
        paletteHexCsv = palette.joinToString("|") { it.hex },
        colorFamiliesCsv = families.joinToString("|"),
        isHighlight = isHighlight,
        isRarelyViewed = isRarelyViewed,
        viewCount = viewCount,
        packId = packId,
        json = json.encodeToString(Artwork.serializer(), this),
        updatedAt = now,
    )
}

fun ArtworkEntity.toArtwork(codec: Json = ArtworkJson): Artwork? = runCatching {
    codec.decodeFromString(Artwork.serializer(), this.json)
}.getOrNull()

fun colorFamily(argb: Long): String {
    val r = ((argb shr 16) and 0xFF).toInt()
    val g = ((argb shr 8) and 0xFF).toInt()
    val b = (argb and 0xFF).toInt()
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    return when {
        max < 55 -> "Black"
        min > 215 -> "White"
        delta < 24 -> if (max > 140) "Grey" else "Black"
        r >= g && r >= b && g < 120 -> "Red"
        r > 150 && g in 80..190 && b < 100 -> if (g > 140) "Gold" else "Red"
        r > 150 && b > 120 && g < 130 -> "Pink"
        g >= r && g >= b -> "Green"
        b >= r && b > g -> "Blue"
        r > 130 && g > 100 && b < 90 -> "Earth"
        else -> "Earth"
    }
}
