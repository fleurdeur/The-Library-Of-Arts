package org.libraryofarts.core.data.engine

import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.FeedItem
import org.libraryofarts.core.model.FeedReason
import org.libraryofarts.core.model.Interest
import org.libraryofarts.core.model.TasteDimension
import org.libraryofarts.core.model.TasteProfile
import kotlin.math.ln
import kotlin.random.Random

/**
 * A transparent, testable recommender. It scores affinity from the taste
 * profile, always reserves a discovery slice, and explains every choice in
 * plain language. No network, no user identifiers, no hidden weights.
 */
class RecommendationEngine(
    private val discoveryRatioDefault: Float = 0.25f,
) {
    data class Scored(
        val artwork: Artwork,
        val affinity: Float,
        val matched: List<Interest>,
    )

    fun score(artwork: Artwork, profile: TasteProfile, viewedIds: Set<String>): Scored {
        val active = profile.interests.filter { !it.muted && it.weight > 0f }
        val matched = active.filter { interest ->
            when (interest.dimension) {
                TasteDimension.GENRE -> interest.label.equals(artwork.genre, true)
                TasteDimension.ARTIST -> interest.label.equals(artwork.artist?.name, true)
                TasteDimension.PERIOD -> interest.label.equals(artwork.period, true)
                TasteDimension.MOVEMENT -> interest.label.equals(artwork.movement, true)
                TasteDimension.MUSEUM -> interest.label.equals(artwork.museum.id, true) ||
                    interest.label.equals(artwork.museum.shortName, true)
                TasteDimension.MOOD -> artwork.moods.any { it.equals(interest.label, true) }
                TasteDimension.MEDIUM -> artwork.medium?.contains(interest.label, true) == true
            }
        }
        var affinity = 0f
        matched.forEach { affinity += kotlin.math.ln(1.0 + it.weight.toDouble()).toFloat() }
        if (viewedIds.contains(artwork.id)) affinity -= 0.6f
        if (artwork.isRarelyViewed) affinity += 0.15f
        return Scored(artwork, affinity, matched.sortedByDescending { it.weight })
    }

    fun buildFeed(
        candidates: List<Artwork>,
        profile: TasteProfile,
        viewedIds: Set<String>,
        limit: Int = 20,
        discoveryRatio: Float = profile.discoveryRatio.takeIf { it > 0f } ?: discoveryRatioDefault,
        random: Random = Random(profile.lastUpdated xor limit.toLong()),
    ): List<FeedItem> {
        val unique = candidates.distinctBy { it.id }
        if (unique.isEmpty()) return emptyList()
        val scored = unique.map { score(it, profile, viewedIds) }
        val discoveryCount = (limit * discoveryRatio.coerceIn(0.05f, 0.6f)).toInt().coerceAtLeast(1)
        val curatedCount = (limit - discoveryCount).coerceAtLeast(0)

        val curated = scored
            .filter { it.affinity > 0f }
            .sortedWith(
                compareByDescending<Scored> { it.affinity }
                    .thenByDescending { it.artwork.isHighlight }
                    .thenBy { it.artwork.id },
            )
            .take(curatedCount)

        val curatedIds = curated.map { it.artwork.id }.toSet()
        val discoveryPool = scored
            .filter { it.artwork.id !in curatedIds }
            .sortedWith(compareBy<Scored> { it.affinity }.thenByDescending { it.artwork.isRarelyViewed })
        val start = if (discoveryPool.isEmpty()) 0 else random.nextInt(discoveryPool.size)
        val discovery = (discoveryPool.drop(start) + discoveryPool.take(start)).take(discoveryCount)

        val feed = mutableListOf<FeedItem>()
        curated.forEach { item ->
            feed += FeedItem(
                artwork = item.artwork,
                reason = FeedReason.RECOMMENDED,
                explanation = explanationFor(item, discovery = false),
                score = item.affinity,
            )
        }
        discovery.forEach { item ->
            feed += FeedItem(
                artwork = item.artwork,
                reason = FeedReason.DISCOVERY,
                explanation = explanationFor(item, discovery = true),
                score = item.affinity,
            )
        }
        // Interleave discovery so it never looks like an afterthought.
        return feed.shuffled(random).sortedByDescending { it.reason == FeedReason.DISCOVERY && random.nextFloat() < 0.5f }
            .ifEmpty { feed }
    }

    fun becauseYouViewed(reference: Artwork, candidates: List<Artwork>, limit: Int = 12): List<FeedItem> {
        return candidates.asSequence()
            .filter { it.id != reference.id }
            .map { artwork ->
                var score = 0
                if (artwork.artist?.name == reference.artist?.name) score += 5
                if (artwork.period == reference.period) score += 3
                if (artwork.genre == reference.genre) score += 2
                if (artwork.movement == reference.movement) score += 2
                if (artwork.museum.id == reference.museum.id) score += 1
                artScorePair(artwork, score)
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { (artwork, _) ->
                FeedItem(
                    artwork = artwork,
                    reason = FeedReason.BECAUSE_YOU_VIEWED,
                    explanation = "Because you spent time with ${reference.title}",
                    score = 1f,
                )
            }
            .toList()
    }

    private fun artScorePair(artwork: Artwork, score: Int): Pair<Artwork, Int> = artwork to score

    private fun explanationFor(item: Scored, discovery: Boolean): String {
        if (discovery) {
            return "A step outside your usual taste — chosen to keep your eye curious."
        }
        val top = item.matched.firstOrNull()
        return when (top?.dimension) {
            TasteDimension.ARTIST -> "Because you've been drawn to ${top.label}"
            TasteDimension.GENRE -> "You often linger on ${top.label.lowercase()}s"
            TasteDimension.PERIOD -> "You've been exploring ${top.label}"
            TasteDimension.MOOD -> "It shares the ${top.label.lowercase()} mood you return to"
            TasteDimension.MUSEUM -> "A highlight from a museum you enjoy"
            TasteDimension.MEDIUM -> "Its ${top.label.lowercase()} caught your attention before"
            TasteDimension.MOVEMENT -> "You've been drawn to ${top.label}"
            null -> "A work from the wider collection"
        }
    }
}
