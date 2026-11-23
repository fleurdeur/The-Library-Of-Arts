package org.libraryofarts.app.update

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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import org.libraryofarts.app.MainActivity
import org.libraryofarts.app.R
import org.libraryofarts.core.common.BuildInfo
import org.libraryofarts.core.data.update.UpdateRepository

object UpdateScheduler {
    private const val CHANNEL_ID = "library_updates"
    private const val PERIODIC_WORK = "library-update-check"
    private const val STARTUP_WORK = "library-update-check-startup"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_updates),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_updates_description)
                setShowBadge(true)
            },
        )
    }

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodic = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag(PERIODIC_WORK)
            .build()
        val manager = WorkManager.getInstance(context)
        manager.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, periodic)
        val startup = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
            .setConstraints(constraints)
            .addTag(STARTUP_WORK)
            .build()
        manager.enqueueUniqueWork(STARTUP_WORK, ExistingWorkPolicy.KEEP, startup)
    }

    fun checkNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .addTag(STARTUP_WORK)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(STARTUP_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    const val channelId = CHANNEL_ID
}

@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val updates: UpdateRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return Result.success()
        }
        val check = updates.check(BuildInfo.VERSION_NAME)
        val update = check.latest ?: return Result.success()
        if (!check.available || !updates.shouldNotify(update)) return Result.success()

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse("libraryofarts://settings")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            200,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, UpdateScheduler.channelId)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(context.getString(R.string.update_notification_title, update.versionName))
            .setContentText(context.getString(R.string.update_notification_body))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.update_notification_body)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SYSTEM)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(43, notification)
        updates.markNotified(update)
        return Result.success()
    }
}
