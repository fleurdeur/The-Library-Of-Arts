package org.libraryofarts.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkFactory {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
        encodeDefaults = true
    }

    fun okHttp(cacheDir: java.io.File): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .cache(okhttp3.Cache(cacheDir, 96L * 1024 * 1024))
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .addInterceptor { chain ->
            val host = chain.request().url.host
            val agent = if (host.endsWith("artic.edu") || host.endsWith("artsandculture.google.com")) {
                // These two hosts challenge non-browser agents at the edge.
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
            } else {
                "TheGreatLibraryOfArts/1.0 (open-access art companion)"
            }
            val request = chain.request().newBuilder()
                .header("User-Agent", agent)
                .header("Accept", "application/json, image/*")
                .build()
            chain.proceed(request)
        }
        .build()

    fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://libraryofarts.invalid/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}

fun String?.orDash(): String = this?.takeIf { it.isNotBlank() } ?: ""

fun String?.imageUrlOrEmpty(): String = this.orEmpty().trim().replace("http://", "https://")
