package org.libraryofarts.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import org.libraryofarts.app.MainActivity
import org.libraryofarts.app.R
import org.libraryofarts.core.data.seed.SeedArtworks

object NotificationScheduler {
    private const val CHANNEL_ID = "daily_artwork"
    private const val WORK_NAME = "daily-artwork"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_daily),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_daily_description)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun schedule(context: Context, hour: Int, minute: Int) {
        val now = java.time.ZonedDateTime.now()
        var next = now.withHour(hour.coerceIn(0, 23)).withMinute(minute.coerceIn(0, 59)).withSecond(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = java.time.Duration.between(now, next).toMillis()
        val request = PeriodicWorkRequestBuilder<DailyArtworkWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(WORK_NAME)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    const val channelId = CHANNEL_ID
}

@HiltWorker
class DailyArtworkWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return Result.success()
        }
        val day = LocalDate.now().dayOfYear
        val artwork = SeedArtworks.all[day % SeedArtworks.all.size]
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse("libraryofarts://artwork/${artwork.id}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, NotificationScheduler.channelId)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(artwork.title)
            .setContentText("${artwork.artist?.name ?: "Unknown artist"} · ${context.getString(R.string.notification_title)}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(artwork.description?.takeIf { it.isNotBlank() } ?: "A quiet moment with a public-domain work."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(42, notification)
        return Result.success()
    }
}
