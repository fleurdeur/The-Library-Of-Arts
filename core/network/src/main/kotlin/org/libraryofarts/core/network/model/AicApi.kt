package org.libraryofarts.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.libraryofarts.core.network.source.AicArtwork
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AicApi {
    @GET("https://api.artic.edu/api/v1/artworks")
    suspend fun artworks(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 24,
        @Query("fields") fields: String,
        @Query("q") query: String? = null,
    ): AicResponse

    @GET("https://api.artic.edu/api/v1/artworks/{id}")
    suspend fun artwork(
        @Path("id") id: String,
        @Query("fields") fields: String = "id,title,image_id,is_public_domain,artist_display,date_start,date_end,medium_display,dimensions,classification_title,credit_line,color,provenance_text,description,style_title",
    ): AicSingleResponse
}

@Serializable
data class AicResponse(
    val data: List<AicArtwork> = emptyList(),
    @SerialName("config") val config: AicConfig? = null,
)

@Serializable
data class AicConfig(
    @SerialName("iiif_url") val iiifUrl: String? = null,
    val website_url: String? = null,
)

@Serializable
data class AicSingleResponse(val data: AicArtwork? = null)
