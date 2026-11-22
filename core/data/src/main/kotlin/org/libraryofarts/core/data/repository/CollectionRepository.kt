package org.libraryofarts.core.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.libraryofarts.core.common.TimeProvider
import org.libraryofarts.core.data.mapper.ArtworkJson
import org.libraryofarts.core.data.mapper.toEntity
import org.libraryofarts.core.database.ArtworkDao
import org.libraryofarts.core.database.CollectionArtworkEntity
import org.libraryofarts.core.database.CollectionDao
import org.libraryofarts.core.database.CollectionEntity
import org.libraryofarts.core.model.Artwork
import org.libraryofarts.core.model.Collection
import org.libraryofarts.core.model.ExportableCollection
import java.util.UUID

@Singleton
class CollectionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val collectionDao: CollectionDao,
    private val artworkDao: ArtworkDao,
    private val artworkRepository: ArtworkRepository,
    private val time: TimeProvider,
    private val okHttp: OkHttpClient,
) {
    val collections: Flow<List<Collection>> = collectionDao.observeCollections().map { list ->
        list.map { it.toModel(emptyList()) }
    }

    val favorites: Flow<Collection?> = collectionDao.observeCollections().map { list ->
        list.firstOrNull { it.isFavorites }?.toModel(emptyList())
    }

    fun observeCollection(id: String): Flow<Collection?> =
        collectionDao.observeCollection(id).map { it?.toModel(emptyList()) }

    fun observeArtworkIds(collectionId: String): Flow<List<String>> =
        collectionDao.observeArtworkIds(collectionId).map { list -> list.map { it.artworkId } }

    suspend fun ensureFavorites(): Collection {
        collectionDao.favorites()?.let { return it.toModel(emptyList()) }
        val now = time.nowMillis()
        val entity = CollectionEntity(
            id = FAVORITES_ID,
            name = "Favorites",
            description = "Works you keep close.",
            coverArtworkId = null,
            createdAt = now,
            updatedAt = now,
            isFavorites = true,
            tagsCsv = "",
        )
        collectionDao.upsertCollection(entity)
        return entity.toModel(emptyList())
    }

    suspend fun create(name: String, description: String = "", tags: List<String> = emptyList()): Collection {
        val now = time.nowMillis()
        val entity = CollectionEntity(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Untitled collection" },
            description = description,
            coverArtworkId = null,
            createdAt = now,
            updatedAt = now,
            isFavorites = false,
            tagsCsv = tags.joinToString("|"),
        )
        collectionDao.upsertCollection(entity)
        return entity.toModel(emptyList())
    }

    suspend fun add(collectionId: String, artworkId: String) {
        val collection = collectionDao.getCollection(collectionId) ?: return
        val position = collectionDao.count(collectionId)
        collectionDao.addArtwork(
            CollectionArtworkEntity(
                collectionId = collectionId,
                artworkId = artworkId,
                addedAt = time.nowMillis(),
                position = position,
            ),
        )
        collectionDao.upsertCollection(collection.copy(updatedAt = time.nowMillis(), coverArtworkId = collection.coverArtworkId ?: artworkId))
        artworkRepository.getArtwork(artworkId)?.let { artworkRepository.recordSave(it) }
    }

    suspend fun remove(collectionId: String, artworkId: String) {
        collectionDao.removeArtwork(collectionId, artworkId)
        collectionDao.getCollection(collectionId)?.let {
            collectionDao.upsertCollection(it.copy(updatedAt = time.nowMillis()))
        }
    }

    suspend fun delete(collectionId: String) = collectionDao.deleteCollection(collectionId)

    suspend fun isInCollection(collectionId: String, artworkId: String): Boolean =
        collectionDao.memberships(artworkId).any { it.collectionId == collectionId }

    suspend fun artworksFor(collection: Collection): List<Artwork> {
        val ids = collectionDao.observeArtworkIds(collection.id).first().sortedBy { it.position }
        return ids.mapNotNull { artworkRepository.getArtwork(it.artworkId) }
    }

    suspend fun exportJson(collection: Collection): File = withContext(Dispatchers.IO) {
        val artworks = artworksFor(collection)
        val payload = ExportableCollection(
            exportedAt = time.nowMillis(),
            collection = collection.copy(artworkIds = artworks.map { it.id }),
            artworks = artworks,
        )
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "${collection.name.sanitize()}.${ExportableCollection.EXTENSION}")
        file.writeText(ArtworkJson.encodeToString(ExportableCollection.serializer(), payload))
        file
    }

    suspend fun importJson(file: File): Collection = withContext(Dispatchers.IO) {
        val payload = ArtworkJson.decodeFromString(ExportableCollection.serializer(), file.readText())
        require(payload.schemaVersion <= ExportableCollection.CURRENT_SCHEMA) { "This collection was made with a newer version of the app." }
        require(payload.artworks.isNotEmpty()) { "This collection file contains no artworks." }
        artworkDao.upsertAll(payload.artworks.map { it.toEntity() })
        val imported = payload.collection.copy(
            id = UUID.randomUUID().toString(),
            createdAt = time.nowMillis(),
            updatedAt = time.nowMillis(),
            isFavorites = false,
        )
        collectionDao.upsertCollection(imported.toEntity())
        payload.artworks.forEachIndexed { index, artwork ->
            collectionDao.addArtwork(
                CollectionArtworkEntity(imported.id, artwork.id, time.nowMillis(), index),
            )
        }
        imported
    }

    /**
     * Builds a virtual exhibition catalogue as a PDF. This runs entirely on the
     * device; images are fetched only when the user explicitly exports.
     */
    suspend fun exportPdf(collection: Collection, includeDescriptions: Boolean = true): File =
        withContext(Dispatchers.IO) {
            val artworks = artworksFor(collection)
            val document = PdfDocument()
            val pageWidth = 1240
            val pageHeight = 1754
            val margin = 92f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            val cover = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
            drawCover(cover.canvas, collection, artworks.size, paint, pageWidth, pageHeight, margin)
            document.finishPage(cover)

            artworks.forEachIndexed { index, artwork ->
                val page = document.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 2).create(),
                )
                drawArtworkPage(page.canvas, artwork, includeDescriptions, paint, pageWidth, pageHeight, margin, index + 1, artworks.size)
                document.finishPage(page)
            }

            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, "${collection.name.sanitize()}-exhibition.pdf")
            FileOutputStream(file).use { document.writeTo(it) }
            document.close()
            file
        }

    fun shareUri(file: File) = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )

    private fun drawCover(
        canvas: Canvas,
        collection: Collection,
        count: Int,
        paint: Paint,
        width: Int,
        height: Int,
        margin: Float,
    ) {
        canvas.drawColor(Color.rgb(247, 242, 233))
        paint.color = Color.rgb(181, 69, 43)
        paint.textSize = 28f
        paint.letterSpacing = 0.12f
        canvas.drawText("THE GREAT LIBRARY OF ARTS", margin, margin + 40, paint)

        paint.color = Color.rgb(26, 23, 20)
        paint.textSize = 92f
        paint.letterSpacing = 0f
        drawWrapped(canvas, collection.name, margin, margin + 240, width - margin * 2, paint, 92f * 1.1f)

        paint.color = Color.rgb(90, 83, 74)
        paint.textSize = 30f
        val description = collection.description.ifBlank {
            "A virtual exhibition of $count works, gathered in The Great Library of Arts."
        }
        drawWrapped(canvas, description, margin, margin + 480, width - margin * 2, paint, 42f, maxLines = 6)

        paint.color = Color.rgb(181, 69, 43)
        paint.textSize = 24f
        canvas.drawText("$count WORKS · A PRIVATE EXHIBITION", margin, height - margin - 30, paint)
        paint.color = Color.rgb(120, 110, 96)
        paint.textSize = 20f
        canvas.drawText("Every work is shown with its licence, credit line and provenance.", margin, height - margin, paint)
    }

    private fun drawArtworkPage(
        canvas: Canvas,
        artwork: Artwork,
        includeDescriptions: Boolean,
        paint: Paint,
        width: Int,
        height: Int,
        margin: Float,
        page: Int,
        total: Int,
    ) {
        canvas.drawColor(Color.rgb(252, 249, 243))
        paint.color = Color.rgb(138, 128, 114)
        paint.textSize = 18f
        canvas.drawText("${page.toString().padStart(2, '0')} / ${total.toString().padStart(2, '0')}", margin, margin, paint)

        val bitmap = loadBitmap(artwork.image.fullUrl.ifBlank { artwork.image.thumbnailUrl })
        if (bitmap != null) {
            val imageHeight = 900f
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val imageWidth = (imageHeight * ratio).coerceAtMost(width - margin * 2)
            val rect = RectF(margin, margin + 40, margin + imageWidth, margin + 40 + imageHeight)
            canvas.drawBitmap(bitmap, null, rectF(rect), Paint(Paint.FILTER_BITMAP_FLAG))
            bitmap.recycle()
        }

        var y = margin + 1_040
        paint.color = Color.rgb(26, 23, 20)
        paint.textSize = 48f
        drawWrapped(canvas, artwork.title, margin, y, width - margin * 2, paint, 58f, maxLines = 2)
        y += 120
        paint.textSize = 28f
        paint.color = Color.rgb(90, 83, 74)
        canvas.drawText("${artwork.displayArtist} · ${artwork.year}", margin, y, paint)
        y += 50
        canvas.drawText("${artwork.museum.name}, ${artwork.museum.city}", margin, y, paint)
        y += 50

        paint.textSize = 20f
        paint.color = Color.rgb(120, 110, 96)
        val details = listOfNotNull(artwork.medium, artwork.dimensions, artwork.license.shortLabel).joinToString(" · ")
        drawWrapped(canvas, details, margin, y, width - margin * 2, paint, 28f, maxLines = 2)
        y += 60

        if (includeDescriptions && !artwork.description.isNullOrBlank()) {
            paint.textSize = 22f
            paint.color = Color.rgb(60, 55, 48)
            y += 10
            drawWrapped(canvas, artwork.description.orEmpty(), margin, y, width - margin * 2, paint, 32f, maxLines = 7)
        }

        paint.textSize = 16f
        paint.color = Color.rgb(138, 128, 114)
        drawWrapped(
            canvas,
            "Credit: ${artwork.creditLine.ifBlank { artwork.license.creditLine }}".trim(),
            margin,
            height - margin - 30,
            width - margin * 2,
            paint,
            22f,
            maxLines = 2,
        )
    }

    private fun loadBitmap(url: String): Bitmap? {
        if (url.isBlank()) return null
        return runCatching {
            val request = Request.Builder().url(url).build()
            okHttp.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bytes = response.body?.bytes() ?: return null
                val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
        }.getOrNull()
    }

    private fun drawWrapped(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float,
        maxLines: Int = 3,
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
                if (lines >= maxLines - 1) break
            } else {
                line = candidate
            }
        }
        if (line.isNotEmpty() && lines < maxLines) canvas.drawText(line, x, currentY, paint)
    }

    private fun rectF(rect: RectF): RectF = rect

    private fun String.sanitize(): String = lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "collection" }

    private fun CollectionEntity.toModel(ids: List<String>): Collection = Collection(
        id = id,
        name = name,
        description = description,
        artworkIds = ids,
        coverArtworkId = coverArtworkId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isFavorites = isFavorites,
        tags = tagsCsv.split('|').filter { it.isNotBlank() },
    )

    private fun Collection.toEntity(): CollectionEntity = CollectionEntity(
        id = id,
        name = name,
        description = description,
        coverArtworkId = coverArtworkId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isFavorites = isFavorites,
        tagsCsv = tags.joinToString("|"),
    )

    companion object {
        const val FAVORITES_ID = "favorites"
    }
}
