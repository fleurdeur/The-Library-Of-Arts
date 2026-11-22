package org.libraryofarts.core.network.model

import org.libraryofarts.core.network.source.NgaResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NgaApi {
    @GET("https://api.nga.gov/artworks")
    suspend fun artworks(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") pageSize: Int = 24,
        @Query("api_key") apiKey: String? = null,
    ): NgaResponse
}
