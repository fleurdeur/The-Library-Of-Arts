package org.libraryofarts.core.network.model

import kotlinx.serialization.json.JsonObject
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface WikimediaApi {
    @GET("https://commons.wikimedia.org/w/api.php")
    suspend fun search(
        @Query("action") action: String = "query",
        @Query("generator") generator: String = "search",
        @Query("gsrsearch") search: String,
        @Query("gsrnamespace") namespace: Int = 6,
        @Query("gsrlimit") limit: Int = 24,
        @Query("gsroffset") offset: Int = 0,
        @Query("prop") prop: String = "imageinfo",
        @Query("iiprop") imageInfo: String = "url|size|extmetadata",
        @Query("iiurlwidth") thumbWidth: Int = 1000,
        @Query("format") format: String = "json",
        @Query("origin") origin: String = "*",
    ): JsonObject
}

interface EuropeanaApi {
    @GET("https://api.europeana.eu/record/v2/search.json")
    suspend fun search(
        @Query("wskey") key: String,
        @Query("query") query: String,
        @Query("rows") rows: Int = 24,
        @Query("start") start: Int = 1,
        @Query("qf") filter: String = "TYPE:IMAGE",
        @Query("reusability") reusability: String = "open",
        @Header("Accept") accept: String = "application/json",
    ): JsonObject
}

interface SmithsonianApi {
    @GET("https://api.si.edu/openaccess/api/v1.0/search")
    suspend fun search(
        @Query("api_key") key: String,
        @Query("q") query: String,
        @Query("rows") rows: Int = 24,
        @Query("start") start: Int = 0,
    ): JsonObject
}

interface HarvardApi {
    @GET("https://api.harvardartmuseums.org/object")
    suspend fun objects(
        @Query("apikey") key: String,
        @Query("q") query: String = "hasimage:1",
        @Query("size") size: Int = 24,
        @Query("page") page: Int = 1,
        @Query("hasimage") hasImage: Int = 1,
        @Query("sort") sort: String = "rank",
    ): JsonObject
}

interface ParisMuseesApi {
    @retrofit2.http.POST("https://apicollections.parismusees.paris.fr/graphql")
    suspend fun graphql(
        @Header("X-App-Token") token: String,
        @retrofit2.http.Body body: JsonObject,
    ): JsonObject
}

interface GettyApi {
    @GET("https://api.getty.edu/art/collection/v1/objects")
    suspend fun objects(
        @Query("api_key") key: String,
        @Query("q") query: String? = null,
        @Query("limit") limit: Int = 24,
        @Query("offset") offset: Int = 0,
    ): JsonObject
}
