package org.libraryofarts.core.data.engine

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.libraryofarts.core.data.seed.SeedArtworks
import org.libraryofarts.core.model.FeedReason
import org.libraryofarts.core.model.Interest
import org.libraryofarts.core.model.TasteDimension
import org.libraryofarts.core.model.TasteProfile

class RecommendationEngineTest {
    private val engine = RecommendationEngine()

    @Test
    fun `discovery reserve is always present`() {
        val profile = TasteProfile(
            interests = listOf(
                Interest("genre:portrait", "Portrait", TasteDimension.GENRE, 3.2f, 0L, 10),
            ),
            discoveryRatio = 0.25f,
        )
        val feed = engine.buildFeed(
            candidates = SeedArtworks.all,
            profile = profile,
            viewedIds = emptySet(),
            limit = 12,
            random = Random(7),
        )
        assertTrue(feed.any { it.reason == FeedReason.DISCOVERY })
        assertTrue(feed.size <= 12)
    }

    @Test
    fun `viewed works receive a penalty and matching works score higher`() {
        val example = SeedArtworks.all.first { !it.genre.isNullOrBlank() }
        val profile = TasteProfile(
            interests = listOf(
                Interest("genre:${example.genre}", example.genre!!, TasteDimension.GENRE, 2.0f, 0L, 5),
            ),
        )
        val fresh = engine.score(example, profile, emptySet())
        val seen = engine.score(example, profile, setOf(example.id))
        assertTrue(fresh.affinity > seen.affinity)
        assertTrue(fresh.matched.isNotEmpty())
    }

    @Test
    fun `because you viewed excludes the reference work`() {
        val reference = SeedArtworks.all.first()
        val related = engine.becauseYouViewed(reference, SeedArtworks.all)
        assertTrue(related.none { it.artwork.id == reference.id })
    }

    @Test
    fun `seed library is complete and attributable`() {
        assertTrue(SeedArtworks.all.size >= 20)
        SeedArtworks.all.forEach { artwork ->
            assertTrue("missing title", artwork.title.isNotBlank())
            assertTrue("missing credit", artwork.creditLine.isNotBlank())
            assertTrue("missing license", artwork.license.name.isNotBlank())
            assertTrue("missing image", artwork.image.fullUrl.isNotBlank())
            assertTrue("missing artist", artwork.artist?.name?.isNotBlank() == true)
        }
        assertEquals(SeedArtworks.all.size, SeedArtworks.all.distinctBy { it.id }.size)
    }
}
