package org.libraryofarts.core.network

import org.libraryofarts.core.model.ArtSource
import org.libraryofarts.core.model.Artwork

/**
 * Every museum adapter implements the same tiny contract. The rest of the app
 * never sees a museum-specific DTO, and a failing source can never take down
 * the whole Explore screen.
 */
interface MuseumRemoteSource {
    val source: ArtSource
    val isConfigured: Boolean get() = true

    suspend fun highlights(pageSize: Int = 24): List<Artwork>

    suspend fun search(query: String, page: Int = 0, pageSize: Int = 24): List<Artwork>

    suspend fun artwork(sourceId: String): Artwork? = null
}

class SourceHealth {
    private val failures = mutableMapOf<ArtSource, Int>()
    private val lastAttempt = mutableMapOf<ArtSource, Long>()

    @Synchronized
    fun shouldTry(source: ArtSource): Boolean {
        val count = failures[source] ?: 0
        if (count < 3) return true
        val last = lastAttempt[source] ?: 0
        return System.currentTimeMillis() - last > 10 * 60_000
    }

    @Synchronized
    fun record(source: ArtSource, success: Boolean) {
        lastAttempt[source] = System.currentTimeMillis()
        if (success) failures[source] = 0 else failures[source] = (failures[source] ?: 0) + 1
    }
}
