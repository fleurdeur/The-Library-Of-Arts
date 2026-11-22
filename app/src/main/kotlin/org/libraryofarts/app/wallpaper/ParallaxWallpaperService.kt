package org.libraryofarts.app.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.time.LocalDate
import org.libraryofarts.core.data.seed.SeedArtworks

/**
 * A parallax live wallpaper that draws a public-domain work's palette and title.
 * Bitmap decoding is intentionally avoided here so the wallpaper stays lightweight
 * and starts instantly; the image itself is available in the app.
 */
class ParallaxWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = ParallaxEngine()

    private inner class ParallaxEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var offset = 0.5f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 255, 255, 255)
            textSize = 36f
        }
        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(160, 255, 255, 255)
            textSize = 22f
        }

        private val drawRunnable = object : Runnable {
            override fun run() {
                draw()
                handler.postDelayed(this, 16_000)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                draw()
                handler.postDelayed(drawRunnable, 16_000)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int) {
            offset = xOffset
            draw()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            draw()
        }

        override fun onDestroy() {
            handler.removeCallbacksAndMessages(null)
            super.onDestroy()
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) render(canvas)
            } catch (_: Exception) {
                // The surface can disappear during rotation; the next frame redraws.
            } finally {
                if (canvas != null) runCatching { holder.unlockCanvasAndPost(canvas) }
            }
        }

        private fun render(canvas: Canvas) {
            val artwork = SeedArtworks.all[LocalDate.now().dayOfYear % SeedArtworks.all.size]
            val palette = artwork.palette.map { it.argb.toInt() }
                .ifEmpty { listOf(Color.rgb(26, 23, 20), Color.rgb(181, 69, 43), Color.rgb(197, 154, 72)) }
            val width = canvas.width.toFloat()
            val height = canvas.height.toFloat()
            paint.shader = LinearGradient(
                0f, 0f, width, height,
                palette.toIntArray(),
                null,
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, width, height, paint)
            paint.shader = null

            val shift = (offset - 0.5f) * width * 0.12f
            paint.color = Color.argb(30, 255, 255, 255)
            canvas.drawRect(width * 0.12f + shift, height * 0.18f, width * 0.88f + shift, height * 0.72f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = Color.argb(90, 255, 255, 255)
            canvas.drawRect(width * 0.12f + shift, height * 0.18f, width * 0.88f + shift, height * 0.72f, paint)
            paint.style = Paint.Style.FILL

            val titleX = width * 0.12f + shift
            canvas.drawText(artwork.title.take(34), titleX, height * 0.80f, textPaint)
            canvas.drawText(
                "${artwork.artist?.name ?: "Unknown artist"} · ${artwork.year.ifBlank { "date unknown" }}",
                titleX,
                height * 0.84f,
                titlePaint,
            )
            canvas.drawText(
                "THE GREAT LIBRARY OF ARTS",
                titleX,
                height * 0.14f,
                titlePaint,
            )
            if (visible) handler.removeCallbacks(drawRunnable)
        }
    }
}
