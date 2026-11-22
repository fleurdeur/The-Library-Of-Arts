package org.libraryofarts.core.network.model

import org.libraryofarts.core.network.source.RijksResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface RijksApi {
    @GET("https://www.rijksmuseum.nl/api/en/collection")
    suspend fun artworks(
        @Query("key") key: String,
        @Query("q") query: String,
        @Query("imgonly") imagesOnly: Boolean = true,
        @Query("ps") pageSize: Int = 24,
        @Query("p") page: Int = 1,
        @Query("s") sort: String = "relevance",
    ): RijksResponse
}
