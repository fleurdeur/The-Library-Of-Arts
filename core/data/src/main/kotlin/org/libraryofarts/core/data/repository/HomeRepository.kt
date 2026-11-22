package org.libraryofarts.core.data.repository

import org.libraryofarts.core.data.engine.RecommendationEngine
import org.libraryofarts.core.data.seed.SeedLearning
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.FeedItem
import org.libraryofarts.core.model.FeedReason
import org.libraryofarts.core.model.StreakState
import org.libraryofarts.core.model.TasteProfile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class HomeSection(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val items: List<FeedItem>,
    val showTasteNote: Boolean = false,
)

data class HomeContent(
    val daily: Artwork,
    val streak: StreakState,
    val sections: List<HomeSection>,
    val continueExploring: List<Artwork>,
    val tasteProfile: TasteProfile,
    val onlineSources: Int = 0,
)

@Singleton
class HomeRepository @Inject constructor(
    private val artworks: ArtworkRepository,
    private val taste: TasteRepository,
    private val streak: StreakRepository,
    private val engine: RecommendationEngine,
) {
    suspend fun load(profile: TasteProfile): HomeContent {
        artworks.seedIfNeeded()
        val now = System.currentTimeMillis()
        val today = java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val epochDay = today.toEpochDay()
        val candidates = artworks.allCandidates(180)
        val daily = artworks.dailyArtwork(epochDay)
        val streakState = streak.recordViewing(daily.id)
        val viewedIds = artworks.hiddenIds()
        val hidden = artworks.hiddenIds()
        val continueList = artworks.continueExploring(8).filterNot { it.id in hidden }

        val personalized = engine.buildFeed(
            candidates = candidates.filterNot { it.id in hidden },
            profile = profile,
            viewedIds = viewedIds,
            limit = 20,
        )

        val becauseViewed = continueList.firstOrNull()?.let { reference ->
            engine.becauseYouViewed(reference, candidates, 10)
        }.orEmpty()

        val event = SeedLearning.onThisDay.firstOrNull { it.month == today.monthValue && it.day == today.dayOfMonth }
        val onThisDay = event?.let { found ->
            SeedLearning.onThisDayEvent(found, artworks)
        } ?: listOf(
            FeedItem(
                artwork = daily,
                reason = FeedReason.ON_THIS_DAY,
                explanation = "A work for today, ${today.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${today.dayOfMonth}",
                score = 1f,
            ),
        )

        val trendingPool = artworks.trending(12).ifEmpty { SeedArtworksHighlights.all() }
        val trending = trendingPool.take(10).map {
            FeedItem(
                artwork = it,
                reason = FeedReason.TRENDING,
                explanation = "Trending across the museums this week",
                score = 1f,
            )
        }

        val genreRows = candidates.filter { !it.genre.isNullOrBlank() }
            .groupBy { it.genre.orEmpty() }
            .entries
            .sortedByDescending { it.value.size }
            .take(4)
            .map { (genre, list) ->
                HomeSection(
                    id = "genre-$genre",
                    title = genre,
                    subtitle = "A shelf of ${genre.lowercase()}s",
                    items = list.distinctBy { it.id }.take(10).map { artwork ->
                        FeedItem(
                            artwork = artwork,
                            reason = FeedReason.RECOMMENDED,
                            explanation = "From your $genre shelf",
                            score = 1f,
                        )
                    },
                )
            }

        val hiddenGems = artworks.hiddenGems(8).map {
            FeedItem(it, FeedReason.HIDDEN_GEM, "A quieter masterpiece, waiting for a slow look", 1f)
        }

        return HomeContent(
            daily = daily,
            streak = streakState,
            sections = buildList {
                add(
                    HomeSection(
                        id = "personal",
                        title = "For your eye today",
                        subtitle = "Learned on this device, with room to surprise you",
                        items = personalized.take(12),
                        showTasteNote = true,
                    ),
                )
                if (becauseViewed.isNotEmpty()) {
                    add(HomeSection("because", "Because you viewed…", null, becauseViewed.take(10)))
                }
                add(HomeSection("on-this-day", "On this day in art history", event?.text, onThisDay))
                add(HomeSection("trending", "Trending in museums this week", "What other visitors are lingering on", trending))
                addAll(genreRows)
                if (hiddenGems.isNotEmpty()) {
                    add(HomeSection("gems", "Hidden gems", "Rarely viewed, quietly extraordinary", hiddenGems))
                }
            },
            continueExploring = continueList,
            tasteProfile = profile,
        )
    }

    private object SeedArtworksHighlights {
        fun all(): List<Artwork> = org.libraryofarts.core.data.seed.SeedArtworks.all.filter { it.isHighlight }
    }

    private fun SeedLearning.onThisDayEvent(
        event: org.libraryofarts.core.data.seed.OnThisDayEvent,
        artworks: ArtworkRepository,
    ): List<FeedItem> {
        val artwork = org.libraryofarts.core.data.seed.SeedArtworks.byId[event.artworkId]
        return if (artwork == null) {
            emptyList()
        } else {
            listOf(
                FeedItem(
                    artwork = artwork,
                    reason = FeedReason.ON_THIS_DAY,
                    explanation = event.text,
                    score = 1f,
                ),
            )
        }
    }
}
