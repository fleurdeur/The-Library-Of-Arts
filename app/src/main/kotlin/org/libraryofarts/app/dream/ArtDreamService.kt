package org.libraryofarts.app.dream

import android.graphics.Color
import android.service.dreams.DreamService
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalDate
import org.libraryofarts.core.data.seed.SeedArtworks

/** A screensaver/daydream for docks and charging stands. */
class ArtDreamService : DreamService() {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        val artwork = SeedArtworks.all[LocalDate.now().dayOfYear % SeedArtworks.all.size]
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(artwork.dominantColor.toInt())
            setPadding(72, 72, 72, 72)
        }
        layout.addView(TextView(this).apply {
            text = artwork.title
            setTextColor(Color.WHITE)
            textSize = 34f
            gravity = Gravity.CENTER
        })
        layout.addView(TextView(this).apply {
            text = "${artwork.artist?.name ?: "Unknown artist"} · ${artwork.year.ifBlank { "date unknown" }}"
            setTextColor(Color.argb(190, 255, 255, 255))
            textSize = 20f
            gravity = Gravity.CENTER
        })
        layout.addView(TextView(this).apply {
            text = "THE GREAT LIBRARY OF ARTS"
            setTextColor(Color.argb(150, 255, 255, 255))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 48, 0, 0)
        })
        setContentView(layout)
    }
}
