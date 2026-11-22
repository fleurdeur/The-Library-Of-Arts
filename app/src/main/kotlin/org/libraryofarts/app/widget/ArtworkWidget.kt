package org.libraryofarts.app.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.LocalDate
import org.libraryofarts.core.data.seed.SeedArtworks

class ArtworkWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val artwork = SeedArtworks.all[LocalDate.now().dayOfYear % SeedArtworks.all.size]
        provideContent {
            Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1714))
                        .padding(16.dp),
                ) {
                    Text(
                        text = "THE GREAT LIBRARY OF ARTS",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFC59A48)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Text(
                        text = artwork.title,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFF4EFE6)),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 2,
                    )
                    Text(
                        text = "${artwork.artist?.name ?: "Unknown artist"} · ${artwork.year.ifBlank { "date unknown" }}",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFB9AFA2)),
                            fontSize = 12.sp,
                        ),
                        maxLines = 1,
                    )
                    Text(
                        text = "Open the library",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFE07A5F)),
                            fontSize = 12.sp,
                        ),
                    )
                }
        }
    }
}

class ArtworkWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ArtworkWidget()
}
