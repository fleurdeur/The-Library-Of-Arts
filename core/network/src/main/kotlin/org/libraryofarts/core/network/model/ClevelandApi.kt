package org.libraryofarts.core.network.model

import org.libraryofarts.core.network.source.ClevelandResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ClevelandApi {
    @GET("https://openaccess-api.clevelandart.org/api/artworks/")
    suspend fun artworks(
        @Query("q") query: String?,
        @Query("has_image") hasImage: Int = 1,
        @Query("cc0") cc0: Int = 1,
        @Query("limit") limit: Int = 24,
        @Query("skip") skip: Int = 0,
    ): ClevelandResponse
}
