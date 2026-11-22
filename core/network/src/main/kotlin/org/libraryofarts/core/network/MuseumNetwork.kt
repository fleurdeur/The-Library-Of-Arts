package org.libraryofarts.core.network

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import org.libraryofarts.core.model.ArtSource
import org.libraryofarts.core.model.Artwork
import javax.inject.Inject
import javax.inject.Singleton

data class MuseumSearchResult(
    val artworks: List<Artwork>,
    val failedSources: List<ArtSource> = emptyList(),
    val configuredSources: Int = 0,
)

/**
 * Fans a query out to every configured museum, tolerates individual failures,
 * and merges the results into one ranked, de-duplicated list. Nothing here
 * blocks offline use: callers fall back to Room when this returns nothing.
 */
@Singleton
class MuseumNetwork @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards MuseumRemoteSource>,
    private val health: SourceHealth,
) {
    val orderedSources: List<MuseumRemoteSource>
        get() = sources.sortedBy { it.source.ordinal }

    val configuredCount: Int
        get() = sources.count { it.isConfigured }

    suspend fun search(
        query: String,
        page: Int = 0,
        pageSize: Int = 24,
        museumIds: Set<String> = emptySet(),
    ): MuseumSearchResult = fanOut(
        pageSize = pageSize,
        filter = { source -> museumIds.isEmpty() || museumIds.contains(source.source.id) },
    ) { source ->
        source.search(query, page, pageSize)
    }

    suspend fun highlights(pageSize: Int = 24): MuseumSearchResult = fanOut(
        pageSize = pageSize,
        filter = { it.isConfigured },
    ) { source ->
        source.highlights(pageSize)
    }

    suspend fun artwork(id: String): Artwork? {
        val prefix = id.substringBefore(':', "")
        val sourceId = id.substringAfter(':', id)
        val source = sources.firstOrNull { it.source.id == prefix } ?: return null
        if (!source.isConfigured) return null
        return withTimeoutOrNull(15_000) {
            runCatching { source.artwork(sourceId) }.getOrNull()
        }
    }

    private suspend fun fanOut(
        pageSize: Int,
        filter: (MuseumRemoteSource) -> Boolean,
        block: suspend (MuseumRemoteSource) -> List<Artwork>,
    ): MuseumSearchResult = coroutineScope {
        val active = sources.filter { it.isConfigured && health.shouldTry(it.source) && filter(it) }
        val outcomes = active.map { source ->
            async {
                val result = withTimeoutOrNull(15_000) {
                    runCatching { block(source) }
                }
                when {
                    result == null -> source.source to null
                    result.isSuccess -> {
                        health.record(source.source, true)
                        source.source to result.getOrElse { emptyList() }
                    }
                    else -> {
                        health.record(source.source, false)
                        source.source to null
                    }
                }
            }
        }.awaitAll()

        val failed = outcomes.filter { it.second == null }.map { it.first }
        val merged = outcomes.flatMap { it.second.orEmpty() }
            .filter { it.license.isPublicDomain || it.license.requiresAttribution }
            .distinctBy { it.id }
            .sortedByDescending { artworkScore(it) }
            .take(pageSize * 2)

        MuseumSearchResult(
            artworks = merged,
            failedSources = failed,
            configuredSources = active.size,
        )
    }

    private fun artworkScore(artwork: Artwork): Int {
        var score = 0
        if (artwork.isHighlight) score += 3
        if (artwork.image.width >= 2_000) score += 2
        if (artwork.description != null) score += 1
        if (artwork.palette.isNotEmpty()) score += 1
        if (artwork.license.isPublicDomain) score += 1
        return score
    }
}
