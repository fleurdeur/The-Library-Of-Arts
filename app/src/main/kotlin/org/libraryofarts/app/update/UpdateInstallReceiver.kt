package org.libraryofarts.app.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.File

/** Removes the downloaded update APK after the new version is installed. */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        File(context.cacheDir, "updates").deleteRecursively()
        context.getSharedPreferences("library_updates", Context.MODE_PRIVATE)
            .edit()
            .remove("pending_version")
            .remove("ready_file")
            .remove("ready_version")
            .apply()
    }
}
