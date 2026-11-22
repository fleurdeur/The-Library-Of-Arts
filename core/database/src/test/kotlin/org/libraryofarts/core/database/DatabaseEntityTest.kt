package org.libraryofarts.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseEntityTest {
    @Test
    fun `artwork entity preserves identity and searchable fields`() {
        val entity = ArtworkEntity(
            id = "seed-1",
            source = "wikimedia",
            sourceId = "seed-1",
            title = "A work",
            description = "A quiet description",
            artistName = "An artist",
            artistId = null,
            year = "1888",
            yearStart = 1888,
            yearEnd = 1888,
            genre = "Painting",
            period = "Post-Impressionism",
            movement = "Post-Impressionism",
            medium = "Oil on canvas",
            museumId = "van-gogh-museum",
            museumName = "Van Gogh Museum",
            moodsCsv = "Calm|Wonder",
            subjectsCsv = "Night",
            tagsCsv = "painting",
            paletteHexCsv = "#1A1714|#C59A48",
            colorFamiliesCsv = "Blue|Gold",
            isHighlight = true,
            isRarelyViewed = false,
            viewCount = 3,
            packId = null,
            json = "{}",
            updatedAt = 0,
        )
        assertEquals("seed-1", entity.id)
        assertTrue(entity.moodsCsv.contains("Calm"))
        assertEquals(entity, entity.copy())
    }

    @Test
    fun `journal entity keeps mood and tags separate`() {
        val entry = JournalEntryEntity(
            id = "j1",
            artworkId = "a1",
            artworkTitle = "A work",
            artworkImageUrl = "https://example.org/a.jpg",
            text = "I noticed the light.",
            mood = "REFLECTIVE",
            tagsCsv = "light|quiet",
            createdAt = 1,
            updatedAt = 2,
            audioNotePath = null,
        )
        assertEquals("REFLECTIVE", entry.mood)
        assertEquals(2, entry.tagsCsv.split('|').size)
    }
}
