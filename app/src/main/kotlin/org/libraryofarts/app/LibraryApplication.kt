package org.libraryofarts.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
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
                .maxSizePercent(context, 0.18)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache").absolutePath.toPath())
                .maxSizeBytes(256L * 1024 * 1024)
                .build()
        }
        .crossfade(120)
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
