package org.libraryofarts.core.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.ArtworkImage

enum class ShareLayout(val label: String) {
    MUSEUM_LABEL("Museum label"),
    POEM("Poem"),
    POSTER("Poster"),
}

enum class ShareRatio(val label: String, val width: Int, val height: Int) {
    STORY("Story 9:16", 1080, 1920),
    POST("Post 4:5", 1080, 1350),
    WIDE("Landscape 16:9", 1920, 1080),
    WALLPAPER("Phone wallpaper", 1080, 2340),
}

enum class ShareFont(val label: String, val typeface: Typeface) {
    SANS("Inter / sans", Typeface.create("sans-serif", Typeface.NORMAL)),
    SERIF("Gallery serif", Typeface.create("serif", Typeface.NORMAL)),
    SERIF_ITALIC("Serif italic", Typeface.create("serif", Typeface.ITALIC)),
}

/**
 * Renders a share card entirely on-device. The output is a PNG in the app cache
 * and is shared through a FileProvider; the app does not upload it.
 */
@Singleton
class ShareCardRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttp: OkHttpClient,
) {
    suspend fun render(
        artwork: Artwork,
        words: String,
        layout: ShareLayout,
        ratio: ShareRatio,
        font: ShareFont,
        showCredit: Boolean = true,
    ): Bitmap = withContext(Dispatchers.IO) {
        val bitmap = Bitmap.createBitmap(ratio.width, ratio.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val image = loadArtworkBitmap(artwork.image)
        drawBackground(canvas, bitmap, artwork, image)
        image?.recycle()

        when (layout) {
            ShareLayout.MUSEUM_LABEL -> drawMuseumLabel(canvas, bitmap, artwork, words, font, showCredit)
            ShareLayout.POEM -> drawPoem(canvas, bitmap, artwork, words, font, showCredit)
            ShareLayout.POSTER -> drawPoster(canvas, bitmap, artwork, words, font, showCredit)
        }
        bitmap
    }

    suspend fun renderToFile(
        artwork: Artwork,
        words: String,
        layout: ShareLayout,
        ratio: ShareRatio,
        font: ShareFont,
    ): File = withContext(Dispatchers.IO) {
        val bitmap = render(artwork, words, layout, ratio, font)
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "share-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        file
    }

    fun shareUri(file: File) = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )

    private fun drawBackground(canvas: Canvas, bitmap: Bitmap, artwork: Artwork, image: Bitmap?) {
        canvas.drawColor(artwork.dominantColor.toInt())
        if (image != null) {
            val src = Rect(0, 0, image.width, image.height)
            val scale = maxOf(bitmap.width.toFloat() / image.width, bitmap.height.toFloat() / image.height)
            val dstWidth = image.width * scale
            val dstHeight = image.height * scale
            val left = (bitmap.width - dstWidth) / 2f
            val top = (bitmap.height - dstHeight) / 2f
            canvas.drawBitmap(image, src, RectF(left, top, left + dstWidth, top + dstHeight), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f, 0f, 0f, bitmap.height.toFloat(),
            intArrayOf(
                Color.argb(60, 0, 0, 0),
                Color.argb(160, 0, 0, 0),
                Color.argb(235, 0, 0, 0),
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
    }

    private fun drawMuseumLabel(canvas: Canvas, bitmap: Bitmap, artwork: Artwork, words: String, font: ShareFont, showCredit: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val labelLeft = bitmap.width * 0.06f
        val labelTop = bitmap.height * 0.47f
        val labelRight = bitmap.width * 0.94f
        val labelBottom = bitmap.height * 0.95f
        paint.color = Color.argb(238, 247, 242, 233)
        canvas.drawRoundRect(RectF(labelLeft, labelTop, labelRight, labelBottom), 30f, 30f, paint)
        // Vermilion catalogue rule at the top of the label.
        paint.color = Color.rgb(166, 60, 38)
        canvas.drawRect(labelLeft, labelTop, labelLeft + 120f, labelTop + 12f, paint)

        var y = labelTop + 78f
        val textLeft = labelLeft + 46f
        val textWidth = labelRight - textLeft - 46f

        paint.color = Color.rgb(120, 110, 96)
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        paint.textSize = 22f
        paint.letterSpacing = 0.18f
        canvas.drawText(artwork.museum.shortName.uppercase(), textLeft, y, paint)
        y += 66

        paint.color = Color.rgb(26, 23, 20)
        paint.typeface = font.typeface
        paint.textSize = 50f
        paint.letterSpacing = 0f
        drawWrapped(canvas, artwork.title, textLeft, y, textWidth, paint, 58f, 2)
        val titleLines = wrappedLineCount(artwork.title, textWidth, paint).coerceIn(1, 2)
        y += titleLines * 58f + 18f

        paint.textSize = 28f
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.color = Color.rgb(90, 83, 74)
        drawWrapped(canvas, "${artwork.displayArtist} · ${artwork.year.ifBlank { "date unrecorded" }}", textLeft, y, textWidth, paint, 34f, 1)
        y += 44f

        paint.textSize = 22f
        paint.color = Color.rgb(104, 96, 86)
        val facts = listOfNotNull(
            artwork.displayPeriod?.let { it },
            artwork.medium?.let { it },
            artwork.museum.city.takeIf { it.isNotBlank() }?.let { city -> "${artwork.museum.name}, $city" },
        ).joinToString("  ·  ")
        if (facts.isNotBlank()) {
            drawWrapped(canvas, facts, textLeft, y, textWidth, paint, 30f, 3)
            y += wrappedLineCount(facts, textWidth, paint).coerceIn(1, 3) * 30f + 14f
        }

        if (words.isNotBlank()) {
            paint.typeface = Typeface.create("sans-serif", Typeface.ITALIC)
            paint.textSize = 26f
            paint.color = Color.rgb(40, 36, 31)
            drawWrapped(canvas, "“$words”", textLeft, y, textWidth, paint, 34f, 4)
        }
        if (showCredit) {
            paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            paint.textSize = 17f
            paint.color = Color.rgb(138, 128, 114)
            drawWrapped(canvas, "${artwork.license.shortLabel} · ${artwork.license.creditLine.ifBlank { artwork.creditLine }}", textLeft, labelBottom - 40f, textWidth, paint, 22f, 2)
        }
    }

    private fun drawPoem(canvas: Canvas, bitmap: Bitmap, artwork: Artwork, words: String, font: ShareFont, showCredit: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.typeface = font.typeface
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 30f
        paint.letterSpacing = 0.22f
        canvas.drawText(artwork.museum.shortName.uppercase(), bitmap.width / 2f, bitmap.height * 0.12f, paint)

        paint.letterSpacing = 0f
        paint.textSize = 52f
        drawCenteredWrapped(
            canvas, artwork.title, bitmap.width / 2f, bitmap.height * 0.2f,
            bitmap.width * 0.8f, paint, 62f, 2,
        )
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.textSize = 26f
        paint.color = Color.argb(220, 255, 255, 255)
        canvas.drawText("${artwork.displayArtist} · ${artwork.year}", bitmap.width / 2f, bitmap.height * 0.3f, paint)

        if (words.isNotBlank()) {
            paint.color = Color.WHITE
            paint.typeface = font.typeface
            paint.textSize = 38f
            drawCenteredWrapped(
                canvas, "“$words”", bitmap.width / 2f, bitmap.height * 0.55f,
                bitmap.width * 0.78f, paint, 50f, 6,
            )
        }
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.textSize = 20f
        paint.color = Color.argb(190, 255, 255, 255)
        paint.textAlign = Paint.Align.LEFT
        if (showCredit) {
            val facts = listOfNotNull(
                artwork.displayPeriod,
                artwork.museum.run { "$name, $city".takeIf { it.isNotBlank() && !city.isBlank() } ?: name.takeIf { it.isNotBlank() } },
            ).joinToString("  ·  ")
            drawWrapped(canvas, facts, bitmap.width * 0.1f, bitmap.height * 0.86f, bitmap.width * 0.8f, paint, 26f, 2)
            drawWrapped(canvas, "${artwork.license.shortLabel} · ${artwork.license.creditLine.ifBlank { artwork.creditLine }}", bitmap.width * 0.1f, bitmap.height * 0.93f, bitmap.width * 0.8f, paint, 26f, 2)
        }
    }

    private fun drawPoster(canvas: Canvas, bitmap: Bitmap, artwork: Artwork, words: String, font: ShareFont, showCredit: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val accent = artwork.palette.getOrNull(1)?.argb?.toInt() ?: Color.rgb(197, 154, 72)
        paint.color = accent
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height * 0.04f, paint)
        paint.color = Color.WHITE
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        paint.textSize = 22f
        paint.letterSpacing = 0.2f
        canvas.drawText(artwork.museum.shortName.uppercase(), bitmap.width * 0.08f, bitmap.height * 0.09f, paint)

        paint.letterSpacing = 0f
        paint.typeface = font.typeface
        paint.textSize = 92f
        drawWrapped(canvas, artwork.title, bitmap.width * 0.08f, bitmap.height * 0.22f, bitmap.width * 0.84f, paint, 98f, 3)
        paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        paint.textSize = 28f
        paint.color = Color.argb(220, 255, 255, 255)
        canvas.drawText("${artwork.displayArtist} · ${artwork.year}", bitmap.width * 0.08f, bitmap.height * 0.42f, paint)

        paint.textSize = 24f
        paint.color = Color.argb(215, 255, 255, 255)
        val facts = listOfNotNull(
            artwork.displayPeriod,
            artwork.medium,
            artwork.museum.run { if (city.isNotBlank()) "$name, $city" else name },
        ).joinToString("  ·  ")
        drawWrapped(canvas, facts, bitmap.width * 0.08f, bitmap.height * 0.50f, bitmap.width * 0.84f, paint, 32f, 3)

        if (words.isNotBlank()) {
            paint.color = Color.WHITE
            paint.typeface = font.typeface
            paint.textSize = 32f
            drawWrapped(canvas, "“$words”", bitmap.width * 0.08f, bitmap.height * 0.58f, bitmap.width * 0.7f, paint, 42f, 5)
        }
        if (showCredit) {
            paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            paint.textSize = 18f
            paint.color = Color.argb(180, 255, 255, 255)
            drawWrapped(canvas, "${artwork.license.shortLabel} · ${artwork.license.creditLine.ifBlank { artwork.creditLine }}", bitmap.width * 0.08f, bitmap.height * 0.93f, bitmap.width * 0.84f, paint, 24f, 2)
        }
    }

    /**
     * Resolves the plate in order: a pack-local file, a copy already stored in
     * the app's Coil disk cache, then the network. The card therefore renders
     * even when the device is offline.
     */
    private fun loadArtworkBitmap(image: ArtworkImage): Bitmap? {
        image.localPath?.let { path ->
            runCatching {
                val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                BitmapFactory.decodeFile(path, opts)
            }.getOrNull()?.let { return it }
        }
        image.thumbnailUrl.takeIf { it.isNotBlank() }?.let { url ->
            cachedBitmap(url)?.let { return it }
        }
        image.fullUrl.takeIf { it.isNotBlank() }?.let { url ->
            cachedBitmap(url)?.let { return it }
            return networkBitmap(url) ?: networkBitmap(image.thumbnailUrl)
        }
        return null
    }

    /** Looks for a file Coil already downloaded into its 512 MB disk cache. */
    private fun cachedBitmap(url: String): Bitmap? = runCatching {
        val cacheDir = File(context.cacheDir, "image_cache")
        val key = url.hashCode().toString(16)
        val folder = File(cacheDir, key.take(2))
        folder.listFiles()?.firstOrNull()?.let { entry ->
            BitmapFactory.decodeFile(entry.absolutePath)
        }
    }.getOrNull()

    private fun networkBitmap(url: String): Bitmap? {
        if (url.isBlank()) return null
        return runCatching {
            val request = Request.Builder().url(url).build()
            okHttp.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val bytes = response.body?.bytes() ?: return@runCatching null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }.getOrNull()
    }

    private fun wrappedLineCount(text: String, maxWidth: Float, paint: Paint): Int {
        val words = text.split(Regex("\\s+"))
        var line = ""
        var lines = 1
        for (word in words) {
            val candidate = if (line.isBlank()) word else "$line $word"
            if (paint.measureText(candidate) > maxWidth && line.isNotBlank()) {
                lines++
                line = word
            } else {
                line = candidate
            }
        }
        return lines
    }

    private fun drawWrapped(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float,
        maxLines: Int,
    ) {
        val words = text.split(Regex("\\s+"))
        var line = ""
        var currentY = y
        var lines = 0
        for (word in words) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                canvas.drawText(line, x, currentY, paint)
                line = word
                currentY += lineHeight
                lines++
                if (lines >= maxLines) break
            } else {
                line = candidate
            }
        }
        if (line.isNotEmpty() && lines < maxLines) canvas.drawText(line, x, currentY, paint)
    }

    private fun drawCenteredWrapped(
        canvas: Canvas,
        text: String,
        centerX: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float,
        maxLines: Int,
    ) {
        val words = text.split(Regex("\\s+"))
        var line = ""
        var currentY = y
        var lines = 0
        for (word in words) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                canvas.drawText(line, centerX, currentY, paint)
                line = word
                currentY += lineHeight
                lines++
                if (lines >= maxLines) break
            } else {
                line = candidate
            }
        }
        if (line.isNotEmpty() && lines < maxLines) canvas.drawText(line, centerX, currentY, paint)
    }
}
