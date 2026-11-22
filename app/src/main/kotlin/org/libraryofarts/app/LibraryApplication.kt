package org.libraryofarts.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.libraryofarts.app.notification.DailyArtworkWorker
import org.libraryofarts.app.notification.NotificationScheduler
import org.libraryofarts.core.datastore.SettingsRepository

@HiltAndroidApp
class LibraryApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var settings: SettingsRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationScheduler.ensureChannel(this)
        applicationScope.launch {
            settings.preferences
                .map { Triple(it.notificationsEnabled, it.notificationHour, it.notificationMinute) }
                .distinctUntilChanged()
                .collect { (enabled, hour, minute) ->
                    if (enabled) {
                        NotificationScheduler.schedule(this@LibraryApplication, hour, minute)
                    } else {
                        NotificationScheduler.cancel(this@LibraryApplication)
                    }
                }
        }
    }
}
