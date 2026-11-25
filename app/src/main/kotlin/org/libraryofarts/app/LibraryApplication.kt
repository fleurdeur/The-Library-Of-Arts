package org.libraryofarts.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowHardware
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import okio.Path.Companion.toPath
import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.libraryofarts.app.notification.DailyArtworkWorker
import org.libraryofarts.app.notification.NotificationScheduler
import org.libraryofarts.app.update.UpdateScheduler
import org.libraryofarts.core.data.update.UpdateRepository
import org.libraryofarts.core.datastore.SettingsRepository

@HiltAndroidApp
class LibraryApplication : Application(), Configuration.Provider, SingletonImageLoader.Factory {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var updates: UpdateRepository

    private val exceptionHandler = CoroutineExceptionHandler { _, error ->
        Log.e("LibraryApplication", "Background startup task failed without taking the app down.", error)
    }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache").absolutePath.toPath())
                .maxSizeBytes(512L * 1024 * 1024)
                .build()
        }
        .components {
            add(
                OkHttpNetworkFetcherFactory(
                    callFactory = { coilOkHttp() },
                ),
            )
        }
        .allowHardware(true)
        .crossfade(90)
        .build()

    /** A dedicated client whose cache directory matches Coil's disk cache. */
    private fun coilOkHttp(): okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder()
        .cache(okhttp3.Cache(cacheDir.resolve("image_cache_okhttp"), 256L * 1024 * 1024))
        .connectTimeout(java.util.concurrent.TimeUnit.SECONDS.toMillis(10), java.util.concurrent.TimeUnit.MILLISECONDS)
        .readTimeout(java.util.concurrent.TimeUnit.SECONDS.toMillis(20), java.util.concurrent.TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val host = chain.request().url.host
            val agent = if (host.endsWith("artic.edu")) {
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
            } else {
                "TheGreatLibraryOfArts/1.0 (open-access art companion)"
            }
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", agent)
                    .build(),
            )
        }
        .build()

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationScheduler.ensureChannel(this)
        UpdateScheduler.ensureChannel(this)
        runCatching { UpdateScheduler.schedule(this) }
        applicationScope.launch { runCatching { updates.cleanUpIfInstalled() } }
        applicationScope.launch {
            settings.preferences
                .map { Triple(it.notificationsEnabled, it.notificationHour, it.notificationMinute) }
                .distinctUntilChanged()
                .collect { (enabled, hour, minute) ->
                    runCatching {
                        if (enabled) {
                            NotificationScheduler.schedule(this@LibraryApplication, hour, minute)
                        } else {
                            NotificationScheduler.cancel(this@LibraryApplication)
                        }
                    }.onFailure { error ->
                        Log.w("LibraryApplication", "Daily notification could not be scheduled.", error)
                    }
                }
        }
    }
}
