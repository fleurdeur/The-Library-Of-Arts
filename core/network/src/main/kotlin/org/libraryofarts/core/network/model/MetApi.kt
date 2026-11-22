package org.libraryofarts.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MetApi {
    @GET("https://collectionapi.metmuseum.org/public/collection/v1/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("hasImages") hasImages: Boolean = true,
        @Query("isHighlight") isHighlight: Boolean? = null,
    ): org.libraryofarts.core.network.source.MetSearchResponse

    @GET("https://collectionapi.metmuseum.org/public/collection/v1/objects/{id}")
    suspend fun getObject(@Path("id") id: Int): MetObject
}

@Serializable
data class MetObject(
    @SerialName("objectID") val objectID: Int,
    val isHighlight: Boolean = false,
    val accessionNumber: String? = null,
    val accessionYear: String? = null,
    @SerialName("isPublicDomain") val isPublicDomain: Boolean = false,
    @SerialName("primaryImage") val primaryImage: String? = null,
    @SerialName("primaryImageSmall") val primaryImageSmall: String? = null,
    val title: String? = null,
    val culture: String = "",
    val period: String = "",
    val dynasty: String = "",
    val objectDate: String? = null,
    @SerialName("objectBeginDate") val objectBeginDate: Int? = null,
    @SerialName("objectEndDate") val objectEndDate: Int? = null,
    val medium: String? = null,
    val dimensions: String? = null,
    val creditLine: String? = null,
    val classification: String? = null,
    val objectName: String? = null,
    val department: String? = null,
    @SerialName("artistDisplayName") val artistDisplayName: String? = null,
    @SerialName("artistDisplayBio") val artistDisplayBio: String? = null,
    @SerialName("artistBeginDate") val artistBeginDate: String? = null,
    @SerialName("artistEndDate") val artistEndDate: String? = null,
    val galleryNumber: String? = null,
    val movement: String? = null,
    val description: String? = null,
    @SerialName("objectURL") val objectURL: String? = null,
    val repository: String? = null,
)
