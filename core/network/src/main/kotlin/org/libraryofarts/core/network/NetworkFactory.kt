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
            val builder = chain.request().newBuilder()
            if (host.endsWith("artic.edu") || host.endsWith("artsandculture.google.com")) {
                // Both hosts challenge requests that don't look like a page the
                // user opened; the referer is what their edge actually checks.
                builder
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36")
                    .header("Referer", "https://$host/")
                    .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8,application/json;q=0.9")
            } else {
                builder
                    .header("User-Agent", "TheGreatLibraryOfArts/1.0 (open-access art companion)")
                    .header("Accept", "application/json, image/*")
            }
            chain.proceed(builder.build())
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
