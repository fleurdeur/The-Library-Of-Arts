package org.libraryofarts.app.tile

import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.TileService
import org.libraryofarts.app.MainActivity

class LibraryTileService : TileService() {
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse("libraryofarts://explore")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        startActivityAndCollapse(pending)
    }
}
